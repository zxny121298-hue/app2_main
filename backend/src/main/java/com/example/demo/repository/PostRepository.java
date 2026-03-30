package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.PostRecord;
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
public class PostRepository extends BaseRepository {

    private static final RowMapper<PostRecord> POST_ROW_MAPPER = PostRepository::mapPost;

    public PostRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public long createPost(Long boardId, Long userId, String title, String contentText) {
        return insertAndReturnId("""
            insert into `posts` (board_id, user_id, title, content_text)
            values (:boardId, :userId, :title, :contentText)
            """, new MapSqlParameterSource()
            .addValue("boardId", boardId)
            .addValue("userId", userId)
            .addValue("title", title)
            .addValue("contentText", contentText));
    }

    public void insertPostImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update("""
                insert into `post_images` (post_id, image_url, sort_order)
                values (:postId, :imageUrl, :sortOrder)
                """, Map.of(
                "postId", postId,
                "imageUrl", imageUrls.get(i),
                "sortOrder", i
            ));
        }
    }

    public Optional<PostRecord> findById(Long postId) {
        return queryOptional("select * from `posts` where id = :postId", Map.of("postId", postId), POST_ROW_MAPPER);
    }

    public Optional<PostRecord> lockById(Long postId) {
        return queryOptional("select * from `posts` where id = :postId for update", Map.of("postId", postId), POST_ROW_MAPPER);
    }

    public Optional<PostViewRow> findViewById(Long postId, Long currentUserId) {
        String sql = """
            select p.*,
                   b.name as board_name,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
            """ + interactionSelect(currentUserId) + """
            from `posts` p
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            where p.id = :postId
            """;
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        if (currentUserId != null) {
            params.put("currentUserId", currentUserId);
        }
        return queryOptional(sql, params, VIEW_ROW_MAPPER);
    }

    public PageResult<PostViewRow> pagePosts(Long currentUserId, Long boardId, Long authorUserId, long page, long pageSize) {
        StringBuilder countSql = new StringBuilder("""
            select count(*) from `posts` p
            where p.status = 'normal'
            """);
        Map<String, Object> params = new HashMap<>();
        if (boardId != null) {
            countSql.append(" and p.board_id = :boardId");
            params.put("boardId", boardId);
        }
        if (authorUserId != null) {
            countSql.append(" and p.user_id = :authorUserId");
            params.put("authorUserId", authorUserId);
        }
        long total = Optional.ofNullable(jdbcTemplate.queryForObject(countSql.toString(), params, Long.class)).orElse(0L);

        StringBuilder sql = new StringBuilder("""
            select p.*,
                   b.name as board_name,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
            """).append(interactionSelect(currentUserId)).append("""
            from `posts` p
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            where p.status = 'normal'
            """);
        if (boardId != null) {
            sql.append(" and p.board_id = :boardId ");
        }
        if (authorUserId != null) {
            sql.append(" and p.user_id = :authorUserId ");
        }
        sql.append("""
            order by p.is_pinned desc, p.is_featured desc, p.created_at desc, p.id desc
            limit :limit offset :offset
            """);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        if (currentUserId != null) {
            params.put("currentUserId", currentUserId);
        }
        List<PostViewRow> list = jdbcTemplate.query(sql.toString(), params, VIEW_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    /**
     * 公开搜索：仅匹配帖子标题与正文，不含作者用户名（与后台管理 keyword 区分）。
     */
    public PageResult<PostViewRow> pageSearchPosts(Long currentUserId, String keyword, long page, long pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("kw", "%" + keyword + "%");
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*) from `posts` p
            where p.status = 'normal'
              and (p.title like :kw or coalesce(p.content_text, '') like :kw)
            """, params, Long.class)).orElse(0L);

        StringBuilder sql = new StringBuilder("""
            select p.*,
                   b.name as board_name,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
            """).append(interactionSelect(currentUserId)).append("""
            from `posts` p
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            where p.status = 'normal'
              and (p.title like :kw or coalesce(p.content_text, '') like :kw)
            order by p.is_pinned desc, p.is_featured desc, p.created_at desc, p.id desc
            limit :limit offset :offset
            """);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        if (currentUserId != null) {
            params.put("currentUserId", currentUserId);
        }
        List<PostViewRow> list = jdbcTemplate.query(sql.toString(), params, VIEW_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    public PageResult<PostViewRow> pageAdminPosts(Long boardId, String keyword, Boolean pinned, Boolean featured,
                                                  long page, long pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String whereClause = buildAdminPostWhereClause(boardId, keyword, pinned, featured, params);

        Long totalValue = jdbcTemplate.queryForObject("""
            select count(*)
            from `posts` p
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            """ + whereClause, params, Long.class);
        long total = totalValue == null ? 0L : totalValue;

        params.addValue("limit", pageSize);
        params.addValue("offset", offset(page, pageSize));
        List<PostViewRow> list = jdbcTemplate.query("""
            select p.*,
                   b.name as board_name,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
            """ + interactionSelect(null) + """
            from `posts` p
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            """ + whereClause + """
            order by p.is_pinned desc, p.is_featured desc, p.created_at desc, p.id desc
            limit :limit offset :offset
            """, params, VIEW_ROW_MAPPER);

        return new PageResult<>(total, page, pageSize, list);
    }

    public PageResult<PostViewRow> pageFavoritePosts(Long currentUserId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*)
            from `post_favorites` pf
            join `posts` p on p.id = pf.post_id
            where pf.user_id = :currentUserId and p.status = 'normal'
            """, Map.of("currentUserId", currentUserId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("currentUserId", currentUserId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<PostViewRow> list = jdbcTemplate.query("""
            select p.*,
                   b.name as board_name,
                   u.username as author_username,
                   u.nickname as author_nickname,
                   u.avatar_url as author_avatar_url,
                   u.bio as author_bio,
                   u.role as author_role,
                   u.status as author_status,
                   u.`level` as author_level,
                   exists(select 1 from `post_likes` pl where pl.post_id = p.id and pl.user_id = :currentUserId) as liked,
                   true as favorited
            from `post_favorites` pf
            join `posts` p on p.id = pf.post_id
            join `boards` b on b.id = p.board_id
            join `users` u on u.id = p.user_id
            where pf.user_id = :currentUserId and p.status = 'normal'
            order by pf.created_at desc, pf.id desc
            limit :limit offset :offset
            """, params, VIEW_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    private String buildAdminPostWhereClause(Long boardId, String keyword, Boolean pinned, Boolean featured,
                                             MapSqlParameterSource params) {
        StringBuilder whereClause = new StringBuilder("""
            where p.status = 'normal'
            """);

        if (boardId != null) {
            whereClause.append(" and p.board_id = :boardId");
            params.addValue("boardId", boardId);
        }

        if (keyword != null) {
            whereClause.append("""
                 and (
                    p.title like :keyword
                    or coalesce(p.content_text, '') like :keyword
                    or u.username like :keyword
                    or coalesce(u.nickname, '') like :keyword
                 )
                """);
            params.addValue("keyword", "%" + keyword + "%");
        }

        if (pinned != null) {
            whereClause.append(" and p.is_pinned = :pinned");
            params.addValue("pinned", pinned);
        }

        if (featured != null) {
            whereClause.append(" and p.is_featured = :featured");
            params.addValue("featured", featured);
        }

        return whereClause.append('\n').toString();
    }

    public Map<Long, List<String>> listImagesByPostIds(Collection<Long> postIds) {
        if (!hasIds(postIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select post_id, image_url
            from `post_images`
            where post_id in (:postIds)
            order by post_id asc, sort_order asc, id asc
            """, Map.of("postIds", postIds));
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long postId = ((Number) row.get("post_id")).longValue();
            map.computeIfAbsent(postId, key -> new java.util.ArrayList<>()).add((String) row.get("image_url"));
        }
        return map;
    }

    public boolean existsPostLike(Long postId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `post_likes`
            where post_id = :postId and user_id = :userId
            """, Map.of("postId", postId, "userId", userId), Integer.class);
        return count != null && count > 0;
    }

    public void insertPostLike(Long postId, Long userId) {
        jdbcTemplate.update("""
            insert into `post_likes` (post_id, user_id)
            values (:postId, :userId)
            """, Map.of("postId", postId, "userId", userId));
    }

    public void deletePostLike(Long postId, Long userId) {
        jdbcTemplate.update("""
            delete from `post_likes`
            where post_id = :postId and user_id = :userId
            """, Map.of("postId", postId, "userId", userId));
    }

    public void incrementPostLikeCount(Long postId, int delta) {
        jdbcTemplate.update("""
            update `posts`
            set like_count = greatest(like_count + :delta, 0)
            where id = :postId
            """, Map.of("delta", delta, "postId", postId));
    }

    public boolean existsPostFavorite(Long postId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `post_favorites`
            where post_id = :postId and user_id = :userId
            """, Map.of("postId", postId, "userId", userId), Integer.class);
        return count != null && count > 0;
    }

    public void insertPostFavorite(Long postId, Long userId) {
        jdbcTemplate.update("""
            insert into `post_favorites` (post_id, user_id)
            values (:postId, :userId)
            """, Map.of("postId", postId, "userId", userId));
    }

    public void deletePostFavorite(Long postId, Long userId) {
        jdbcTemplate.update("""
            delete from `post_favorites`
            where post_id = :postId and user_id = :userId
            """, Map.of("postId", postId, "userId", userId));
    }

    public void incrementCommentCount(Long postId, int delta) {
        jdbcTemplate.update("""
            update `posts`
            set comment_count = greatest(comment_count + :delta, 0)
            where id = :postId
            """, Map.of("delta", delta, "postId", postId));
    }

    public void incrementRewardCoinCount(Long postId, long delta) {
        jdbcTemplate.update("""
            update `posts`
            set reward_coin_count = reward_coin_count + :delta
            where id = :postId
            """, Map.of("delta", delta, "postId", postId));
    }

    public void softDelete(Long postId, Long deletedByUserId, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
            update `posts`
            set status = 'deleted',
                deleted_at = :deletedAt,
                deleted_by_user_id = :deletedByUserId
            where id = :postId
            """, Map.of(
            "postId", postId,
            "deletedAt", deletedAt,
            "deletedByUserId", deletedByUserId
        ));
    }

    public void updatePinState(Long postId, boolean pinned, Long operatorUserId, LocalDateTime operationTime) {
        jdbcTemplate.update("""
            update `posts`
            set is_pinned = :pinned,
                pinned_at = :pinnedAt,
                pinned_by_user_id = :operatorUserId
            where id = :postId
            """, new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("pinned", pinned)
            .addValue("pinnedAt", pinned ? operationTime : null)
            .addValue("operatorUserId", pinned ? operatorUserId : null));
    }

    public void updateFeaturedState(Long postId, boolean featured, Long operatorUserId, LocalDateTime operationTime) {
        jdbcTemplate.update("""
            update `posts`
            set is_featured = :featured,
                featured_at = :featuredAt,
                featured_by_user_id = :operatorUserId
            where id = :postId
            """, new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("featured", featured)
            .addValue("featuredAt", featured ? operationTime : null)
            .addValue("operatorUserId", featured ? operatorUserId : null));
    }

    private static final RowMapper<PostViewRow> VIEW_ROW_MAPPER = (rs, rowNum) -> new PostViewRow(
        rs.getLong("id"),
        rs.getLong("board_id"),
        rs.getString("board_name"),
        rs.getLong("user_id"),
        rs.getString("author_username"),
        rs.getString("author_nickname"),
        rs.getString("author_avatar_url"),
        rs.getString("author_bio"),
        rs.getString("author_role"),
        rs.getString("author_status"),
        rs.getInt("author_level"),
        rs.getString("title"),
        rs.getString("content_text"),
        rs.getInt("like_count"),
        rs.getInt("comment_count"),
        rs.getLong("reward_coin_count"),
        rs.getBoolean("is_pinned"),
        getDateTime(rs, "pinned_at"),
        getLongObject(rs, "pinned_by_user_id"),
        rs.getBoolean("is_featured"),
        getDateTime(rs, "featured_at"),
        getLongObject(rs, "featured_by_user_id"),
        rs.getString("status"),
        rs.getBoolean("liked"),
        rs.getBoolean("favorited"),
        getDateTime(rs, "created_at"),
        getDateTime(rs, "updated_at")
    );

    private static String interactionSelect(Long currentUserId) {
        if (currentUserId == null) {
            return """
                       false as liked,
                       false as favorited
                """;
        }
        return """
                   exists(select 1 from `post_likes` pl where pl.post_id = p.id and pl.user_id = :currentUserId) as liked,
                   exists(select 1 from `post_favorites` pf where pf.post_id = p.id and pf.user_id = :currentUserId) as favorited
                """;
    }

    private static PostRecord mapPost(ResultSet rs, int rowNum) throws SQLException {
        return new PostRecord(
            rs.getLong("id"),
            rs.getLong("board_id"),
            rs.getLong("user_id"),
            rs.getString("title"),
            rs.getString("content_text"),
            rs.getInt("like_count"),
            rs.getInt("comment_count"),
            rs.getLong("reward_coin_count"),
            rs.getBoolean("is_pinned"),
            getDateTime(rs, "pinned_at"),
            getLongObject(rs, "pinned_by_user_id"),
            rs.getBoolean("is_featured"),
            getDateTime(rs, "featured_at"),
            getLongObject(rs, "featured_by_user_id"),
            rs.getString("status"),
            getDateTime(rs, "deleted_at"),
            getLongObject(rs, "deleted_by_user_id"),
            getDateTime(rs, "created_at"),
            getDateTime(rs, "updated_at")
        );
    }

    public record PostViewRow(
        Long id,
        Long boardId,
        String boardName,
        Long authorUserId,
        String authorUsername,
        String authorNickname,
        String authorAvatarUrl,
        String authorBio,
        String authorRole,
        String authorStatus,
        Integer authorLevel,
        String title,
        String contentText,
        Integer likeCount,
        Integer commentCount,
        Long rewardCoinCount,
        boolean pinned,
        LocalDateTime pinnedAt,
        Long pinnedByUserId,
        boolean featured,
        LocalDateTime featuredAt,
        Long featuredByUserId,
        String status,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
