package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.ConversationMemberRecord;
import com.example.demo.model.ForumModels.ConversationRecord;
import com.example.demo.model.ForumModels.MessageRecord;
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
public class ConversationRepository extends BaseRepository {

    private static final RowMapper<ConversationRecord> CONVERSATION_ROW_MAPPER = ConversationRepository::mapConversation;
    private static final RowMapper<ConversationMemberRecord> MEMBER_ROW_MAPPER = ConversationRepository::mapMember;
    private static final RowMapper<MessageRecord> MESSAGE_ROW_MAPPER = ConversationRepository::mapMessage;

    public ConversationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public Optional<ConversationRecord> findById(Long conversationId) {
        return queryOptional("""
            select * from `conversations` where id = :conversationId
            """, Map.of("conversationId", conversationId), CONVERSATION_ROW_MAPPER);
    }

    public Optional<ConversationRecord> lockById(Long conversationId) {
        return queryOptional("""
            select * from `conversations` where id = :conversationId for update
            """, Map.of("conversationId", conversationId), CONVERSATION_ROW_MAPPER);
    }

    public Optional<ConversationRecord> findByUserPair(Long user1Id, Long user2Id) {
        return queryOptional("""
            select * from `conversations`
            where user1_id = :user1Id and user2_id = :user2Id
            """, Map.of("user1Id", user1Id, "user2Id", user2Id), CONVERSATION_ROW_MAPPER);
    }

    public long createConversation(Long user1Id, Long user2Id) {
        return insertAndReturnId("""
            insert into `conversations` (user1_id, user2_id)
            values (:user1Id, :user2Id)
            """, new MapSqlParameterSource()
            .addValue("user1Id", user1Id)
            .addValue("user2Id", user2Id));
    }

    public void createConversationMember(Long conversationId, Long userId) {
        jdbcTemplate.update("""
            insert into `conversation_members` (conversation_id, user_id)
            values (:conversationId, :userId)
            """, Map.of("conversationId", conversationId, "userId", userId));
    }

    public Optional<ConversationMemberRecord> findMember(Long conversationId, Long userId) {
        return queryOptional("""
            select * from `conversation_members`
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of("conversationId", conversationId, "userId", userId), MEMBER_ROW_MAPPER);
    }

    public Optional<ConversationMemberRecord> lockMember(Long conversationId, Long userId) {
        return queryOptional("""
            select * from `conversation_members`
            where conversation_id = :conversationId and user_id = :userId
            for update
            """, Map.of("conversationId", conversationId, "userId", userId), MEMBER_ROW_MAPPER);
    }

    public void restoreConversationMember(Long conversationId, Long userId) {
        jdbcTemplate.update("""
            update `conversation_members`
            set is_deleted = 0,
                deleted_at = null
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of(
            "conversationId", conversationId,
            "userId", userId
        ));
    }

    public int nextSequenceNo(Long conversationId) {
        Integer nextValue = jdbcTemplate.queryForObject("""
            select coalesce(max(sequence_no), 0) + 1
            from `messages`
            where conversation_id = :conversationId
            """, Map.of("conversationId", conversationId), Integer.class);
        return nextValue == null ? 1 : nextValue;
    }

    public long createMessage(Long conversationId, Long senderUserId, int sequenceNo, String contentText) {
        return insertAndReturnId("""
            insert into `messages` (conversation_id, sender_user_id, sequence_no, content_text)
            values (:conversationId, :senderUserId, :sequenceNo, :contentText)
            """, new MapSqlParameterSource()
            .addValue("conversationId", conversationId)
            .addValue("senderUserId", senderUserId)
            .addValue("sequenceNo", sequenceNo)
            .addValue("contentText", contentText));
    }

