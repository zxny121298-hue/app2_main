package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SocialRepository extends BaseRepository {

    public SocialRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public boolean existsFollow(Long followerUserId, Long followeeUserId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `user_follows`
            where follower_user_id = :followerUserId
              and followee_user_id = :followeeUserId
            """, Map.of(
            "followerUserId", followerUserId,
            "followeeUserId", followeeUserId
        ), Integer.class);
        return count != null && count > 0;
    }

    public long createFollow(Long followerUserId, Long followeeUserId) {
        return insertAndReturnId("""
            insert into `user_follows` (follower_user_id, followee_user_id)
            values (:followerUserId, :followeeUserId)
            """, new MapSqlParameterSource()
            .addValue("followerUserId", followerUserId)
            .addValue("followeeUserId", followeeUserId));
    }

    public void deleteFollow(Long followerUserId, Long followeeUserId) {
        jdbcTemplate.update("""
            delete from `user_follows`
            where follower_user_id = :followerUserId
              and followee_user_id = :followeeUserId
            """, Map.of(
            "followerUserId", followerUserId,
            "followeeUserId", followeeUserId
        ));
    }

    public long countFollowees(Long userId) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*) from `user_follows`
            where follower_user_id = :userId
            """, Map.of("userId", userId), Long.class);
        return total == null ? 0L : total;
    }

    public long countFollowers(Long userId) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*) from `user_follows`
            where followee_user_id = :userId
            """, Map.of("userId", userId), Long.class);
        return total == null ? 0L : total;
    }

    public List<FollowRow> pageFollowees(Long userId, long page, long pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        return jdbcTemplate.query("""
            select u.id,
                   u.username,
                   u.nickname,
                   u.avatar_url,
                   u.bio,
                   u.role,
                   u.status,
                   u.`level`,
                   uf.created_at as followed_at
            from `user_follows` uf
            join `users` u on u.id = uf.followee_user_id
            where uf.follower_user_id = :userId
            order by uf.created_at desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new FollowRow(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("nickname"),
            rs.getString("avatar_url"),
            rs.getString("bio"),
            rs.getString("role"),
            rs.getString("status"),
            rs.getInt("level"),
            getDateTime(rs, "followed_at")
        ));
    }

    public List<FollowRow> pageFollowers(Long userId, long page, long pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        return jdbcTemplate.query("""
            select u.id,
                   u.username,
                   u.nickname,
                   u.avatar_url,
                   u.bio,
                   u.role,
                   u.status,
                   u.`level`,
                   uf.created_at as followed_at
            from `user_follows` uf
            join `users` u on u.id = uf.follower_user_id
            where uf.followee_user_id = :userId
            order by uf.created_at desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new FollowRow(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("nickname"),
            rs.getString("avatar_url"),
            rs.getString("bio"),
            rs.getString("role"),
            rs.getString("status"),
            rs.getInt("level"),
            getDateTime(rs, "followed_at")
        ));
    }

    public Set<Long> findFolloweeIdsIn(Long followerUserId, Collection<Long> candidateIds) {
        if (!hasIds(candidateIds)) {
            return Collections.emptySet();
        }
        List<Long> rows = jdbcTemplate.query("""
            select followee_user_id
            from `user_follows`
            where follower_user_id = :followerUserId
              and followee_user_id in (:candidateIds)
            """, Map.of(
            "followerUserId", followerUserId,
            "candidateIds", candidateIds
        ), (rs, rowNum) -> rs.getLong("followee_user_id"));
        return new LinkedHashSet<>(rows);
    }

    public record FollowRow(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        String status,
        Integer level,
        LocalDateTime followedAt
    ) {
    }
}
