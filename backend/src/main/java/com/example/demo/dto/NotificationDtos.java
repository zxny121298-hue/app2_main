package com.example.demo.dto;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationGroupView(
        Long id,
        String eventType,
        String targetType,
        Long targetId,
        Integer totalCount,
        Integer unreadCount,
        UserDtos.UserSimpleView latestActor,
        LocalDateTime latestAt,
        LocalDateTime lastReadAt
    ) {
    }

    public record NotificationItemView(
        Long id,
        Long groupId,
        String eventType,
        String targetType,
        Long targetId,
        UserDtos.UserSimpleView actor,
        Long conversationId,
        Long messageId,
        Long postId,
        Long commentId,
        Long rewardId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
    ) {
    }

    public record NotificationUnreadView(int unreadCount) {
    }
}