    public void insertMessageImages(Long messageId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update("""
                insert into `message_images` (message_id, image_url, sort_order)
                values (:messageId, :imageUrl, :sortOrder)
                """, Map.of(
                "messageId", messageId,
                "imageUrl", imageUrls.get(i),
                "sortOrder", i
            ));
        }
    }

    public Optional<MessageRecord> findByMessageId(Long messageId) {
        return queryOptional("""
            select * from `messages` where id = :messageId
            """, Map.of("messageId", messageId), MESSAGE_ROW_MAPPER);
    }

    public Optional<MessageRecord> findLatestMessage(Long conversationId) {
        return queryOptional("""
            select *
            from `messages`
            where conversation_id = :conversationId
            order by sequence_no desc
            limit 1
            """, Map.of("conversationId", conversationId), MESSAGE_ROW_MAPPER);
    }

    public void updateConversationLastMessage(Long conversationId, Long messageId, LocalDateTime lastMessageAt) {
        jdbcTemplate.update("""
            update `conversations`
            set last_message_id = :messageId,
                last_message_at = :lastMessageAt
            where id = :conversationId
            """, Map.of(
            "conversationId", conversationId,
            "messageId", messageId,
            "lastMessageAt", lastMessageAt
        ));
    }

    public void updateSenderMemberAfterSend(Long conversationId, Long userId, Long messageId, int sequenceNo, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `conversation_members`
            set last_read_message_id = :messageId,
                last_read_sequence_no = :sequenceNo,
                last_read_at = :readAt,
                unread_count = 0,
                is_deleted = 0,
                deleted_at = null
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of(
            "conversationId", conversationId,
            "userId", userId,
            "messageId", messageId,
            "sequenceNo", sequenceNo,
            "readAt", readAt
        ));
    }

    public void updateRecipientMemberAfterSend(Long conversationId, Long userId) {
        jdbcTemplate.update("""
            update `conversation_members`
            set unread_count = unread_count + 1,
                is_deleted = 0,
                deleted_at = null
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of("conversationId", conversationId, "userId", userId));
    }

    public void markRead(Long conversationId, Long userId, Long messageId, int sequenceNo, LocalDateTime readAt) {
        jdbcTemplate.update("""
            update `conversation_members`
            set last_read_message_id = :messageId,
                last_read_sequence_no = :sequenceNo,
                last_read_at = :readAt,
                unread_count = 0
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of(
            "conversationId", conversationId,
            "userId", userId,
            "messageId", messageId,
            "sequenceNo", sequenceNo,
            "readAt", readAt
        ));
    }

    public void updatePinned(Long conversationId, Long userId, boolean pinned, LocalDateTime pinnedAt) {
        jdbcTemplate.update("""
            update `conversation_members`
            set is_pinned = :pinned,
                pinned_at = :pinnedAt
            where conversation_id = :conversationId and user_id = :userId
            """, new MapSqlParameterSource()
            .addValue("conversationId", conversationId)
            .addValue("userId", userId)
            .addValue("pinned", pinned)
            .addValue("pinnedAt", pinned ? pinnedAt : null));
    }

    public void updateMuted(Long conversationId, Long userId, boolean muted) {
        jdbcTemplate.update("""
            update `conversation_members`
            set is_muted = :muted
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of("conversationId", conversationId, "userId", userId, "muted", muted));
    }

    public void softDeleteConversation(Long conversationId, Long userId, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
            update `conversation_members`
            set is_deleted = 1,
                deleted_at = :deletedAt
            where conversation_id = :conversationId and user_id = :userId
            """, Map.of(
            "conversationId", conversationId,
            "userId", userId,
            "deletedAt", deletedAt
        ));
    }

    public PageResult<ConversationSummaryRow> pageConversations(Long userId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*)
            from `conversation_members` cm
            where cm.user_id = :userId and cm.is_deleted = 0
            """, Map.of("userId", userId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<ConversationSummaryRow> list = jdbcTemplate.query("""
            select c.id,
                   c.user1_id,
                   c.user2_id,
                   c.last_message_id,
                   c.last_message_at,
                   cm.unread_count,
                   cm.is_pinned,
                   cm.is_muted,
                   cm.last_read_message_id,
                   cm.last_read_sequence_no,
                   cm.last_read_at,
                   peer.id as peer_user_id,
                   peer.username as peer_username,
                   peer.nickname as peer_nickname,
                   peer.avatar_url as peer_avatar_url,
                   peer.bio as peer_bio,
                   peer.role as peer_role,
                   peer.status as peer_status,
                   peer.`level` as peer_level,
                   m.sender_user_id as last_sender_user_id,
                   m.sequence_no as last_sequence_no,
                   m.content_text as last_content_text,
                   m.created_at as last_message_created_at
            from `conversation_members` cm
            join `conversations` c on c.id = cm.conversation_id
            join `users` peer on peer.id = case when c.user1_id = :userId then c.user2_id else c.user1_id end
            left join `messages` m on m.id = c.last_message_id
            where cm.user_id = :userId and cm.is_deleted = 0
            order by cm.is_pinned desc, c.last_message_at desc, c.id desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new ConversationSummaryRow(
            rs.getLong("id"),
            rs.getLong("peer_user_id"),
            rs.getString("peer_username"),
            rs.getString("peer_nickname"),
            rs.getString("peer_avatar_url"),
            rs.getString("peer_bio"),
            rs.getString("peer_role"),
            rs.getString("peer_status"),
            rs.getInt("peer_level"),
            getLongObject(rs, "last_message_id"),
            getLongObject(rs, "last_sender_user_id"),
            getIntegerObject(rs, "last_sequence_no"),
            rs.getString("last_content_text"),
            getDateTime(rs, "last_message_created_at"),
            rs.getInt("unread_count"),
            rs.getBoolean("is_pinned"),
            rs.getBoolean("is_muted"),
            getLongObject(rs, "last_read_message_id"),
            getIntegerObject(rs, "last_read_sequence_no"),
            getDateTime(rs, "last_read_at"),
            getDateTime(rs, "last_message_at")
        ));
        return new PageResult<>(total, page, pageSize, list);
    }

    public PageResult<MessageRow> pageMessages(Long conversationId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*) from `messages`
            where conversation_id = :conversationId
            """, Map.of("conversationId", conversationId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("conversationId", conversationId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<MessageRow> list = jdbcTemplate.query("""
            select *
            from `messages`
            where conversation_id = :conversationId
            order by sequence_no desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new MessageRow(
            rs.getLong("id"),
            rs.getLong("conversation_id"),
            rs.getLong("sender_user_id"),
            rs.getInt("sequence_no"),
            rs.getString("content_text"),
            getDateTime(rs, "created_at")
        ));
        return new PageResult<>(total, page, pageSize, list);
    }

    public Map<Long, List<String>> listImagesByMessageIds(Collection<Long> messageIds) {
        if (!hasIds(messageIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select message_id, image_url
            from `message_images`
            where message_id in (:messageIds)
            order by message_id asc, sort_order asc, id asc
            """, Map.of("messageIds", messageIds));
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long messageId = ((Number) row.get("message_id")).longValue();
            map.computeIfAbsent(messageId, key -> new java.util.ArrayList<>()).add((String) row.get("image_url"));
        }
        return map;
    }

    private static ConversationRecord mapConversation(ResultSet rs, int rowNum) throws SQLException {
        return new ConversationRecord(
            rs.getLong("id"),
            rs.getLong("user1_id"),
            rs.getLong("user2_id"),
            getLongObject(rs, "last_message_id"),
            getDateTime(rs, "last_message_at"),
            getDateTime(rs, "created_at")
        );
    }

    private static ConversationMemberRecord mapMember(ResultSet rs, int rowNum) throws SQLException {
        return new ConversationMemberRecord(
            rs.getLong("id"),
            rs.getLong("conversation_id"),
            rs.getLong("user_id"),
            getDateTime(rs, "joined_at"),
            getLongObject(rs, "last_read_message_id"),
            getIntegerObject(rs, "last_read_sequence_no"),
            getDateTime(rs, "last_read_at"),
            rs.getInt("unread_count"),
            rs.getBoolean("is_pinned"),
            getDateTime(rs, "pinned_at"),
            rs.getBoolean("is_muted"),
            rs.getBoolean("is_deleted"),
            getDateTime(rs, "deleted_at")
        );
    }

    private static MessageRecord mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new MessageRecord(
            rs.getLong("id"),
            rs.getLong("conversation_id"),
            rs.getLong("sender_user_id"),
            rs.getInt("sequence_no"),
            rs.getString("content_text"),
            getDateTime(rs, "created_at")
        );
    }

    public record ConversationSummaryRow(
        Long id,
        Long peerUserId,
        String peerUsername,
        String peerNickname,
        String peerAvatarUrl,
        String peerBio,
        String peerRole,
        String peerStatus,
        Integer peerLevel,
        Long lastMessageId,
        Long lastSenderUserId,
        Integer lastSequenceNo,
        String lastContentText,
        LocalDateTime lastMessageCreatedAt,
        Integer unreadCount,
        boolean pinned,
        boolean muted,
        Long lastReadMessageId,
        Integer lastReadSequenceNo,
        LocalDateTime lastReadAt,
        LocalDateTime updatedAt
    ) {
    }

    public record MessageRow(
        Long id,
        Long conversationId,
        Long senderUserId,
        Integer sequenceNo,
        String contentText,
        LocalDateTime createdAt
    ) {
    }
}
