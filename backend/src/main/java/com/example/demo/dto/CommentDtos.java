package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record CreateCommentRequest(
        Long parentCommentId,
        Long replyToUserId,
        String contentText,
        List<@Size(max = 255) String> imageUrls
    ) {
    }

    public record CommentView(
        Long id,
        Long postId,
        Long parentCommentId,
        Long replyToUserId,
        UserDtos.UserSimpleView author,
        UserDtos.UserSimpleView replyToUser,
        String contentText,
        List<String> imageUrls,
        Integer likeCount,
        Long rewardCoinCount,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentView> children
    ) {
    }
}
