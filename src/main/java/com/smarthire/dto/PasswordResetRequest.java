package com.smarthire.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(@NotBlank @Email @Size(max = 160) String email) { }
