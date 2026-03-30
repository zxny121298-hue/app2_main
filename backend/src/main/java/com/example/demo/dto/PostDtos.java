package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class PostDtos {

    private PostDtos() {
    }

    public record CreatePostRequest(
        @NotNull Long boardId,
        @NotBlank @Size(max = 200) String title,
        String contentText,
        List<@Size(max = 255) String> imageUrls
    ) {
    }

    public record TogglePinRequest(@NotNull Boolean pinned) {
    }

    public record ToggleFeaturedRequest(@NotNull Boolean featured) {
    }

    public record RewardRequest(@NotNull @Min(1) Long coinAmount) {
    }

    public record PostCardView(
        Long id,
        Long boardId,
        String boardName,
        UserDtos.UserSimpleView author,
        String title,
        String contentText,
        List<String> imageUrls,
        Integer likeCount,
        Integer commentCount,
        Long rewardCoinCount,
        boolean pinned,
        boolean featured,
        String status,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt
    ) {
    }

    public record PostDetailView(
        Long id,
        Long boardId,
        String boardName,
        UserDtos.UserSimpleView author,
        String title,
        String contentText,
        List<String> imageUrls,
        Integer likeCount,
        Integer commentCount,
        Long rewardCoinCount,
        boolean pinned,
        LocalDateTime pinnedAt,
        Long pinnedByUserId,
        boolean featured,
        LocalDateTime featuredAt,
        Long featuredByUserId,
        String status,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
