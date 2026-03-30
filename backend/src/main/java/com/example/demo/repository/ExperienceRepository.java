package com.example.demo.repository;

import com.example.demo.common.PageResult;
import com.example.demo.model.ForumModels.CheckInRecord;
import com.example.demo.model.ForumModels.LevelRuleRecord;
import com.example.demo.model.ForumModels.UserExpLogRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExperienceRepository extends BaseRepository {

    private static final RowMapper<LevelRuleRecord> LEVEL_RULE_ROW_MAPPER = (rs, rowNum) -> new LevelRuleRecord(
        rs.getLong("id"),
        rs.getInt("level"),
        rs.getString("level_name"),
        rs.getInt("upgrade_need_exp")
    );

    private static final RowMapper<UserExpLogRecord> EXP_LOG_ROW_MAPPER = ExperienceRepository::mapExpLog;

    public ExperienceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public boolean hasCheckedIn(Long userId, LocalDate checkInDate) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `check_ins`
            where user_id = :userId and check_in_date = :checkInDate
            """, Map.of("userId", userId, "checkInDate", checkInDate), Integer.class);
        return count != null && count > 0;
    }

    public long createCheckIn(Long userId, LocalDate checkInDate, int expGain) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("checkInDate", checkInDate)
            .addValue("expGain", expGain);
        return insertAndReturnId("""
            insert into `check_ins` (user_id, check_in_date, exp_gain)
            values (:userId, :checkInDate, :expGain)
            """, params);
    }

    public List<LevelRuleRecord> listLevelRules() {
        return jdbcTemplate.query("""
            select id, `level`, level_name, upgrade_need_exp
            from `level_rules`
            order by `level` asc
            """, LEVEL_RULE_ROW_MAPPER);
    }

    public long createExpLog(Long userId, String changeType, int changeExp, int totalExpAfter, int levelAfter,
                             Long postId, Long commentId, Long operatorUserId, String remark) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("changeType", changeType)
            .addValue("changeExp", changeExp)
            .addValue("totalExpAfter", totalExpAfter)
            .addValue("levelAfter", levelAfter)
            .addValue("postId", postId)
            .addValue("commentId", commentId)
            .addValue("operatorUserId", operatorUserId)
            .addValue("remark", remark);
        return insertAndReturnId("""
            insert into `user_exp_logs`
            (user_id, change_type, change_exp, total_exp_after, level_after, post_id, comment_id, operator_user_id, remark)
            values
            (:userId, :changeType, :changeExp, :totalExpAfter, :levelAfter, :postId, :commentId, :operatorUserId, :remark)
            """, params);
    }

    public boolean existsPostExpLog(Long postId, String changeType) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*) from `user_exp_logs`
            where post_id = :postId and change_type = :changeType
            """, Map.of("postId", postId, "changeType", changeType), Integer.class);
        return count != null && count > 0;
    }

    public PageResult<UserExpLogRecord> pageExpLogs(Long userId, long page, long pageSize) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
            select count(*) from `user_exp_logs` where user_id = :userId
            """, Map.of("userId", userId), Long.class)).orElse(0L);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", pageSize);
        params.put("offset", offset(page, pageSize));
        List<UserExpLogRecord> list = jdbcTemplate.query("""
            select * from `user_exp_logs`
            where user_id = :userId
            order by created_at desc, id desc
            limit :limit offset :offset
            """, params, EXP_LOG_ROW_MAPPER);
        return new PageResult<>(total, page, pageSize, list);
    }

    public Optional<CheckInRecord> findTodayCheckIn(Long userId, LocalDate date) {
        return queryOptional("""
            select * from `check_ins`
            where user_id = :userId and check_in_date = :checkInDate
            """, Map.of("userId", userId, "checkInDate", date), (rs, rowNum) -> new CheckInRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            getDate(rs, "check_in_date"),
            rs.getInt("exp_gain"),
            getDateTime(rs, "created_at")
        ));
    }

    private static UserExpLogRecord mapExpLog(ResultSet rs, int rowNum) throws SQLException {
        return new UserExpLogRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("change_type"),
            rs.getInt("change_exp"),
            rs.getInt("total_exp_after"),
            rs.getInt("level_after"),
            getLongObject(rs, "post_id"),
            getLongObject(rs, "comment_id"),
            getLongObject(rs, "operator_user_id"),
            rs.getString("remark"),
            getDateTime(rs, "created_at")
        );
    }
}
