package com.smarthire.service;

import com.smarthire.dto.ResumeGenerationRequest;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.User;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds a no-network, ATS-readable resume from candidate supplied facts. */
final class ResumeTemplateBuilder {
    private static final int MAX_CONTENT_LENGTH = 28_000;

    private ResumeTemplateBuilder() { }

    static String build(User user, CandidateProfile profile, ResumeGenerationRequest request) {
        List<String> lines = new ArrayList<>();
        String role = text(request.targetRole());
        String name = text(user.getName());

        lines.add(name.isBlank() ? "CANDIDATE" : name.toUpperCase());
        lines.add(role);
        addContact(lines, user, profile);
        lines.add("");

        heading(lines, "PROFESSIONAL SUMMARY");
        String summary = firstPresent(request.summary(), profile.getBio());
        if (summary.isBlank()) {
            String skills = skills(request.skills());
            lines.add("Candidate seeking a " + role + " opportunity" +
                    (skills.isBlank() ? "." : " with strengths in " + skills + "."));
        } else {
            lines.add(summary);
        }
        lines.add("");

        heading(lines, "CORE SKILLS");
        String skills = skills(request.skills());
        lines.add(skills.isBlank() ? "Add the technical and professional skills most relevant to the role." : skills);
        lines.add("");

        heading(lines, "PROFESSIONAL EXPERIENCE");
        addNarrativeSection(lines, request.experience(), "Add your most relevant work experience, internships, or freelance work.");
        lines.add("");

        heading(lines, "EDUCATION");
        addNarrativeSection(lines, request.education(), "Add your education, training, or relevant coursework.");
        lines.add("");

        heading(lines, "PROJECTS");
        addNarrativeSection(lines, request.projects(), "Add projects that demonstrate the skills required for your target role.");

        return truncate(String.join("\n", lines).strip() + "\n");
    }

    private static void addContact(List<String> lines, User user, CandidateProfile profile) {
        List<String> contact = new ArrayList<>();
        addIfPresent(contact, user.getEmail());
        addIfPresent(contact, profile.getPhone());
        addIfPresent(contact, profile.getLocation());
        lines.add(String.join(" | ", contact));

        List<String> links = new ArrayList<>();
        addLabeled(links, "LinkedIn", profile.getLinkedinUrl());
        addLabeled(links, "GitHub", profile.getGithubUrl());
        addLabeled(links, "Portfolio", profile.getPortfolioUrl());
        if (!links.isEmpty()) {
            lines.add(String.join(" | ", links));
        }
    }

    private static void addNarrativeSection(List<String> lines, String value, String emptyState) {
        if (text(value).isBlank()) {
            lines.add(emptyState);
            return;
        }
        addBullets(lines, value);
    }

    private static void heading(List<String> lines, String heading) {
        lines.add(heading);
        lines.add("-".repeat(heading.length()));
    }

    private static void addBullets(List<String> lines, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String part : value.replace('\r', '\n').split("\\n+")) {
            String bullet = text(part);
            if (!bullet.isBlank()) {
                lines.add("- " + bullet);
            }
        }
    }

    private static String skills(String suppliedSkills) {
        Set<String> unique = new LinkedHashSet<>();
        for (String skill : text(suppliedSkills).split("[,\\n]+")) {
            String clean = text(skill);
            if (!clean.isBlank()) {
                unique.add(clean);
            }
        }
        return String.join(", ", unique);
    }

    private static void addIfPresent(List<String> values, String value) {
        String clean = text(value);
        if (!clean.isBlank()) {
            values.add(clean);
        }
    }

    private static void addLabeled(List<String> values, String label, String value) {
        String clean = text(value);
        if (!clean.isBlank()) {
            values.add(label + ": " + clean);
        }
    }

    private static String firstPresent(String preferred, String fallback) {
        String clean = text(preferred);
        return clean.isBlank() ? text(fallback) : clean;
    }

    private static String text(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u0000', ' ').replaceAll("[\\t\\f\\v ]+", " ").trim();
    }

    private static String truncate(String content) {
        return content.length() <= MAX_CONTENT_LENGTH ? content : content.substring(0, MAX_CONTENT_LENGTH - 4).stripTrailing() + "...\n";
    }
}
