package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.CommentRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository extends BaseRepository {

    private static final RowMapper<CommentRecord> COMMENT_ROW_MAPPER = CommentRepository::mapComment;

    public CommentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public long createComment(Long postId, Long userId, Long parentCommentId, Long replyToUserId, String contentText) {
        return insertAndReturnId("""
            insert into `post_comments` (post_id, user_id, parent_comment_id, reply_to_user_id, content_text)
            values (:postId, :userId, :parentCommentId, :replyToUserId, :contentText)
            """, new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("userId", userId)
            .addValue("parentCommentId", parentCommentId)
            .addValue("replyToUserId", replyToUserId)
            .addValue("contentText", contentText));
    }

    public void insertCommentImages(Long commentId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update("""
                insert into `comment_images` (comment_id, image_url, sort_order)
                values (:commentId, :imageUrl, :sortOrder)
                """, Map.of(
                "commentId", commentId,
                "imageUrl", imageUrls.get(i),
                "sortOrder", i
            ));
        }
    }

    public Optional<CommentRecord> findById(Long commentId) {
        return queryOptional("select * from `post_comments` where id = :commentId", Map.of("commentId", commentId), COMMENT_ROW_MAPPER);
    }

    public Optional<CommentRecord> lockById(Long commentId) {
        return queryOptional("select * from `post_comments` where id = :commentId for update", Map.of("commentId", commentId), COMMENT_ROW_MAPPER);
    }

    public List<CommentViewRow> listByPostId(Long postId, Long currentUserId) {
        String sql = """
            select c.*,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
                   ru.username as reply_username,
                   ru.nickname as reply_nickname,
                   ru.avatar_url as reply_avatar_url,
                   ru.bio as reply_bio,
                   ru.role as reply_role,
                   ru.status as reply_status,
                   ru.`level` as reply_level,
            """ + interactionSelect(currentUserId) + """
            from `post_comments` c
            join `users` u on u.id = c.user_id
            left join `users` ru on ru.id = c.reply_to_user_id
            where c.post_id = :postId
            order by c.created_at asc, c.id asc
            """;
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        if (currentUserId != null) {
            params.put("currentUserId", currentUserId);
        }
        return jdbcTemplate.query(sql, params, COMMENT_VIEW_ROW_MAPPER);
    }

    public PageResult<CommentViewRow> pageFavoriteComments(Long userId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*)
            from `comment_favorites` cf
            join `post_comments` c on c.id = cf.comment_id
            join `posts` p on p.id = c.post_id
            where cf.user_id = :userId
              and p.status = 'normal'
            """, Map.of("userId", userId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<CommentViewRow> list = jdbcTemplate.query("""
            select c.*,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
                   ru.username as reply_username,
                   ru.nickname as reply_nickname,
                   ru.avatar_url as reply_avatar_url,
                   ru.bio as reply_bio,
                   ru.role as reply_role,
                   ru.status as reply_status,
                   ru.`level` as reply_level,
                   exists(select 1 from `comment_likes` cl where cl.comment_id = c.id and cl.user_id = :userId) as liked,
                   true as favorited
            from `comment_favorites` cf
            join `post_comments` c on c.id = cf.comment_id
            join `posts` p on p.id = c.post_id
            join `users` u on u.id = c.user_id
            left join `users` ru on ru.id = c.reply_to_user_id
            where cf.user_id = :userId
              and p.status = 'normal'
            order by cf.created_at desc, cf.id desc
            limit :limit offset :offset
            """, params, COMMENT_VIEW_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    public Map<Long, List<String>> listImagesByCommentIds(Collection<Long> commentIds) {
        if (!hasIds(commentIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select comment_id, image_url
            from `comment_images`
            where comment_id in (:commentIds)
            order by comment_id asc, sort_order asc, id asc
            """, Map.of("commentIds", commentIds));
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long commentId = ((Number) row.get("comment_id")).longValue();
            map.computeIfAbsent(commentId, key -> new java.util.ArrayList<>()).add((String) row.get("image_url"));
        }
        return map;
    }

    public boolean existsCommentLike(Long commentId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `comment_likes`
            where comment_id = :commentId and user_id = :userId
            """, Map.of("commentId", commentId, "userId", userId), Integer.class);
        return count != null && count > 0;
    }

    public void insertCommentLike(Long commentId, Long userId) {
        jdbcTemplate.update("""
            insert into `comment_likes` (comment_id, user_id)
            values (:commentId, :userId)
            """, Map.of("commentId", commentId, "userId", userId));
    }

    public void deleteCommentLike(Long commentId, Long userId) {
        jdbcTemplate.update("""
            delete from `comment_likes`
            where comment_id = :commentId and user_id = :userId
            """, Map.of("commentId", commentId, "userId", userId));
    }

    public void incrementCommentLikeCount(Long commentId, int delta) {
        jdbcTemplate.update("""
            update `post_comments`
            set like_count = greatest(like_count + :delta, 0)
            where id = :commentId
            """, Map.of("commentId", commentId, "delta", delta));
    }

    public boolean existsCommentFavorite(Long commentId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `comment_favorites`
            where comment_id = :commentId and user_id = :userId
            """, Map.of("commentId", commentId, "userId", userId), Integer.class);
        return count != null && count > 0;
    }

    public void insertCommentFavorite(Long commentId, Long userId) {
        jdbcTemplate.update("""
            insert into `comment_favorites` (comment_id, user_id)
            values (:commentId, :userId)
            """, Map.of("commentId", commentId, "userId", userId));
    }

    public void deleteCommentFavorite(Long commentId, Long userId) {
        jdbcTemplate.update("""
            delete from `comment_favorites`
            where comment_id = :commentId and user_id = :userId
            """, Map.of("commentId", commentId, "userId", userId));
    }

    public void incrementRewardCoinCount(Long commentId, long delta) {
        jdbcTemplate.update("""
            update `post_comments`
            set reward_coin_count = reward_coin_count + :delta
            where id = :commentId
            """, Map.of("commentId", commentId, "delta", delta));
    }

    private static final RowMapper<CommentViewRow> COMMENT_VIEW_ROW_MAPPER = (rs, rowNum) -> new CommentViewRow(
        rs.getLong("id"),
        rs.getLong("post_id"),
        rs.getLong("user_id"),
        getLongObject(rs, "parent_comment_id"),
        getLongObject(rs, "reply_to_user_id"),
        rs.getString("content_text"),
        rs.getInt("like_count"),
        rs.getLong("reward_coin_count"),
        getDateTime(rs, "created_at"),
        getDateTime(rs, "updated_at"),
        rs.getString("author_username"),
        rs.getString("author_nickname"),
        rs.getString("author_avatar_url"),
        rs.getString("author_bio"),
        rs.getString("author_role"),
        rs.getString("author_status"),
        rs.getInt("author_level"),
        rs.getString("reply_username"),
        rs.getString("reply_nickname"),
        rs.getString("reply_avatar_url"),
        rs.getString("reply_bio"),
        rs.getString("reply_role"),
        rs.getString("reply_status"),
        getIntegerObject(rs, "reply_level"),
        rs.getBoolean("liked"),
        rs.getBoolean("favorited")
    );

    private static String interactionSelect(Long currentUserId) {
        if (currentUserId == null) {
            return """
                       false as liked,
                       false as favorited
                """;
        }
        return """
                   exists(select 1 from `comment_likes` cl where cl.comment_id = c.id and cl.user_id = :currentUserId) as liked,
                   exists(select 1 from `comment_favorites` cf where cf.comment_id = c.id and cf.user_id = :currentUserId) as favorited
                """;
    }

    private static CommentRecord mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new CommentRecord(
            rs.getLong("id"),
            rs.getLong("post_id"),
            rs.getLong("user_id"),
            getLongObject(rs, "parent_comment_id"),
            getLongObject(rs, "reply_to_user_id"),
            rs.getString("content_text"),
            rs.getInt("like_count"),
            rs.getLong("reward_coin_count"),
            getDateTime(rs, "created_at"),
            getDateTime(rs, "updated_at")
        );
    }

    public record CommentViewRow(
        Long id,
        Long postId,
        Long userId,
        Long parentCommentId,
        Long replyToUserId,
        String contentText,
        Integer likeCount,
        Long rewardCoinCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String authorUsername,
        String authorNickname,
        String authorAvatarUrl,
        String authorBio,
        String authorRole,
        String authorStatus,
        Integer authorLevel,
        String replyUsername,
        String replyNickname,
        String replyAvatarUrl,
        String replyBio,
        String replyRole,
        String replyStatus,
        Integer replyLevel,
        boolean liked,
        boolean favorited
    ) {
    }
}
