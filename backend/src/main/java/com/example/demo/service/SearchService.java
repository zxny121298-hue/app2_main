package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.common.PageResult;
import com.example.demo.dto.SearchDtos;
import com.example.demo.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final int KEYWORD_MAX_LEN = 200;

    private final PostService postService;
    private final UserService userService;
    private final BoardService boardService;
    private final SearchHistoryService searchHistoryService;

    public SearchService(PostService postService, UserService userService, BoardService boardService,
                         SearchHistoryService searchHistoryService) {
        this.postService = postService;
        this.userService = userService;
        this.boardService = boardService;
        this.searchHistoryService = searchHistoryService;
    }

    public SearchDtos.UnifiedSearchResponse search(String type, String qOrKeyword, long page, long pageSize) {
        String normalizedType = normalizeType(type);
        String keyword = trimToNull(qOrKeyword);
        if (keyword != null && keyword.length() > KEYWORD_MAX_LEN) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "关键词过长");
        }

        Long userId = currentUserIdOrNull();

        SearchDtos.UnifiedSearchResponse response;
        if (keyword == null) {
            response = emptyResponse(normalizedType, page, pageSize);
        } else {
            response = switch (normalizedType) {
                case "content" -> SearchDtos.UnifiedSearchResponse.ofPosts(normalizedType, postService.pageSearchPosts(keyword, page, pageSize));
                case "user" -> SearchDtos.UnifiedSearchResponse.ofUsers(normalizedType, userService.pageSearchUsers(keyword, page, pageSize));
                case "topic" -> SearchDtos.UnifiedSearchResponse.ofTopics(normalizedType, boardService.pageSearchBoards(keyword, page, pageSize));
                default -> throw new BusinessException(ErrorCodes.BAD_REQUEST, "搜索类型不合法");
            };
        }

        if (userId != null && keyword != null) {
            searchHistoryService.recordAsync(userId, keyword, normalizedType);
        }
        return response;
    }

    public List<SearchDtos.SearchHistoryView> listMyHistory(int limit) {
        var principal = SecurityUtils.requireUser();
        return searchHistoryService.listRecent(principal.userId(), limit);
    }

    public void clearMyHistory() {
        var principal = SecurityUtils.requireUser();
        searchHistoryService.deleteAllForUser(principal.userId());
    }

    private SearchDtos.UnifiedSearchResponse emptyResponse(String normalizedType, long page, long pageSize) {
        return switch (normalizedType) {
            case "content" -> SearchDtos.UnifiedSearchResponse.ofPosts(normalizedType, emptyPage(page, pageSize));
            case "user" -> SearchDtos.UnifiedSearchResponse.ofUsers(normalizedType, emptyPage(page, pageSize));
            case "topic" -> SearchDtos.UnifiedSearchResponse.ofTopics(normalizedType, emptyPage(page, pageSize));
            default -> throw new BusinessException(ErrorCodes.BAD_REQUEST, "搜索类型不合法");
        };
    }

    private static String normalizeType(String type) {
        BizAssert.notNull(type, ErrorCodes.BAD_REQUEST, "搜索类型不能为空");
        String t = type.trim().toLowerCase();
        BizAssert.isTrue("content".equals(t) || "user".equals(t) || "topic".equals(t), ErrorCodes.BAD_REQUEST, "搜索类型不合法");
        return t;
    }

    private static Long currentUserIdOrNull() {
        var p = SecurityUtils.getCurrentUserOrNull();
        return p == null ? null : p.userId();
    }

    private static <T> PageResult<T> emptyPage(long page, long pageSize) {
        return new PageResult<>(0, page, pageSize, List.of());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
