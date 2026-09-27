package com.smarthire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.entity.Application;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import com.smarthire.enums.MatchSource;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.ResumeRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class AiAnalysisService {
    private final ResumeRepository resumes;
    private final ApplicationRepository applications;
    private final ObjectMapper json;
    private final String apiKey;
    private final String model;

    public AiAnalysisService(ResumeRepository resumes, ApplicationRepository applications, ObjectMapper json,
                             @Value("${anthropic.api-key:}") String apiKey,
                             @Value("${anthropic.model}") String model) {
        this.resumes = resumes; this.applications = applications; this.json = json;
        this.apiKey = apiKey; this.model = model;
    }

    @Async
    public void analyzeResume(Long resumeId) { analyzeResumeNow(resumeId); }

    public void analyzeResumeNow(Long resumeId) {
        resumes.findById(resumeId).ifPresent(resume -> {
            try {
                JsonNode result = ask("Return only JSON with keys skills (array), summary (string), experience (string). Analyze this resume:\n" + resume.getExtractedText());
                resume.completeAnalysis(array(result, "skills"), text(result, "summary"), text(result, "experience"));
            } catch (Exception ignored) {
                resume.completeAnalysis(localSkills(resume.getExtractedText()), localSummary(resume.getExtractedText()), localExperience(resume.getExtractedText()));
            }
            resumes.save(resume);
        });
    }

    @Async
    public void scoreApplication(Long applicationId, Job job, Resume resume) {
        scoreApplicationNow(applicationId, job, resume);
    }

    /** Synchronous retry used only when a recruiter explicitly requests a recomputation. */
    public void scoreApplicationNow(Long applicationId, Job job, Resume resume) {
        applications.findById(applicationId).ifPresent(application -> {
            application.markMatchProcessing();
            applications.save(application);

            Set<String> required = skillsIn(job.getDescription() + " " + job.getRequirements());
            if (required.isEmpty()) {
                application.markMatchUnavailable("Match is unavailable because this job does not include recognizable skills. Add concrete requirements such as Java, React, SQL, Figma, or Python, then recalculate.");
                applications.save(application);
                return;
            }

            try {
                JsonNode result = ask("Return only JSON with keys score (integer 0-100), reasoning (string), strengths (array), missingSkills (array). Compare resume to job. JOB:\n"
                        + job.getDescription() + "\nREQUIREMENTS:\n" + job.getRequirements() + "\nRESUME:\n" + resume.getExtractedText());
                JsonNode scoreNode = result.path("score");
                if (!scoreNode.canConvertToInt()) throw new IllegalStateException("AI response does not contain a score");
                application.completeMatch(scoreNode.asInt(), text(result, "reasoning"), array(result, "strengths"), array(result, "missingSkills"), MatchSource.AI);
            } catch (Exception ignored) {
                Set<String> candidate = skillsIn(resume.getExtractedText());
                Set<String> matched = new LinkedHashSet<>(required);
                matched.retainAll(candidate);
                Set<String> missing = new LinkedHashSet<>(required);
                missing.removeAll(candidate);
                int score = (int) Math.round(matched.size() * 100.0 / required.size());
                String reasoning = matched.isEmpty()
                        ? "No required skills were detected in the selected resume."
                        : "Local matching found " + matched.size() + " of " + required.size() + " required skills.";
                application.completeMatch(score, reasoning, toJson(matched), toJson(missing), MatchSource.HEURISTIC);
            }
            applications.save(application);
        });
    }

    public boolean hasRecognizableJobSkills(Job job) {
        return !skillsIn(job.getDescription() + " " + job.getRequirements()).isEmpty();
    }

    private JsonNode ask(String prompt) throws Exception {
        if (apiKey.isBlank() || apiKey.startsWith("your_") || apiKey.startsWith("replace_")) throw new IllegalStateException("No API key");
        Map<String, Object> body = Map.of("model", model, "max_tokens", 700,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        JsonNode response = RestClient.builder().baseUrl("https://api.anthropic.com").requestFactory(factory).build().post().uri("/v1/messages")
                .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01")
                .body(body).retrieve().body(JsonNode.class);
        String content = response.path("content").path(0).path("text").asText();
        int start = content.indexOf('{'), end = content.lastIndexOf('}');
        return json.readTree(start >= 0 && end > start ? content.substring(start, end + 1) : content);
    }

    private String text(JsonNode node, String key) { return node.path(key).asText(""); }
    private String array(JsonNode node, String key) { try { return json.writeValueAsString(node.path(key)); } catch (Exception e) { return "[]"; } }

    private Set<String> skillsIn(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            Pattern word = Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(skill.toLowerCase(Locale.ROOT)) + "(?![\\p{L}\\p{N}_])");
            if (word.matcher(lower).find()) found.add(skill);
        }
        return found;
    }

    private String localSkills(String resumeText) { return toJson(skillsIn(resumeText)); }
    private String localSummary(String resumeText) {
        String compact = resumeText == null ? "" : resumeText.replaceAll("\\s+", " ").trim();
        if (compact.isBlank()) return "Local analysis could not extract readable text from this PDF.";
        return "Local analysis: " + compact.substring(0, Math.min(compact.length(), 320));
    }
    private String localExperience(String resumeText) {
        Matcher matcher = Pattern.compile("(?i)(\\d{1,2})\\+?\\s*(years|yrs)").matcher(resumeText == null ? "" : resumeText);
        return matcher.find() ? matcher.group(1) + "+ years of experience mentioned in the resume." : "Experience details are available in the uploaded resume.";
    }
    private String toJson(Set<String> values) { try { return json.writeValueAsString(new ArrayList<>(values)); } catch (Exception e) { return "[]"; } }
    private static final List<String> KNOWN_SKILLS = List.of("Java", "Spring Boot", "Spring", "React", "JavaScript", "TypeScript", "Python", "SQL", "PostgreSQL", "MySQL", "MongoDB", "AWS", "Docker", "Kubernetes", "Git", "REST", "HTML", "CSS", "Node.js", "Angular", "Figma", "Machine Learning", "AI", "Excel", "Power BI");
}
