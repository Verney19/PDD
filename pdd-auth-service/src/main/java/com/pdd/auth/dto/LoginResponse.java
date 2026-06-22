package com.pdd.auth.dto;

public record LoginResponse(
        Long userId,
        String username,
        String role,
        String token
) {
}
