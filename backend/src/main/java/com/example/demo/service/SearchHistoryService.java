package com.example.demo.service;

import com.example.demo.dto.SearchDtos;
import com.example.demo.repository.SearchHistoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SearchHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryService.class);

    private final SearchHistoryRepository searchHistoryRepository;

    public SearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Async("taskExecutor")
    public void recordAsync(Long userId, String keyword, String searchType) {
        if (userId == null || keyword == null || keyword.isBlank()) {
            return;
        }
        try {
            searchHistoryRepository.insert(userId, keyword.trim(), searchType);
        } catch (Exception e) {
            log.warn("写入搜索历史失败 userId={} type={}", userId, searchType, e);
        }
    }

    public List<SearchDtos.SearchHistoryView> listRecent(long userId, int limit) {
        return searchHistoryRepository.listRecent(userId, limit);
    }

    public void deleteAllForUser(long userId) {
        searchHistoryRepository.deleteAllForUser(userId);
    }
}
