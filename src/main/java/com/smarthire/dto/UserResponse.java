package com.smarthire.dto;
import com.smarthire.enums.Role;
public record UserResponse(Long id, String name, String email, Role role) { }
