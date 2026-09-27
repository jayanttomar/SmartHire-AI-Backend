package com.smarthire.controller;

import com.smarthire.dto.ResumeGenerationRequest;
import com.smarthire.entity.Resume;
import com.smarthire.service.ResumeService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    private final ResumeService resumes;

    public ResumeController(ResumeService resumes) {
        this.resumes = resumes;
    }

    @PostMapping("/upload")
    public ResponseEntity<Resume> upload(Principal principal, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resumes.upload(principal.getName(), file));
    }

    @PostMapping("/generate")
    public ResponseEntity<Resume> generate(Principal principal, @Valid @RequestBody ResumeGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumes.generate(principal.getName(), request));
    }

    @GetMapping("/me")
    public List<Resume> mine(Principal principal) {
        return resumes.mine(principal.getName());
    }

    @GetMapping(value = "/{id}/content", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> content(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE + ";charset=" + StandardCharsets.UTF_8)
                .body(resumes.content(principal.getName(), id));
    }

    @GetMapping(value = "/{id}/file", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> file(Principal principal, @PathVariable Long id) {
        Resume resume = resumes.mine(principal.getName(), id);
        Resource file = resumes.file(principal.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resume.getOriginalFileName().replace("\"", "") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
}
