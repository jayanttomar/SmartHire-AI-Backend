package com.smarthire.controller;

import com.smarthire.dto.ApplicationRequest;
import com.smarthire.dto.ApplicationStatusRequest;
import com.smarthire.entity.Application;
import com.smarthire.service.ApplicationService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApplicationController {
    private final ApplicationService applications;

    public ApplicationController(ApplicationService applications) { this.applications = applications; }

    @PostMapping("/applications")
    public ResponseEntity<Application> apply(Principal principal, @Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(applications.apply(principal.getName(), request));
    }

    @GetMapping("/applications/me")
    public List<Application> mine(Principal principal) { return applications.mine(principal.getName()); }

    @GetMapping("/jobs/{jobId}/applications")
    public List<Application> forJob(Principal principal, @PathVariable Long jobId) {
        return applications.forJob(principal.getName(), jobId);
    }

    @GetMapping("/recruiter/applications")
    public List<Application> forRecruiter(Principal principal) {
        return applications.forRecruiter(principal.getName());
    }

    @GetMapping(value = "/applications/{id}/resume-content", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> resumeContent(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE + ";charset=" + StandardCharsets.UTF_8)
                .body(applications.resumeContent(principal.getName(), id));
    }

    /** Streams the original submitted PDF only to the recruiter who owns the job application. */
    @GetMapping(value = "/applications/{id}/resume-file", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> resumeFile(Principal principal, @PathVariable Long id) {
        ApplicationService.SubmittedResume file = applications.resumeFile(principal.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName().replace("\"", "") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.resource());
    }

    @PatchMapping("/applications/{id}/status")
    public Application updateStatus(Principal principal, @PathVariable Long id,
                                    @Valid @RequestBody ApplicationStatusRequest request) {
        return applications.updateStatus(principal.getName(), id, request.status());
    }

    @PatchMapping("/applications/{id}/withdraw")
    public Application withdraw(Principal principal, @PathVariable Long id) {
        return applications.withdraw(principal.getName(), id);
    }

    @PostMapping("/applications/{id}/recalculate-match")
    public Application recalculateMatch(Principal principal, @PathVariable Long id) {
        return applications.recalculateMatch(principal.getName(), id);
    }
}
