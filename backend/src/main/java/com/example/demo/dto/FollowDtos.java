package com.example.demo.dto;

import java.time.LocalDateTime;

public final class FollowDtos {

    private FollowDtos() {
    }

    public record FollowRelationView(
        UserDtos.UserSimpleView user,
        LocalDateTime followedAt,
        boolean following
    ) {
    }

    public record FollowStatusView(
        Long targetUserId,
        boolean following
    ) {
    }
}
