package com.smarthire.dto;
import jakarta.validation.constraints.Size;
public record RecruiterProfileRequest(@Size(max=160) String companyName, @Size(max=255) String companyWebsite, @Size(max=120) String designation, @Size(max=2000) String companyDescription) { }
