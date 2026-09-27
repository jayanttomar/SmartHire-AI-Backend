package com.smarthire.dto;
import jakarta.validation.constraints.NotBlank;
public record OAuthCodeRequest(@NotBlank String code) { }
