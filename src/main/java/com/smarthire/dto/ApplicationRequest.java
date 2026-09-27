package com.smarthire.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @NotNull Long jobId,
        @NotNull Long resumeId,
        @Size(max = 3000) String coverLetter) { }
