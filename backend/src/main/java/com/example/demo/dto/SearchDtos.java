package com.example.demo.dto;

import com.example.demo.common.PageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

public final class SearchDtos {

    private SearchDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UnifiedSearchResponse(
        String type,
        PageResult<PostDtos.PostCardView> posts,
        PageResult<UserDtos.UserProfileView> users,
        PageResult<BoardDtos.BoardView> topics
    ) {
        public static UnifiedSearchResponse ofPosts(String type, PageResult<PostDtos.PostCardView> posts) {
            return new UnifiedSearchResponse(type, posts, null, null);
        }

        public static UnifiedSearchResponse ofUsers(String type, PageResult<UserDtos.UserProfileView> users) {
            return new UnifiedSearchResponse(type, null, users, null);
        }

        public static UnifiedSearchResponse ofTopics(String type, PageResult<BoardDtos.BoardView> topics) {
            return new UnifiedSearchResponse(type, null, null, topics);
        }
    }

    public record SearchHistoryView(
        Long id,
        String keyword,
        String searchType,
        LocalDateTime createdAt
    ) {
    }
}
