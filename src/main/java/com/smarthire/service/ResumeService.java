package com.smarthire.service;

import com.smarthire.dto.ResumeGenerationRequest;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import com.smarthire.repository.CandidateProfileRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResumeService {
    private static final String DEFAULT_TEMPLATE = "ATS Classic";

    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final ResumeRepository resumes;
    private final AiAnalysisService ai;
    private final Path uploadDir;

    public ResumeService(UserRepository users, CandidateProfileRepository candidates, ResumeRepository resumes,
                         AiAnalysisService ai, @Value("${app.upload-dir:uploads/resumes}") String uploadDir) {
        this.users = users;
        this.candidates = candidates;
        this.resumes = resumes;
        this.ai = ai;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Resume upload(String email, MultipartFile file) {
        CandidateProfile candidate = candidate(email);
        validate(file);
        try {
            byte[] data = file.getBytes();
            String extracted = extract(data);
            if (extracted.length() > 30000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text is too long. Please upload a shorter resume.");
            }
            Files.createDirectories(uploadDir);
            String original = Path.of(file.getOriginalFilename()).getFileName().toString();
            String saved = UUID.randomUUID() + ".pdf";
            Path destination = uploadDir.resolve(saved).normalize();
            if (!destination.startsWith(uploadDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name.");
            }
            Files.write(destination, data);
            Resume resume = resumes.save(new Resume(candidate.getId(), "/uploads/resumes/" + saved, original, extracted));
            ai.analyzeResume(resume.getId());
            return resume;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the PDF.");
        }
    }

    /**
     * Generates a usable template synchronously and locally. This deliberately has no external-AI dependency,
     * so an absent or unavailable ANTHROPIC_API_KEY can never prevent a candidate from creating a resume.
     */
    public Resume generate(String email, ResumeGenerationRequest request) {
        User user = candidateUser(email);
        CandidateProfile candidate = candidate(user);
        String content = ResumeTemplateBuilder.build(user, candidate, request);
        Resume generated = Resume.generated(candidate.getId(), generatedFileName(request), content, templateName(request));
        generated = resumes.save(generated);
        generated.updateGeneratedContentUrl();
        return resumes.save(generated);
    }

    public List<Resume> mine(String email) {
        List<Resume> result = resumes.findByCandidateProfileIdOrderByUploadedAtDesc(candidate(email).getId());
        for (Resume resume : result) {
            if (resume.getAiSummary() != null && resume.getAiSummary().contains("AI analysis is unavailable")) {
                ai.analyzeResumeNow(resume.getId());
            }
        }
        return resumes.findByCandidateProfileIdOrderByUploadedAtDesc(candidate(email).getId());
    }

    public Resume mine(String email, Long id) {
        CandidateProfile candidate = candidate(email);
        return resumes.findByIdAndCandidateProfileId(id, candidate.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found."));
    }

    /** Streams the original upload only to the candidate who owns it. */
    public Resource file(String email, Long id) {
        Resume resume = mine(email, id);
        if ("GENERATED".equals(resume.getSourceType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Generated resumes do not have an uploaded PDF file.");
        }
        String fileName = Path.of(resume.getFileUrl()).getFileName().toString();
        Path file = uploadDir.resolve(fileName).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume file not found.");
        }
        return new FileSystemResource(file);
    }

    /** Returns generated Markdown or the extracted plain text of an uploaded PDF, always ownership checked. */
    public String content(String email, Long id) {
        Resume resume = mine(email, id);
        String content = resume.getGeneratedContent();
        if (content == null || content.isBlank()) {
            content = resume.getExtractedText();
        }
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume content is not available.");
        }
        return content;
    }

    private CandidateProfile candidate(String email) {
        return candidate(candidateUser(email));
    }

    private User candidateUser(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This endpoint is for candidates.");
        }
        return user;
    }

    private CandidateProfile candidate(User user) {
        return candidates.findByUserId(user.getId())
                .orElseGet(() -> candidates.save(new CandidateProfile(user.getId())));
    }

    private String generatedFileName(ResumeGenerationRequest request) {
        String value = request.title() == null || request.title().isBlank() ? request.targetRole() : request.title();
        String stem = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return (stem.isBlank() ? "generated-resume" : stem.substring(0, Math.min(stem.length(), 220))) + ".md";
    }

    private String templateName(ResumeGenerationRequest request) {
        String requested = request.templateName();
        return requested == null || requested.isBlank() ? DEFAULT_TEMPLATE : requested.trim();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A PDF file is required.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 255 || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please use a valid PDF file name of at most 255 characters.");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType()) && !name.endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF resumes are supported.");
        }
    }

    private String extract(byte[] data) throws IOException {
        try (PDDocument document = Loader.loadPDF(data)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }
}
