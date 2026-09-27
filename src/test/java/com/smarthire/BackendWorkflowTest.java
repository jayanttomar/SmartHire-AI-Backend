package com.smarthire;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.dto.AuthResponse;
import com.smarthire.enums.Role;
import com.smarthire.service.OAuthLoginCodeService;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false", "app.google.client-id=test-client", "app.google.client-secret=test-secret",
    "app.jwt.secret=test-only-secret-with-more-than-thirty-two-characters", "app.frontend-url=http://localhost:5173",
    "anthropic.api-key=", "app.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class BackendWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OAuthLoginCodeService codes;
    @Autowired com.smarthire.service.AuthService auth;
    @MockBean JavaMailSender mail;

    @Test void registrationLoginValidationAndAccessControl() throws Exception {
        String email = email();
        JsonNode account = register(email, "CANDIDATE");
        String token = account.path("token").asText();
        assertFalse(token.isBlank());
        request(get("/api/auth/me"), token, null, 200);
        request(get("/api/auth/me"), null, null, 401);
        request(get("/api/auth/me"), "invalid-token", null, 401);
        request(get("/api/jobs/mine"), null, null, 401);
        request(get("/api/jobs/mine"), token, null, 403);
        request(get("/api/recruiters/profile"), token, null, 403);
        request(post("/api/auth/login"), null, Map.of("email", email.toUpperCase(), "password", "Password123!"), 200);
        JsonNode badLogin = request(post("/api/auth/login"), null, Map.of("email", email, "password", "wrong"), 401);
        assertEquals("Invalid email or password.", badLogin.path("message").asText());
        request(post("/api/auth/register"), null, Map.of("name", "Test", "email", email, "password", "Password123!", "role", "CANDIDATE"), 409);
        request(post("/api/auth/register"), null, Map.of("name", "Test", "email", "invalid", "password", "short", "role", "CANDIDATE"), 400);
        request(post("/api/auth/register"), null, Map.of("name", "Test", "email", email(), "password", "Password123!", "role", "ADMIN"), 400);
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test void passwordResetChangesLoginAndRejectsTokenReuse() throws Exception {
        String email = email(); register(email, "CANDIDATE");
        request(post("/api/auth/forgot-password"), null, Map.of("email", email), 204);
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        String link = message.getValue().getText().lines().filter(line -> line.startsWith("http://")).findFirst().orElseThrow();
        String token = UriComponentsBuilder.fromUriString(link).build().getQueryParams().getFirst("token");
        request(post("/api/auth/reset-password"), null, Map.of("token", token, "password", "NewPassword123!"), 204);
        request(post("/api/auth/login"), null, Map.of("email", email, "password", "Password123!"), 401);
        request(post("/api/auth/login"), null, Map.of("email", email, "password", "NewPassword123!"), 200);
        request(post("/api/auth/reset-password"), null, Map.of("token", token, "password", "AnotherPassword!"), 400);
        request(post("/api/auth/reset-password"), null, Map.of("token", "invalid", "password", "AnotherPassword!"), 400);
        request(post("/api/auth/forgot-password"), null, Map.of("email", email()), 204);
    }

    @Test void googleRedirectFailureAndSingleUseCodeExchange() throws Exception {
        MvcResult redirect = mvc.perform(get("/oauth2/authorization/google").param("role", "RECRUITER"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertTrue(redirect.getResponse().getRedirectedUrl().startsWith("https://accounts.google.com/"));
        assertEquals("RECRUITER", redirect.getRequest().getSession().getAttribute("googleSignupRole"));
        assertTrue(redirect.getResponse().getRedirectedUrl().contains("redirect_uri="));
        mvc.perform(get("/login/oauth2/code/google").param("error", "access_denied"))
                .andExpect(redirectedUrl("http://localhost:5173/?oauthError=google_sign_in_failed"));
        String code = codes.create(new AuthResponse("test-token", "Bearer", 123L, "Test", "test@example.test", Role.CANDIDATE));
        request(post("/api/auth/oauth/exchange"), null, Map.of("code", code), 200);
        request(post("/api/auth/oauth/exchange"), null, Map.of("code", code), 401);
    }

    @Test void googleLoginCreatesSelectedRoleButPreservesExistingAccountRole() throws Exception {
        String existing = email();
        JsonNode registered = register(existing, "CANDIDATE");
        AuthResponse linked = auth.googleLogin(existing.toUpperCase(), "Google Name", Role.RECRUITER);
        assertEquals(registered.path("userId").asLong(), linked.userId());
        assertEquals(Role.CANDIDATE, linked.role());
        request(get("/api/auth/me"), linked.token(), null, 200);
        AuthResponse created = auth.googleLogin(email(), "Google Recruiter", Role.RECRUITER);
        assertEquals(Role.RECRUITER, created.role());
        request(get("/api/jobs/mine"), created.token(), null, 200);
    }

    @Test void candidateRecruiterJobResumeAndApplicationWorkflow() throws Exception {
        String candidate = register(email(), "CANDIDATE").path("token").asText();
        String recruiter = register(email(), "RECRUITER").path("token").asText();
        String stranger = register(email(), "RECRUITER").path("token").asText();
        String otherCandidate = register(email(), "CANDIDATE").path("token").asText();
        request(put("/api/candidates/profile"), candidate, Map.of("phone", "5551234567", "location", "Delhi", "headline", "Java Developer",
                "experienceYears", 3, "skills", "Java, Spring Boot, SQL", "education", "BTech", "bio", "Building Java services"), 200);
        request(get("/api/candidates/profile"), candidate, null, 200);
        request(put("/api/recruiters/profile"), recruiter, Map.of("companyName", "Test Company", "designation", "Recruiter"), 200);
        Map<String, Object> jobBody = Map.of("title", "Java Developer", "companyName", "Test Company", "location", "Delhi",
                "jobType", "FULL_TIME", "workMode", "REMOTE", "description", "Build Java Spring Boot services with SQL", "requirements", "Java SQL");
        JsonNode job = request(post("/api/jobs"), recruiter, jobBody, 201);
        long jobId = job.path("id").asLong();
        request(get("/api/jobs"), null, null, 200);
        request(get("/api/jobs/" + jobId), null, null, 200);
        request(get("/api/jobs/mine"), recruiter, null, 200);
        request(post("/api/jobs"), candidate, jobBody, 403);
        request(put("/api/jobs/" + jobId), stranger, jobBody, 403);
        request(put("/api/jobs/" + jobId), recruiter, jobBody, 200);
        JsonNode resume = request(post("/api/resumes/generate"), candidate, Map.of("targetRole", "Java Developer", "skills", "Java, SQL, Spring Boot", "experience", "3 years building APIs"), 201);
        long resumeId = resume.path("id").asLong();
        mvc.perform(get("/api/resumes/" + resumeId + "/content").header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Java")));
        request(get("/api/resumes/" + resumeId + "/content"), otherCandidate, null, 404);
        request(post("/api/resumes/generate"), recruiter, Map.of("targetRole", "Java Developer"), 403);
        Map<String, Object> applicationBody = Map.of("jobId", jobId, "resumeId", resumeId, "coverLetter", "Interested in this Java role");
        JsonNode application = request(post("/api/applications"), candidate, applicationBody, 202);
        long applicationId = application.path("id").asLong();
        request(post("/api/applications"), candidate, applicationBody, 409);
        request(post("/api/applications"), otherCandidate, applicationBody, 404);
        request(get("/api/applications/me"), candidate, null, 200);
        request(get("/api/jobs/" + jobId + "/applications"), recruiter, null, 200);
        JsonNode inbox = request(get("/api/recruiter/applications"), recruiter, null, 200);
        assertEquals("Test User", inbox.get(0).path("candidateName").asText());
        request(get("/api/applications/" + applicationId + "/resume-content"), stranger, null, 403);
        mvc.perform(get("/api/applications/" + applicationId + "/resume-content").header("Authorization", "Bearer " + recruiter)).andExpect(status().isOk());
        JsonNode scored = request(post("/api/applications/" + applicationId + "/recalculate-match"), recruiter, null, 200);
        assertEquals("COMPLETED", scored.path("matchAnalysisStatus").asText());
        assertEquals("HEURISTIC", scored.path("matchSource").asText());
        request(patch("/api/applications/" + applicationId + "/status"), stranger, Map.of("status", "SHORTLISTED"), 403);
        request(patch("/api/applications/" + applicationId + "/status"), recruiter, Map.of("status", "SHORTLISTED"), 200);
        verify(mail).send(any(SimpleMailMessage.class));
        request(patch("/api/applications/" + applicationId + "/withdraw"), otherCandidate, null, 403);
        request(patch("/api/applications/" + applicationId + "/withdraw"), candidate, null, 200);
        request(patch("/api/applications/" + applicationId + "/status"), recruiter, Map.of("status", "HIRED"), 409);
        request(delete("/api/jobs/" + jobId), recruiter, null, 204);
        request(get("/api/jobs/" + jobId), null, null, 404);
    }

    @Test void pdfUploadAnalysisDownloadAndInvalidFileRejection() throws Exception {
        String token = register(email(), "CANDIDATE").path("token").asText();
        byte[] pdf;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700); stream.showText("Test resume: Java SQL Spring Boot, 3 years experience"); stream.endText();
            }
            doc.save(out); pdf = out.toByteArray();
        }
        MvcResult uploaded = mvc.perform(multipart("/api/resumes/upload")
                .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf)).header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted()).andReturn();
        long id = json.readTree(uploaded.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/api/resumes/" + id + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(pdf));
        mvc.perform(get("/api/resumes/" + id + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Java SQL")));
        request(get("/api/resumes/me"), token, null, 200);
        mvc.perform(multipart("/api/resumes/upload").file(new MockMultipartFile("file", "bad.pdf", "application/pdf", "not a PDF".getBytes()))
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/resumes/upload").file(new MockMultipartFile("file", "bad.txt", "text/plain", "text".getBytes()))
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
    }

    private String email() { return "test-" + UUID.randomUUID() + "@example.test"; }
    private JsonNode register(String email, String role) throws Exception {
        return request(post("/api/auth/register"), null, Map.of("name", "Test User", "email", email, "password", "Password123!", "role", role), 201);
    }
    private JsonNode request(MockHttpServletRequestBuilder request, String token, Object body, int status) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        String response = mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }
}
