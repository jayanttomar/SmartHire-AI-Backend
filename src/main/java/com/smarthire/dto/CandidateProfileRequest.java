package com.smarthire.dto;
import jakarta.validation.constraints.Size;
public record CandidateProfileRequest(@Size(max=40) String phone, @Size(max=120) String location, @Size(max=120) String headline, Integer experienceYears, @Size(max=1000) String skills, @Size(max=500) String education, @Size(max=30) String dateOfBirth, @Size(max=30) String gender, @Size(max=30) String preferredWorkMode, @Size(max=255) String linkedinUrl, @Size(max=255) String githubUrl, @Size(max=255) String portfolioUrl, @Size(max=2000) String bio) { }
