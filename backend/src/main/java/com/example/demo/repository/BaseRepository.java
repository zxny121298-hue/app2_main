package com.example.demo.repository;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

public abstract class BaseRepository {

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected BaseRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected <T> Optional<T> queryOptional(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
        List<T> list = jdbcTemplate.query(sql, params, rowMapper);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getFirst());
    }

    protected long insertAndReturnId(String sql, MapSqlParameterSource params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException(ErrorCodes.SERVER_ERROR, "写入数据失败");
        }
        return key.longValue();
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return rs.getDate(column) == null ? null : rs.getDate(column).toLocalDate();
    }

    protected static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column) == null ? null : rs.getBoolean(column);
    }

    protected static Long getLongObject(ResultSet rs, String column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.longValue();
    }

    protected static Integer getIntegerObject(ResultSet rs, String column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.intValue();
    }

    protected static long offset(long page, long pageSize) {
        return Math.max(page - 1, 0) * pageSize;
    }

    protected static boolean hasIds(Collection<Long> ids) {
        return ids != null && !ids.isEmpty();
    }
}
