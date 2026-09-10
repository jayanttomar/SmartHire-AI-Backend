package com.smarthire.dto;

import com.smarthire.enums.Role;

public record AuthResponse(String token, String tokenType, Long userId, String name, String email, Role role) { }
