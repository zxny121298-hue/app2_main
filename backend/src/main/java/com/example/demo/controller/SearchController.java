package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.SearchDtos;
import com.example.demo.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ApiResponse<SearchDtos.UnifiedSearchResponse> search(
        @RequestParam String type,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        String raw = q != null ? q : keyword;
        return ApiResponse.ok(searchService.search(type, raw, page, pageSize));
    }

    @GetMapping("/search/history")
    public ApiResponse<List<SearchDtos.SearchHistoryView>> searchHistory(
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return ApiResponse.ok(searchService.listMyHistory(limit));
    }

    @DeleteMapping("/search/history")
    public ApiResponse<Void> clearSearchHistory() {
        searchService.clearMyHistory();
        return ApiResponse.ok(null);
    }
}
