package com.smarthire.service;

import com.smarthire.dto.CandidateProfileRequest;
import com.smarthire.dto.RecruiterProfileRequest;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.RecruiterProfile;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import com.smarthire.repository.CandidateProfileRepository;
import com.smarthire.repository.RecruiterProfileRepository;
import com.smarthire.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    private final UserRepository users; private final CandidateProfileRepository candidates; private final RecruiterProfileRepository recruiters;
    public ProfileService(UserRepository users, CandidateProfileRepository candidates, RecruiterProfileRepository recruiters) { this.users = users; this.candidates = candidates; this.recruiters = recruiters; }
    public CandidateProfile candidate(String email, CandidateProfileRequest request) {
        User user = user(email, Role.CANDIDATE);
        CandidateProfile profile = candidates.findByUserId(user.getId()).orElseGet(() -> new CandidateProfile(user.getId()));
        if (request != null) { validateCandidateProfile(request); profile.update(request.phone(), request.location(), request.headline(), request.experienceYears(), request.skills(), request.education(), request.dateOfBirth(), request.gender(), request.preferredWorkMode(), request.linkedinUrl(), request.githubUrl(), request.portfolioUrl(), request.bio()); }
        return request == null ? profile : candidates.save(profile);
    }
    public RecruiterProfile recruiter(String email, RecruiterProfileRequest request) {
        User user = user(email, Role.RECRUITER);
        RecruiterProfile profile = recruiters.findByUserId(user.getId()).orElseGet(() -> new RecruiterProfile(user.getId()));
        if (request != null) { profile.update(request.companyName(), request.companyWebsite(), request.designation(), request.companyDescription()); }
        return request == null ? profile : recruiters.save(profile);
    }
    private User user(String email, Role role) { User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); if (user.getRole() != role) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This endpoint is not available for your role."); return user; }
    private void validateCandidateProfile(CandidateProfileRequest request) {
        if (blank(request.phone()) || blank(request.location()) || blank(request.headline()) || request.experienceYears() == null || blank(request.skills()) || blank(request.education()) || blank(request.bio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fill in phone, location, headline, experience, skills, education, and about you before saving.");
        }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
