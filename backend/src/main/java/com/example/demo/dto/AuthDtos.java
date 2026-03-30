package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @Size(max = 50) String nickname
    ) {
    }

    public record LoginRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 64) String password
    ) {
    }

    public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresDays,
        UserDtos.UserProfileView user
    ) {
    }

    public record SessionUserView(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        String status,
        Integer level,
        Integer totalExp,
        Integer currentLevelExp,
        Long coinBalance,
        LocalDateTime lastLoginAt
    ) {
    }
}
