package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ForumModels {

    private ForumModels() {
    }

    public record UserAccount(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String passwordHash,
        Integer tokenVersion,
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
        LocalDateTime lastLoginAt,
        LocalDateTime updatedAt
    ) {
    }

    public record BoardRecord(
        Long id,
        String name,
        String description,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record PostRecord(
        Long id,
        Long boardId,
        Long userId,
        String title,
        String contentText,
        Integer likeCount,
        Integer commentCount,
        Long rewardCoinCount,
        Boolean isPinned,
        LocalDateTime pinnedAt,
        Long pinnedByUserId,
        Boolean isFeatured,
        LocalDateTime featuredAt,
        Long featuredByUserId,
        String status,
        LocalDateTime deletedAt,
        Long deletedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record PostImageRecord(Long id, Long postId, String imageUrl, Integer sortOrder) {
    }

    public record CommentRecord(
        Long id,
        Long postId,
        Long userId,
        Long parentCommentId,
        Long replyToUserId,
        String contentText,
        Integer likeCount,
        Long rewardCoinCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CommentImageRecord(Long id, Long commentId, String imageUrl, Integer sortOrder) {
    }

    public record ConversationRecord(
        Long id,
        Long user1Id,
        Long user2Id,
        Long lastMessageId,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
    ) {
    }

    public record ConversationMemberRecord(
        Long id,
        Long conversationId,
        Long userId,
        LocalDateTime joinedAt,
        Long lastReadMessageId,
        Integer lastReadSequenceNo,
        LocalDateTime lastReadAt,
        Integer unreadCount,
        Boolean isPinned,
        LocalDateTime pinnedAt,
        Boolean isMuted,
        Boolean isDeleted,
        LocalDateTime deletedAt
    ) {
    }

    public record MessageRecord(
        Long id,
        Long conversationId,
        Long senderUserId,
        Integer sequenceNo,
        String contentText,
        LocalDateTime createdAt
    ) {
    }

    public record MessageImageRecord(Long id, Long messageId, String imageUrl, Integer sortOrder) {
    }

    public record NotificationGroupRecord(
        Long id,
        Long recipientUserId,
        String eventType,
        String targetType,
        Long targetId,
        Long latestActorUserId,
        Integer totalCount,
        Integer unreadCount,
        LocalDateTime lastReadAt,
        LocalDateTime latestAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record NotificationItemRecord(
        Long id,
        Long groupId,
        Long recipientUserId,
        Long actorUserId,
        String eventType,
        String targetType,
        Long targetId,
        Long conversationId,
        Long messageId,
        Long postId,
        Long commentId,
        Long rewardId,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
    ) {
    }

    public record RewardRecord(
        Long id,
        Long senderUserId,
        Long recipientUserId,
        Long postId,
        Long commentId,
        Long coinAmount,
        LocalDateTime createdAt
    ) {
    }

    public record CheckInRecord(
        Long id,
        Long userId,
        LocalDate checkInDate,
        Integer expGain,
        LocalDateTime createdAt
    ) {
    }

    public record UserExpLogRecord(
        Long id,
        Long userId,
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

    public record LevelRuleRecord(
        Long id,
        Integer level,
        String levelName,
        Integer upgradeNeedExp
    ) {
    }
}
