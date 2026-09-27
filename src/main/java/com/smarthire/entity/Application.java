package com.smarthire.entity;

import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.MatchAnalysisStatus;
import com.smarthire.enums.MatchSource;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "candidate_profile_id"}))
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="job_id", nullable=false) private Long jobId;
    @Column(name="candidate_profile_id", nullable=false) private Long candidateProfileId;
    @Column(name="resume_id", nullable=false) private Long resumeId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ApplicationStatus status=ApplicationStatus.APPLIED;
    private Integer matchScore;
    // Nullable for safe rollout against existing application rows; new rows always begin as PENDING.
    @Enumerated(EnumType.STRING) @Column(length=32) private MatchAnalysisStatus matchAnalysisStatus = MatchAnalysisStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(length=32) private MatchSource matchSource;
    private Instant matchAnalyzedAt;
    @Column(length=5000) private String matchReasoning;
    @Column(length=5000) private String missingSkills;
    @Column(length=5000) private String strengths;
    @Column(length=3000) private String coverLetter;
    @Column(nullable=false,updatable=false) private Instant appliedAt=Instant.now();
    private Instant updatedAt=Instant.now();
    @Transient private String candidateName;
    @Transient private String candidateEmail;
    @Transient private String candidatePhone;
    @Transient private String candidateLocation;
    @Transient private String candidateBio;
    @Transient private String candidateLinkedinUrl;
    @Transient private String candidateGithubUrl;
    @Transient private String candidatePortfolioUrl;
    @Transient private String resumeFileName;
    @Transient private String resumeSourceType;
    protected Application() { }
    public Application(Long jobId,Long candidateProfileId,Long resumeId){this(jobId,candidateProfileId,resumeId,null);}
    public Application(Long jobId,Long candidateProfileId,Long resumeId,String coverLetter){this.jobId=jobId;this.candidateProfileId=candidateProfileId;this.resumeId=resumeId;this.coverLetter=coverLetter;}
    public void updateStatus(ApplicationStatus value){status=value;updatedAt=Instant.now();}
    public void withdraw(){status=ApplicationStatus.WITHDRAWN;updatedAt=Instant.now();}
    public void markMatchProcessing(){matchAnalysisStatus=MatchAnalysisStatus.PROCESSING;updatedAt=Instant.now();}
    public void completeMatch(int score,String reasoning,String strengthsValue,String missingSkillsValue){completeMatch(score,reasoning,strengthsValue,missingSkillsValue,MatchSource.HEURISTIC);}
    public void completeMatch(int score,String reasoning,String strengthsValue,String missingSkillsValue,MatchSource source){matchScore=Math.max(0,Math.min(100,score));matchReasoning=reasoning;strengths=strengthsValue;missingSkills=missingSkillsValue;matchSource=source;matchAnalysisStatus=MatchAnalysisStatus.COMPLETED;matchAnalyzedAt=Instant.now();updatedAt=Instant.now();}
    public void markMatchUnavailable(String reasoning){matchScore=null;matchReasoning=reasoning;strengths="[]";missingSkills="[]";matchSource=null;matchAnalysisStatus=MatchAnalysisStatus.INSUFFICIENT_DATA;matchAnalyzedAt=Instant.now();updatedAt=Instant.now();}
    public void markMatchFailed(String reasoning){matchScore=null;matchReasoning=reasoning;matchAnalysisStatus=MatchAnalysisStatus.FAILED;matchAnalyzedAt=Instant.now();updatedAt=Instant.now();}
    public void setCandidateDetails(String name,String email){candidateName=name;candidateEmail=email;}
    public void setCandidateDetails(String name, String email, String phone, String location, String bio,
                                    String linkedinUrl, String githubUrl, String portfolioUrl) {
        candidateName=name; candidateEmail=email; candidatePhone=phone; candidateLocation=location; candidateBio=bio;
        candidateLinkedinUrl=linkedinUrl; candidateGithubUrl=githubUrl; candidatePortfolioUrl=portfolioUrl;
    }
    public void setResumeDetails(String fileName, String sourceType) { resumeFileName=fileName; resumeSourceType=sourceType; }
    public Long getId(){return id;} public Long getJobId(){return jobId;} public Long getCandidateProfileId(){return candidateProfileId;} public Long getResumeId(){return resumeId;} public ApplicationStatus getStatus(){return status;} public Integer getMatchScore(){return matchScore;} public boolean hasExplicitMatchAnalysisStatus(){return matchAnalysisStatus != null;} public MatchAnalysisStatus getMatchAnalysisStatus(){return matchAnalysisStatus == null ? (matchScore == null ? MatchAnalysisStatus.PENDING : MatchAnalysisStatus.COMPLETED) : matchAnalysisStatus;} public MatchSource getMatchSource(){return matchSource;} public Instant getMatchAnalyzedAt(){return matchAnalyzedAt;} public String getMatchReasoning(){return matchReasoning;} public String getMissingSkills(){return missingSkills;} public String getStrengths(){return strengths;} public String getCoverLetter(){return coverLetter;} public Instant getAppliedAt(){return appliedAt;} public Instant getUpdatedAt(){return updatedAt;} public String getCandidateName(){return candidateName;} public String getCandidateEmail(){return candidateEmail;} public String getCandidatePhone(){return candidatePhone;} public String getCandidateLocation(){return candidateLocation;} public String getCandidateBio(){return candidateBio;} public String getCandidateLinkedinUrl(){return candidateLinkedinUrl;} public String getCandidateGithubUrl(){return candidateGithubUrl;} public String getCandidatePortfolioUrl(){return candidatePortfolioUrl;} public String getResumeFileName(){return resumeFileName;} public String getResumeSourceType(){return resumeSourceType;}
}
