package com.smarthire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recruiter_profiles")
public class RecruiterProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long userId;
    private String companyName;
    private String companyWebsite;
    private String designation;
    @Column(length = 2000) private String companyDescription;
    protected RecruiterProfile() { }
    public RecruiterProfile(Long userId) { this.userId = userId; }
    public void update(String companyName, String companyWebsite, String designation, String companyDescription) { this.companyName = companyName; this.companyWebsite = companyWebsite; this.designation = designation; this.companyDescription = companyDescription; }
    public String getCompanyName() { return companyName; } public String getCompanyWebsite() { return companyWebsite; } public String getDesignation() { return designation; } public String getCompanyDescription() { return companyDescription; }
}
