package com.smarthire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long userId;
    private String phone;
    private String location;
    private String headline;
    private Integer experienceYears;
    @Column(length = 1000) private String skills;
    @Column(length = 500) private String education;
    private String dateOfBirth;
    private String gender;
    private String preferredWorkMode;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    @Column(length = 2000) private String bio;
    protected CandidateProfile() { }
    public CandidateProfile(Long userId) { this.userId = userId; }
    public void update(String phone, String location, String linkedinUrl, String githubUrl, String portfolioUrl, String bio) { update(phone, location, null, null, null, null, null, null, null, linkedinUrl, githubUrl, portfolioUrl, bio); }
    public void update(String phone, String location, String headline, Integer experienceYears, String skills, String education, String dateOfBirth, String gender, String preferredWorkMode, String linkedinUrl, String githubUrl, String portfolioUrl, String bio) { this.phone = phone; this.location = location; this.headline = headline; this.experienceYears = experienceYears; this.skills = skills; this.education = education; this.dateOfBirth = dateOfBirth; this.gender = gender; this.preferredWorkMode = preferredWorkMode; this.linkedinUrl = linkedinUrl; this.githubUrl = githubUrl; this.portfolioUrl = portfolioUrl; this.bio = bio; }
    public Long getId() { return id; } public Long getUserId() { return userId; } public String getPhone() { return phone; } public String getLocation() { return location; } public String getHeadline() { return headline; } public Integer getExperienceYears() { return experienceYears; } public String getSkills() { return skills; } public String getEducation() { return education; } public String getDateOfBirth() { return dateOfBirth; } public String getGender() { return gender; } public String getPreferredWorkMode() { return preferredWorkMode; } public String getLinkedinUrl() { return linkedinUrl; } public String getGithubUrl() { return githubUrl; } public String getPortfolioUrl() { return portfolioUrl; } public String getBio() { return bio; }
}
