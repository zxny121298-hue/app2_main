package com.example.demo.repository;

import com.example.demo.dto.SearchDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SearchHistoryRepository extends BaseRepository {

    private static final RowMapper<SearchDtos.SearchHistoryView> ROW_MAPPER = SearchHistoryRepository::mapRow;

    public SearchHistoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public void insert(Long userId, String keyword, String searchType) {
        jdbcTemplate.update("""
            insert into `search_history` (`user_id`, `keyword`, `search_type`)
            values (:userId, :keyword, :searchType)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("keyword", keyword)
            .addValue("searchType", searchType));
    }

    public void deleteAllForUser(Long userId) {
        jdbcTemplate.update(
            "delete from `search_history` where `user_id` = :userId",
            Map.of("userId", userId)
        );
    }

    public List<SearchDtos.SearchHistoryView> listRecent(Long userId, int limit) {
        return jdbcTemplate.query("""
            select `id`, `user_id`, `keyword`, `search_type`, `created_at`
            from `search_history`
            where `user_id` = :userId
            order by `created_at` desc, `id` desc
            limit :limit
            """, Map.of("userId", userId, "limit", limit), ROW_MAPPER);
    }

    private static SearchDtos.SearchHistoryView mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SearchDtos.SearchHistoryView(
            rs.getLong("id"),
            rs.getString("keyword"),
            rs.getString("search_type"),
            getDateTime(rs, "created_at")
        );
    }
}
