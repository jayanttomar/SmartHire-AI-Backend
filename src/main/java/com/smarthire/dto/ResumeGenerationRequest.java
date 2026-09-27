package com.smarthire.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Candidate-provided facts used to create an editable, ATS-friendly resume.
 * Identity and contact information always come from the authenticated account/profile.
 */
public record ResumeGenerationRequest(
        @NotBlank @Size(max = 120) String targetRole,
        @Size(max = 120) String title,
        @JsonAlias("template") @Size(max = 80) String templateName,
        @Size(max = 2000) String summary,
        @Size(max = 4000) String skills,
        @Size(max = 8000) String experience,
        @Size(max = 5000) String education,
        @Size(max = 8000) String projects) { }
