package com.smarthire.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.dto.ResumeGenerationRequest;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import org.junit.jupiter.api.Test;

class ResumeTemplateBuilderTest {
    @Test
    void createsAtsFriendlyContentWithoutAnyExternalAiDependency() {
        User user = new User("Ayesha Khan", "ayesha@example.com", "hash", Role.CANDIDATE);
        CandidateProfile profile = new CandidateProfile(1L);
        profile.update("9876543210", "Delhi", "linkedin.com/in/ayesha", "github.com/ayesha", "", "Backend developer focused on reliable APIs.");
        ResumeGenerationRequest request = new ResumeGenerationRequest(
                "Java Backend Developer", null, "modern", null,
                "Java, Spring Boot, PostgreSQL", "Software Engineer - Acme\nBuilt REST APIs", "B.Tech, Computer Science", "SmartHire - Built a hiring platform");

        String content = ResumeTemplateBuilder.build(user, profile, request);

        assertTrue(content.contains("AYESHA KHAN"));
        assertTrue(content.contains("Java Backend Developer"));
        assertTrue(content.contains("CORE SKILLS"));
        assertTrue(content.contains("Java, Spring Boot, PostgreSQL"));
        assertTrue(content.contains("PROFESSIONAL EXPERIENCE"));
        assertTrue(content.contains("Built REST APIs"));
        assertTrue(content.contains("EDUCATION"));
        assertTrue(content.contains("PROJECTS"));
    }

    @Test
    void usesTruthfulPlaceholdersWhenCandidateHasNoHistoryYet() {
        User user = new User("Ravi", "ravi@example.com", "hash", Role.CANDIDATE);
        CandidateProfile profile = new CandidateProfile(1L);
        ResumeGenerationRequest request = new ResumeGenerationRequest("Frontend Developer", null, null, null, null, null, null, null);

        String content = ResumeTemplateBuilder.build(user, profile, request);

        assertTrue(content.contains("Candidate seeking a Frontend Developer opportunity."));
        assertTrue(content.contains("Add your most relevant work experience"));
        assertTrue(content.contains("Add your education"));
        assertTrue(content.contains("Add projects"));
    }

    @Test
    void acceptsTheFrontendTemplateAliasAndPlainTextDraftSections() throws Exception {
        ResumeGenerationRequest request = new ObjectMapper().readValue("""
                {"targetRole":"React Developer","template":"modern","skills":"React, JavaScript",
                 "experience":"Built accessible UI","education":"BCA","projects":"Portfolio"}
                """, ResumeGenerationRequest.class);

        assertEquals("modern", request.templateName());
        assertEquals("React, JavaScript", request.skills());
        assertEquals("Built accessible UI", request.experience());
    }
}
