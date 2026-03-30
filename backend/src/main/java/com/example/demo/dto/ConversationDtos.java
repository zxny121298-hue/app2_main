package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record CreateConversationRequest(@NotNull Long targetUserId) {
    }

    public record SendMessageRequest(
        String contentText,
        List<@Size(max = 255) String> imageUrls
    ) {
    }

    public record ConversationSettingRequest(@NotNull Boolean value) {
    }

    public record MessageView(
        Long id,
        Long conversationId,
        Integer sequenceNo,
        Long senderUserId,
        String contentText,
        List<String> imageUrls,
        LocalDateTime createdAt
    ) {
    }

    public record ConversationListView(
        Long id,
        UserDtos.UserSimpleView peerUser,
        MessageView lastMessage,
        Integer unreadCount,
        boolean pinned,
        boolean muted,
        LocalDateTime updatedAt
    ) {
    }

    public record ConversationDetailView(
        Long id,
        UserDtos.UserSimpleView peerUser,
        Integer unreadCount,
        boolean pinned,
        boolean muted,
        Long lastReadMessageId,
        Integer lastReadSequenceNo,
        LocalDateTime lastReadAt,
        List<MessageView> messages
    ) {
    }
}
