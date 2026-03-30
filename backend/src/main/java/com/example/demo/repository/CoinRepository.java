package com.example.demo.repository;

import com.example.demo.common.PageResult;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CoinRepository extends BaseRepository {

    public CoinRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public long createReward(Long senderUserId, Long recipientUserId, Long postId, Long commentId, long coinAmount) {
        return insertAndReturnId("""
            insert into `rewards`
            (sender_user_id, recipient_user_id, post_id, comment_id, coin_amount)
            values
            (:senderUserId, :recipientUserId, :postId, :commentId, :coinAmount)
            """, new MapSqlParameterSource()
            .addValue("senderUserId", senderUserId)
            .addValue("recipientUserId", recipientUserId)
            .addValue("postId", postId)
            .addValue("commentId", commentId)
            .addValue("coinAmount", coinAmount));
    }

    public long createCoinLedger(Long userId, String changeType, long changeAmount, long balanceAfter,
                                 Long relatedUserId, Long rewardId, String description) {
        return insertAndReturnId("""
            insert into `coin_ledgers`
            (user_id, change_type, change_amount, balance_after, related_user_id, reward_id, description)
            values
            (:userId, :changeType, :changeAmount, :balanceAfter, :relatedUserId, :rewardId, :description)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("changeType", changeType)
            .addValue("changeAmount", changeAmount)
            .addValue("balanceAfter", balanceAfter)
            .addValue("relatedUserId", relatedUserId)
            .addValue("rewardId", rewardId)
            .addValue("description", description));
    }

    public PageResult<CoinLedgerRow> pageCoinLedgers(Long userId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*) from `coin_ledgers`
            where user_id = :userId
            """, Map.of("userId", userId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<CoinLedgerRow> list = jdbcTemplate.query("""
            select *
            from `coin_ledgers`
            where user_id = :userId
            order by created_at desc, id desc
            limit :limit offset :offset
            """, params, (rs, rowNum) -> new CoinLedgerRow(
            rs.getLong("id"),
            rs.getString("change_type"),
            rs.getLong("change_amount"),
            rs.getLong("balance_after"),
            getLongObject(rs, "related_user_id"),
            getLongObject(rs, "reward_id"),
            rs.getString("description"),
            getDateTime(rs, "created_at")
        ));
        return new PageResult<>(total, page, pageSize, list);
    }

    public record CoinLedgerRow(
        Long id,
        String changeType,
        Long changeAmount,
        Long balanceAfter,
        Long relatedUserId,
        Long rewardId,
        String description,
        LocalDateTime createdAt
    ) {
    }
}
