package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record UpdateProfileRequest(
        @Size(max = 50) String nickname,
        @Size(max = 255) String avatarUrl,
        @Size(max = 500) String bio
    ) {
    }

    public record ChangePasswordRequest(
        @NotBlank @Size(min = 6, max = 64) String oldPassword,
        @NotBlank @Size(min = 6, max = 64) String newPassword
    ) {
    }

    public record AdminBanRequest(
        LocalDateTime bannedUntilAt,
        @Size(max = 255) String reason
    ) {
    }

    public record AdminMuteRequest(
        LocalDateTime mutedUntilAt,
        @Size(max = 255) String reason
    ) {
    }

    public record AdminAdjustExpRequest(
        int changeExp,
        @Size(max = 255) String remark
    ) {
    }

    public record AdminAdjustCoinRequest(
        long changeAmount,
        @Size(max = 255) String remark
    ) {
    }

    public record UserProfileView(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        String status,
        LocalDateTime bannedUntilAt,
        String banReason,
        LocalDateTime mutedUntilAt,
        String muteReason,
        Long coinBalance,
        Integer level,
        Integer totalExp,
        Integer currentLevelExp,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
    ) {
    }

    public record CheckInView(
        boolean checkedInToday,
        LocalDateTime checkedAt,
        int expGain,
        long coinGain,
        Integer level,
        Integer totalExp,
        Integer currentLevelExp
    ) {
    }

    public record ExpLogView(
        Long id,
        String changeType,
        Integer changeExp,
        Integer totalExpAfter,
        Integer levelAfter,
        Long postId,
        Long commentId,
        Long operatorUserId,
        String remark,
        LocalDateTime createdAt
    ) {
    }

    public record UserSimpleView(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        String status,
        Integer level
    ) {
    }
}
