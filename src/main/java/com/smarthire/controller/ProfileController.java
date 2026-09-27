package com.smarthire.controller;

import com.smarthire.dto.CandidateProfileRequest;
import com.smarthire.dto.RecruiterProfileRequest;
import com.smarthire.entity.CandidateProfile;
import com.smarthire.entity.RecruiterProfile;
import com.smarthire.service.ProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles = profiles; }
    @GetMapping("/candidates/profile") public CandidateProfile candidate(Principal principal) { return profiles.candidate(principal.getName(), null); }
    @PutMapping("/candidates/profile") public CandidateProfile updateCandidate(Principal principal, @Valid @RequestBody CandidateProfileRequest request) { return profiles.candidate(principal.getName(), request); }
    @GetMapping("/recruiters/profile") public RecruiterProfile recruiter(Principal principal) { return profiles.recruiter(principal.getName(), null); }
    @PutMapping("/recruiters/profile") public RecruiterProfile updateRecruiter(Principal principal, @Valid @RequestBody RecruiterProfileRequest request) { return profiles.recruiter(principal.getName(), request); }
}
