package com.example.demo.repository;

import com.example.demo.model.ForumEnums.UserRole;
import com.example.demo.model.ForumEnums.UserStatus;
import com.example.demo.model.ForumModels.UserAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends BaseRepository {

    private static final RowMapper<UserAccount> USER_ROW_MAPPER = UserRepository::mapUser;

    public UserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public Optional<UserAccount> findById(Long id) {
        return queryOptional("select * from `users` where id = :id", Map.of("id", id), USER_ROW_MAPPER);
    }

    public Optional<UserAccount> lockById(Long id) {
        return queryOptional("select * from `users` where id = :id for update", Map.of("id", id), USER_ROW_MAPPER);
    }

    public Optional<UserAccount> findByUsername(String username) {
        return queryOptional("select * from `users` where username = :username", Map.of("username", username), USER_ROW_MAPPER);
    }

    public List<UserAccount> findByIds(Collection<Long> ids) {
        if (!hasIds(ids)) {
            return Collections.emptyList();
        }
        return jdbcTemplate.query("select * from `users` where id in (:ids)", Map.of("ids", ids), USER_ROW_MAPPER);
    }

    public long createUser(String username, String nickname, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("nickname", nickname)
            .addValue("passwordHash", passwordHash)
            .addValue("role", UserRole.USER.value())
            .addValue("status", UserStatus.ACTIVE.value());
        return insertAndReturnId("""
            insert into `users`
            (username, nickname, password_hash, role, status, coin_balance, level, total_exp, current_level_exp)
            values
            (:username, :nickname, :passwordHash, :role, :status, 0, 1, 0, 0)
            """, params);
    }

    public void updateLastLoginAt(Long userId, LocalDateTime time) {
        jdbcTemplate.update("""
            update `users`
            set last_login_at = :time
            where id = :userId
            """, Map.of("time", time, "userId", userId));
    }

    public void updateProfile(Long userId, String nickname, String avatarUrl, String bio) {
        jdbcTemplate.update("""
            update `users`
            set nickname = :nickname,
                avatar_url = :avatarUrl,
                bio = :bio
            where id = :userId
            """, new MapSqlParameterSource()
            .addValue("nickname", nickname)
            .addValue("avatarUrl", avatarUrl)
            .addValue("bio", bio)
            .addValue("userId", userId));
    }

    public void updatePasswordAndIncrementTokenVersion(Long userId, String passwordHash) {
        jdbcTemplate.update("""
            update `users`
            set password_hash = :passwordHash,
                token_version = token_version + 1
            where id = :userId
            """, Map.of(
            "passwordHash", passwordHash,
            "userId", userId
        ));
    }

    public void banUser(Long userId, LocalDateTime bannedUntilAt, String reason) {
        jdbcTemplate.update("""
            update `users`
            set status = :status,
                banned_until_at = :bannedUntilAt,
                ban_reason = :reason
            where id = :userId
            """, new MapSqlParameterSource()
            .addValue("status", UserStatus.BANNED.value())
            .addValue("bannedUntilAt", bannedUntilAt)
            .addValue("reason", reason)
            .addValue("userId", userId));
    }

    public void unbanUser(Long userId) {
        jdbcTemplate.update("""
            update `users`
            set status = :status,
                banned_until_at = null,
                ban_reason = null
            where id = :userId
            """, Map.of("status", UserStatus.ACTIVE.value(), "userId", userId));
    }

    public void muteUser(Long userId, LocalDateTime mutedUntilAt, String reason) {
        jdbcTemplate.update("""
            update `users`
            set muted_until_at = :mutedUntilAt,
                mute_reason = :reason
            where id = :userId
            """, new MapSqlParameterSource()
            .addValue("mutedUntilAt", mutedUntilAt)
            .addValue("reason", reason)
            .addValue("userId", userId));
    }

    public void unmuteUser(Long userId) {
        jdbcTemplate.update("""
            update `users`
            set muted_until_at = null,
                mute_reason = null
            where id = :userId
            """, Map.of("userId", userId));
    }

    public void updateExp(Long userId, int totalExp, int currentLevelExp, int level) {
        jdbcTemplate.update("""
            update `users`
            set total_exp = :totalExp,
                current_level_exp = :currentLevelExp,
                `level` = :level
            where id = :userId
            """, Map.of(
            "totalExp", totalExp,
            "currentLevelExp", currentLevelExp,
            "level", level,
            "userId", userId
        ));
    }

    public void updateCoinBalance(Long userId, long newBalance) {
        jdbcTemplate.update("""
            update `users`
            set coin_balance = :newBalance
            where id = :userId
            """, Map.of("newBalance", newBalance, "userId", userId));
    }

    public long countAllUsers() {
        Long value = jdbcTemplate.queryForObject("select count(*) from `users`", Collections.emptyMap(), Long.class);
        return value == null ? 0L : value;
    }

    public com.example.demo.common.PageResult<UserAccount> pageAdminUsers(String keyword, String status, String role,
                                                                          long page, long pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String whereClause = buildAdminUserWhereClause(keyword, status, role, params);

        Long totalValue = jdbcTemplate.queryForObject(
            "select count(*) from `users`" + whereClause,
            params,
            Long.class
        );
        long total = totalValue == null ? 0L : totalValue;

        params.addValue("limit", pageSize);
        params.addValue("offset", offset(page, pageSize));
        List<UserAccount> list = jdbcTemplate.query("""
            select *
            from `users`
            """ + whereClause + """
            order by created_at desc, id desc
            limit :limit offset :offset
            """, params, USER_ROW_MAPPER);

        return new com.example.demo.common.PageResult<>(total, page, pageSize, list);
    }

    /**
     * 公开用户搜索：仅活跃用户；用户名/昵称模糊匹配；关键词为纯数字时额外匹配用户 ID。
     */
    public com.example.demo.common.PageResult<UserAccount> pageSearchUsers(String keyword, long page, long pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder("""
            where status = 'active'
              and (
                username like :kw
                or coalesce(nickname, '') like :kw
            """);
        params.addValue("kw", "%" + keyword + "%");
        Long numericId = parsePositiveLong(keyword);
        if (numericId != null) {
            where.append(" or id = :exactId");
            params.addValue("exactId", numericId);
        }
        where.append(")\n");

        Long totalValue = jdbcTemplate.queryForObject(
            "select count(*) from `users` " + where,
            params,
            Long.class
        );
        long total = totalValue == null ? 0L : totalValue;

        params.addValue("limit", pageSize);
        params.addValue("offset", offset(page, pageSize));
        List<UserAccount> list = jdbcTemplate.query("""
            select *
            from `users`
            """ + where + """
            order by created_at desc, id desc
            limit :limit offset :offset
            """, params, USER_ROW_MAPPER);

        return new com.example.demo.common.PageResult<>(total, page, pageSize, list);
    }

    private static Long parsePositiveLong(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<UserAccount> pageUsers(long page, long pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        return jdbcTemplate.query("""
            select * from `users`
            order by created_at desc
            limit :limit offset :offset
            """, params, USER_ROW_MAPPER);
    }

    private String buildAdminUserWhereClause(String keyword, String status, String role, MapSqlParameterSource params) {
        StringBuilder whereClause = new StringBuilder(" where 1 = 1");

        if (keyword != null) {
            whereClause.append("""
                 and (
                    username like :keyword
                    or coalesce(nickname, '') like :keyword
                 )
                """);
            params.addValue("keyword", "%" + keyword + "%");
        }

        if (status != null) {
            whereClause.append(" and status = :status");
            params.addValue("status", status);
        }

        if (role != null) {
            whereClause.append(" and role = :role");
            params.addValue("role", role);
        }

        return whereClause.append('\n').toString();
    }

    private static UserAccount mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("nickname"),
            rs.getString("avatar_url"),
            rs.getString("bio"),
            rs.getString("password_hash"),
            rs.getInt("token_version"),
            rs.getString("role"),
            rs.getString("status"),
            getDateTime(rs, "banned_until_at"),
            rs.getString("ban_reason"),
            getDateTime(rs, "muted_until_at"),
            rs.getString("mute_reason"),
            rs.getLong("coin_balance"),
            rs.getInt("level"),
            rs.getInt("total_exp"),
            rs.getInt("current_level_exp"),
            getDateTime(rs, "created_at"),
            getDateTime(rs, "last_login_at"),
            getDateTime(rs, "updated_at")
        );
    }
}
