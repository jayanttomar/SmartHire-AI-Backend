package com.smarthire.service;

import com.smarthire.dto.ApplicationRequest;
import com.smarthire.entity.Application;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.Role;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.CandidateProfileRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationService {
    private final ApplicationRepository applications;
    private final CandidateProfileRepository candidates;
    private final ResumeRepository resumes;
    private final JobRepository jobs;
    private final UserRepository users;
    private final AiAnalysisService ai;
    private final ApplicationNotificationService notifications;
    private final Path uploadDir;

    public ApplicationService(ApplicationRepository applications, CandidateProfileRepository candidates,
                              ResumeRepository resumes, JobRepository jobs, UserRepository users, AiAnalysisService ai,
                              ApplicationNotificationService notifications,
                              @Value("${app.upload-dir:uploads/resumes}") String uploadDir) {
        this.applications = applications; this.candidates = candidates; this.resumes = resumes;
        this.jobs = jobs; this.users = users; this.ai = ai; this.notifications = notifications;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Application apply(String email, ApplicationRequest request) {
        CandidateProfile candidate = candidate(email);
        Job job = jobs.findById(request.jobId()).filter(Job::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active job not found."));
        if (applications.existsByJobIdAndCandidateProfileId(job.getId(), candidate.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this job.");
        }
        Resume resume = resumes.findByIdAndCandidateProfileId(request.resumeId(), candidate.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found."));
        String coverLetter = request.coverLetter() == null ? null : request.coverLetter().trim();
        Application application = applications.save(new Application(job.getId(), candidate.getId(), resume.getId(), coverLetter));
        ai.scoreApplication(application.getId(), job, resume);
        return application;
    }

    public List<Application> mine(String email) {
        return applications.findByCandidateProfileIdOrderByAppliedAtDesc(candidate(email).getId());
    }

    public List<Application> forJob(String email, Long jobId) {
        Job job = ownedRecruiterJob(email, jobId);
        return applications.findByJobIdOrderByAppliedAtDesc(jobId).stream().map(application -> enrichApplication(application, job)).toList();
    }

    /** Single recruiter inbox endpoint; avoids one application query per job in the client. */
    public List<Application> forRecruiter(String email) {
        User recruiter = recruiter(email);
        return jobs.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId()).stream()
                .flatMap(job -> applications.findByJobIdOrderByAppliedAtDesc(job.getId()).stream().map(application -> enrichApplication(application, job)))
                .sorted((left, right) -> right.getAppliedAt().compareTo(left.getAppliedAt()))
                .peek(this::addCandidateDetails)
                .toList();
    }

    public Application updateStatus(String email, Long applicationId, ApplicationStatus status) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        Job job = ownedRecruiterJob(email, application.getJobId());
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A withdrawn application cannot be moved in the pipeline.");
        }
        ApplicationStatus previousStatus = application.getStatus();
        application.updateStatus(status);
        Application saved = applications.save(application);
        if (status == ApplicationStatus.SHORTLISTED && previousStatus != ApplicationStatus.SHORTLISTED) {
            candidates.findById(saved.getCandidateProfileId())
                    .flatMap(candidate -> users.findById(candidate.getUserId()))
                    .ifPresent(candidate -> notifications.sendShortlistedEmail(candidate, job));
        }
        addCandidateDetails(saved);
        return saved;
    }

    public Application withdraw(String email, Long applicationId) {
        CandidateProfile candidate = candidate(email);
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        if (!application.getCandidateProfileId().equals(candidate.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can withdraw only your own applications.");
        }
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A hired application cannot be withdrawn here.");
        }
        application.withdraw();
        return applications.save(application);
    }

    /** Recruiter-triggered retry for a score that was pending, unavailable, or based on an incomplete job post. */
    public Application recalculateMatch(String email, Long applicationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        Job job = ownedRecruiterJob(email, application.getJobId());
        Resume resume = resumes.findById(application.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found."));
        ai.scoreApplicationNow(application.getId(), job, resume);
        Application updated = applications.findById(applicationId).orElse(application);
        addCandidateDetails(updated);
        return updated;
    }

    /** Recruiters may read a submitted resume only for their own job. */
    public String resumeContent(String email, Long applicationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        ownedRecruiterJob(email, application.getJobId());
        Resume resume = resumes.findById(application.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found."));
        String content = resume.getGeneratedContent();
        if (content == null || content.isBlank()) content = resume.getExtractedText();
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume content is not available.");
        }
        return content;
    }

    /** Provides the original PDF after confirming the recruiter owns the application job. */
    public SubmittedResume resumeFile(String email, Long applicationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        ownedRecruiterJob(email, application.getJobId());
        Resume resume = resumes.findById(application.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found."));
        if ("GENERATED".equals(resume.getSourceType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This application uses a generated resume, not an uploaded PDF.");
        }
        String fileName = Path.of(resume.getFileUrl()).getFileName().toString();
        Path file = uploadDir.resolve(fileName).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume PDF file not found.");
        }
        return new SubmittedResume(resume.getOriginalFileName(), new FileSystemResource(file));
    }

    public record SubmittedResume(String fileName, Resource resource) { }

    private CandidateProfile candidate(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This endpoint is for candidates.");
        }
        return candidates.findByUserId(user.getId()).orElseGet(() -> candidates.save(new CandidateProfile(user.getId())));
    }

    private void addCandidateDetails(Application application) {
        candidates.findById(application.getCandidateProfileId()).flatMap(profile -> users.findById(profile.getUserId())
                .map(user -> new Object[] { profile, user })).ifPresent(details -> {
            CandidateProfile profile = (CandidateProfile) details[0];
            User user = (User) details[1];
            application.setCandidateDetails(user.getName(), user.getEmail(), profile.getPhone(), profile.getLocation(),
                    profile.getBio(), profile.getLinkedinUrl(), profile.getGithubUrl(), profile.getPortfolioUrl());
        });
        resumes.findById(application.getResumeId())
                .ifPresent(resume -> application.setResumeDetails(resume.getOriginalFileName(), resume.getSourceType()));
    }

    private Application enrichApplication(Application application, Job job) {
        // Old rows predate match lifecycle fields. A legacy zero for a role with no skill requirements is not a real 0% match.
        if (!application.hasExplicitMatchAnalysisStatus() && Integer.valueOf(0).equals(application.getMatchScore()) && !ai.hasRecognizableJobSkills(job)) {
            application.markMatchUnavailable("Match is unavailable because this role has no recognizable skills. Add concrete requirements and recalculate.");
            applications.save(application);
        }
        addCandidateDetails(application);
        return application;
    }

    private Job ownedRecruiterJob(String email, Long jobId) {
        User user = recruiter(email);
        Job job = jobs.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        if (!job.getRecruiterId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can access only applications for your jobs.");
        }
        return job;
    }

    private User recruiter(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This endpoint is for recruiters.");
        }
        return user;
    }
}
