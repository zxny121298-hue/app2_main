package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.NotificationGroupRecord;
import com.example.demo.model.ForumModels.NotificationItemRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository extends BaseRepository {

    private static final RowMapper<NotificationGroupRecord> GROUP_ROW_MAPPER = NotificationRepository::mapGroup;
    private static final RowMapper<NotificationItemRecord> ITEM_ROW_MAPPER = NotificationRepository::mapItem;

    public NotificationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public Optional<NotificationGroupRecord> lockGroup(Long recipientUserId, String eventType, String targetType, Long targetId) {
        return queryOptional("""
            select *
            from `notification_groups`
            where recipient_user_id = :recipientUserId
              and event_type = :eventType
              and target_type = :targetType
              and target_id = :targetId
            for update
            """, Map.of(
            "recipientUserId", recipientUserId,
            "eventType", eventType,
            "targetType", targetType,
            "targetId", targetId
        ), GROUP_ROW_MAPPER);
    }

    public Optional<NotificationGroupRecord> lockGroupById(Long groupId, Long recipientUserId) {
        return queryOptional("""
            select *
            from `notification_groups`
            where id = :groupId and recipient_user_id = :recipientUserId
            for update
            """, Map.of("groupId", groupId, "recipientUserId", recipientUserId), GROUP_ROW_MAPPER);
    }

    public long createGroup(Long recipientUserId, String eventType, String targetType, Long targetId,
                            Long latestActorUserId, LocalDateTime latestAt) {
        return insertAndReturnId("""
            insert into `notification_groups`
            (recipient_user_id, event_type, target_type, target_id, latest_actor_user_id, total_count, unread_count, latest_at)
            values
            (:recipientUserId, :eventType, :targetType, :targetId, :latestActorUserId, 1, 1, :latestAt)
            """, new MapSqlParameterSource()
            .addValue("recipientUserId", recipientUserId)
            .addValue("eventType", eventType)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("latestActorUserId", latestActorUserId)
            .addValue("latestAt", latestAt));
    }

    public void incrementGroup(Long groupId, Long latestActorUserId, LocalDateTime latestAt) {
        jdbcTemplate.update("""
            update `notification_groups`
            set total_count = total_count + 1,
                unread_count = unread_count + 1,
                latest_actor_user_id = :latestActorUserId,
                latest_at = :latestAt
            where id = :groupId
            """, Map.of(
            "groupId", groupId,
            "latestActorUserId", latestActorUserId,
            "latestAt", latestAt
        ));
    }

    public long createItem(Long groupId, Long recipientUserId, Long actorUserId, String eventType, String targetType,
                           Long targetId, Long conversationId, Long messageId, Long postId, Long commentId, Long rewardId) {
        return insertAndReturnId("""
            insert into `notification_items`
            (group_id, recipient_user_id, actor_user_id, event_type, target_type, target_id, conversation_id, message_id, post_id, comment_id, reward_id)
            values
            (:groupId, :recipientUserId, :actorUserId, :eventType, :targetType, :targetId, :conversationId, :messageId, :postId, :commentId, :rewardId)
            """, new MapSqlParameterSource()
            .addValue("groupId", groupId)
            .addValue("recipientUserId", recipientUserId)
            .addValue("actorUserId", actorUserId)
            .addValue("eventType", eventType)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("conversationId", conversationId)
            .addValue("messageId", messageId)
            .addValue("postId", postId)
            .addValue("commentId", commentId)
            .addValue("rewardId", rewardId));
    }

    public Optional<NotificationItemRecord> lockItem(Long itemId, Long recipientUserId) {
        return queryOptional("""
            select *
            from `notification_items`
            where id = :itemId and recipient_user_id = :recipientUserId
            for update
            """, Map.of("itemId", itemId, "recipientUserId", recipientUserId), ITEM_ROW_MAPPER);
    }

    public void markItemRead(Long itemId, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `notification_items`
            set is_read = 1,
                read_at = :readAt
            where id = :itemId
            """, Map.of("itemId", itemId, "readAt", readAt));
    }

    public void decrementGroupUnread(Long groupId, int delta, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `notification_groups`
            set unread_count = greatest(unread_count - :delta, 0),
                last_read_at = :readAt
            where id = :groupId
            """, Map.of("groupId", groupId, "delta", delta, "readAt", readAt));
    }

    public int markGroupItemsRead(Long groupId, Long recipientUserId, LocalDateTime readAt) {
        return jdbcTemplate.update("""
            update `notification_items`
            set is_read = 1,
                read_at = :readAt
            where group_id = :groupId
              and recipient_user_id = :recipientUserId
              and is_read = 0
            """, Map.of(
            "groupId", groupId,
            "recipientUserId", recipientUserId,
            "readAt", readAt
        ));
    }

    public void markGroupRead(Long groupId, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `notification_groups`
            set unread_count = 0,
                last_read_at = :readAt
            where id = :groupId
            """, Map.of("groupId", groupId, "readAt", readAt));
    }

    public int markAllItemsRead(Long recipientUserId, LocalDateTime readAt) {
        return jdbcTemplate.update("""
            update `notification_items`
            set is_read = 1,
                read_at = :readAt
            where recipient_user_id = :recipientUserId
              and is_read = 0
            """, Map.of("recipientUserId", recipientUserId, "readAt", readAt));
    }

    public void markAllGroupsRead(Long recipientUserId, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `notification_groups`
            set unread_count = 0,
                last_read_at = :readAt
            where recipient_user_id = :recipientUserId
            """, Map.of("recipientUserId", recipientUserId, "readAt", readAt));
    }

    public int sumUnreadCount(Long recipientUserId) {
        Integer value = jdbcTemplate.queryForObject("""
            select coalesce(sum(unread_count), 0)
            from `notification_groups`
            where recipient_user_id = :recipientUserId
            """, Map.of("recipientUserId", recipientUserId), Integer.class);
        return value == null ? 0 : value;
    }

    public PageResult<NotificationGroupRow> pageGroups(Long recipientUserId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*)
            from `notification_groups`
            where recipient_user_id = :recipientUserId
            """, Map.of("recipientUserId", recipientUserId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("recipientUserId", recipientUserId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<NotificationGroupRow> list = jdbcTemplate.query("""
            select ng.*,
                   u.username as actor_username,
                   u.nickname as actor_nickname,
                   u.avatar_url as actor_avatar_url,
                   u.bio as actor_bio,
                   u.role as actor_role,
                   u.status as actor_status,
                   u.`level` as actor_level
            from `notification_groups` ng
            left join `users` u on u.id = ng.latest_actor_user_id
            where ng.recipient_user_id = :recipientUserId
            order by ng.latest_at desc, ng.id desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new NotificationGroupRow(
            rs.getLong("id"),
            rs.getString("event_type"),
            rs.getString("target_type"),
            rs.getLong("target_id"),
            rs.getInt("total_count"),
            rs.getInt("unread_count"),
            getLongObject(rs, "latest_actor_user_id"),
            rs.getString("actor_username"),
            rs.getString("actor_nickname"),
            rs.getString("actor_avatar_url"),
            rs.getString("actor_bio"),
            rs.getString("actor_role"),
            rs.getString("actor_status"),
            getIntegerObject(rs, "actor_level"),
            getDateTime(rs, "latest_at"),
            getDateTime(rs, "last_read_at")
        ));
        return new PageResult<>(total, page, pageSize, list);
    }

    public PageResult<NotificationItemRow> pageItems(Long groupId, Long recipientUserId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*)
            from `notification_items`
            where group_id = :groupId and recipient_user_id = :recipientUserId
            """, Map.of("groupId", groupId, "recipientUserId", recipientUserId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("recipientUserId", recipientUserId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<NotificationItemRow> list = jdbcTemplate.query("""
            select ni.*,
                   u.username as actor_username,
                   u.nickname as actor_nickname,
                   u.avatar_url as actor_avatar_url,
                   u.bio as actor_bio,
                   u.role as actor_role,
                   u.status as actor_status,
                   u.`level` as actor_level
            from `notification_items` ni
            join `users` u on u.id = ni.actor_user_id
            where ni.group_id = :groupId and ni.recipient_user_id = :recipientUserId
            order by ni.created_at desc, ni.id desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new NotificationItemRow(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getString("event_type"),
            rs.getString("target_type"),
            rs.getLong("target_id"),
            rs.getLong("actor_user_id"),
            rs.getString("actor_username"),
            rs.getString("actor_nickname"),
            rs.getString("actor_avatar_url"),
            rs.getString("actor_bio"),
            rs.getString("actor_role"),
            rs.getString("actor_status"),
            rs.getInt("actor_level"),
            getLongObject(rs, "conversation_id"),
            getLongObject(rs, "message_id"),
            getLongObject(rs, "post_id"),
            getLongObject(rs, "comment_id"),
            getLongObject(rs, "reward_id"),
            rs.getBoolean("is_read"),
            getDateTime(rs, "read_at"),
            getDateTime(rs, "created_at")
        ));
        return new PageResult<>(total, page, pageSize, list);
    }

    private static NotificationGroupRecord mapGroup(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationGroupRecord(
            rs.getLong("id"),
            rs.getLong("recipient_user_id"),
            rs.getString("event_type"),
            rs.getString("target_type"),
            rs.getLong("target_id"),
            getLongObject(rs, "latest_actor_user_id"),
            rs.getInt("total_count"),
            rs.getInt("unread_count"),
            getDateTime(rs, "last_read_at"),
            getDateTime(rs, "latest_at"),
            getDateTime(rs, "created_at"),
            getDateTime(rs, "updated_at")
        );
    }

    private static NotificationItemRecord mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationItemRecord(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getLong("recipient_user_id"),
            rs.getLong("actor_user_id"),
            rs.getString("event_type"),
            rs.getString("target_type"),
            rs.getLong("target_id"),
            getLongObject(rs, "conversation_id"),
            getLongObject(rs, "message_id"),
            getLongObject(rs, "post_id"),
            getLongObject(rs, "comment_id"),
            getLongObject(rs, "reward_id"),
            rs.getBoolean("is_read"),
            getDateTime(rs, "read_at"),
            getDateTime(rs, "created_at")
        );
    }

    public record NotificationGroupRow(
        Long id,
        String eventType,
        String targetType,
        Long targetId,
        Integer totalCount,
        Integer unreadCount,
        Long actorUserId,
        String actorUsername,
        String actorNickname,
        String actorAvatarUrl,
        String actorBio,
        String actorRole,
        String actorStatus,
        Integer actorLevel,
        LocalDateTime latestAt,
        LocalDateTime lastReadAt
    ) {
    }

    public record NotificationItemRow(
        Long id,
        Long groupId,
        String eventType,
        String targetType,
        Long targetId,
        Long actorUserId,
        String actorUsername,
        String actorNickname,
        String actorAvatarUrl,
        String actorBio,
        String actorRole,
        String actorStatus,
        Integer actorLevel,
        Long conversationId,
        Long messageId,
        Long postId,
        Long commentId,
        Long rewardId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
    ) {
    }
}
