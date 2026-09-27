package com.smarthire.dto;
import com.smarthire.enums.ApplicationStatus; import jakarta.validation.constraints.NotNull;
public record ApplicationStatusRequest(@NotNull ApplicationStatus status) { }
