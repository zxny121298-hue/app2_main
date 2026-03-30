package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.BoardRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BoardRepository extends BaseRepository {

    private static final RowMapper<BoardRecord> BOARD_ROW_MAPPER = BoardRepository::mapBoard;

    public BoardRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public List<BoardRecord> listBoards(boolean includeDisabled) {
        if (includeDisabled) {
            return jdbcTemplate.query("""
                select * from `boards`
                order by sort_order asc, id asc
                """, BOARD_ROW_MAPPER);
        }
        return jdbcTemplate.query("""
            select * from `boards`
            where status = 'enabled'
            order by sort_order asc, id asc
            """, BOARD_ROW_MAPPER);
    }

    public Optional<BoardRecord> findById(Long id) {
        return queryOptional("select * from `boards` where id = :id", Map.of("id", id), BOARD_ROW_MAPPER);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `boards` where name = :name
            """, Map.of("name", name), Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByNameExcludingId(String name, Long id) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `boards`
            where name = :name and id <> :id
            """, Map.of("name", name, "id", id), Integer.class);
        return count != null && count > 0;
    }

    public long createBoard(String name, String description, int sortOrder, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("name", name)
            .addValue("description", description)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status);
        return insertAndReturnId("""
            insert into `boards` (name, description, sort_order, status)
            values (:name, :description, :sortOrder, :status)
            """, params);
    }

    public void updateBoard(Long boardId, String name, String description, int sortOrder, String status) {
        jdbcTemplate.update("""
            update `boards`
            set name = :name,
                description = :description,
                sort_order = :sortOrder,
                status = :status
            where id = :boardId
            """, new MapSqlParameterSource()
            .addValue("name", name)
            .addValue("description", description)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("boardId", boardId));
    }

    public void updateStatus(Long boardId, String status) {
        jdbcTemplate.update("""
            update `boards`
            set status = :status
            where id = :boardId
            """, Map.of("status", status, "boardId", boardId));
    }

    public long countBoards() {
        Long total = jdbcTemplate.queryForObject("select count(*) from `boards`", new HashMap<>(), Long.class);
        return total == null ? 0L : total;
    }

    public PageResult<BoardRecord> pageSearchBoards(String keyword, long page, long pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("kw", "%" + keyword + "%");
        Long totalValue = jdbcTemplate.queryForObject("""
            select count(*) from `boards`
            where status = 'enabled'
              and (name like :kw or coalesce(description, '') like :kw)
            """, params, Long.class);
        long total = totalValue == null ? 0L : totalValue;

        params.addValue("limit", pageSize);
        params.addValue("offset", offset(page, pageSize));
        List<BoardRecord> list = jdbcTemplate.query("""
            select * from `boards`
            where status = 'enabled'
              and (name like :kw or coalesce(description, '') like :kw)
            order by sort_order asc, id asc
            limit :limit offset :offset
            """, params, BOARD_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    private static BoardRecord mapBoard(ResultSet rs, int rowNum) throws SQLException {
        return new BoardRecord(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("sort_order"),
            rs.getString("status"),
            getDateTime(rs, "created_at"),
            getDateTime(rs, "updated_at")
        );
    }
}
