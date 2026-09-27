package com.smarthire.entity;

import com.smarthire.enums.AnalysisStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resumes")
public class Resume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long candidateProfileId;
    @Column(nullable = false, length = 500) private String fileUrl;
    @Column(nullable = false, length = 255) private String originalFileName;
    @Column(length = 30000) private String extractedText;
    /** Canonical, ATS-friendly Markdown/plain text for a platform-generated resume. */
    @Column(length = 30000) private String generatedContent;
    @Column(length = 80) private String templateName;
    @Column(length = 20) private String sourceType = "UPLOADED";
    @Column(length = 5000) private String aiSkills;
    @Column(length = 5000) private String aiSummary;
    @Column(length = 5000) private String aiExperience;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AnalysisStatus analysisStatus = AnalysisStatus.PROCESSING;
    @Column(nullable = false, updatable = false) private Instant uploadedAt = Instant.now();
    protected Resume() { }
    public Resume(Long candidateProfileId, String fileUrl, String originalFileName, String extractedText) { this.candidateProfileId=candidateProfileId; this.fileUrl=fileUrl; this.originalFileName=originalFileName; this.extractedText=extractedText; }
    public static Resume generated(Long candidateProfileId, String originalFileName, String content, String templateName) {
        Resume resume = new Resume(candidateProfileId, "/api/resumes/pending/content", originalFileName, content);
        resume.generatedContent = content;
        resume.templateName = templateName;
        resume.sourceType = "GENERATED";
        resume.analysisStatus = AnalysisStatus.COMPLETED;
        return resume;
    }
    public void updateGeneratedContentUrl() { this.fileUrl = "/api/resumes/" + id + "/content"; }
    public void completeAnalysis(String skills, String summary, String experience) { aiSkills=skills; aiSummary=summary; aiExperience=experience; analysisStatus=AnalysisStatus.COMPLETED; }
    public void failAnalysis() { analysisStatus=AnalysisStatus.FAILED; }
    public Long getId(){return id;} public Long getCandidateProfileId(){return candidateProfileId;} public String getFileUrl(){return fileUrl;} public String getOriginalFileName(){return originalFileName;} public String getExtractedText(){return extractedText;} public String getGeneratedContent(){return generatedContent;} public String getTemplateName(){return templateName;} public String getSourceType(){return sourceType == null ? "UPLOADED" : sourceType;} public String getAiSkills(){return aiSkills;} public String getAiSummary(){return aiSummary;} public String getAiExperience(){return aiExperience;} public AnalysisStatus getAnalysisStatus(){return analysisStatus;} public Instant getUploadedAt(){return uploadedAt;}
}
