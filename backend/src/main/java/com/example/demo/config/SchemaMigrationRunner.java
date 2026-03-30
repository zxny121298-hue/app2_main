package com.example.demo.config;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureUsersTokenVersionColumn();
        ensureCoinLedgerChangeTypeSupportsSignIn();
        try {
            ensureSearchHistoryTable();
        } catch (Exception e) {
            // 与登录/注册无关的辅助表：DDL 失败时不应阻止应用启动（避免误判为「加搜索后登不进去」）
            log.error("创建 search_history 表失败，搜索历史功能将不可用，请检查数据库权限或手动执行 database.sql 中对应建表语句", e);
        }
    }

    /**
     * 不使用外键：部分环境对 FK 校验/权限更敏感；业务上仍按 user_id 查询，一致性由应用保证。
     */
    /**
     * 旧库若未执行 database.sql 末尾的 ALTER，会缺少 token_version，导致登录/注册查询 users 映射失败。
     */
    private void ensureUsersTokenVersionColumn() {
        var hasColumn = jdbcTemplate.getJdbcOperations().query("""
            select 1
            from information_schema.columns
            where table_schema = database()
              and table_name = 'users'
              and column_name = 'token_version'
            limit 1
            """, (rs, rowNum) -> 1);
        if (!hasColumn.isEmpty()) {
            return;
        }
        var hasUsers = jdbcTemplate.getJdbcOperations().query("""
            select 1
            from information_schema.tables
            where table_schema = database()
              and table_name = 'users'
            limit 1
            """, (rs, rowNum) -> 1);
        if (hasUsers.isEmpty()) {
            log.warn("跳过 users.token_version：当前库中不存在 users 表");
            return;
        }
        jdbcTemplate.getJdbcOperations().execute("""
            alter table `users`
            add column `token_version` int unsigned not null default 0 after `password_hash`
            """);
        log.info("已为 users 表补充 token_version 列");
    }

    private void ensureSearchHistoryTable() {
        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS `search_history` (
              `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
              `user_id` BIGINT UNSIGNED NOT NULL,
              `keyword` VARCHAR(200) NOT NULL,
              `search_type` VARCHAR(20) NOT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              KEY `idx_search_history_user_created` (`user_id`, `created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户搜索历史'
            """);
        log.debug("search_history table ready");
    }

    private void ensureCoinLedgerChangeTypeSupportsSignIn() {
        var columnTypes = jdbcTemplate.getJdbcOperations().query("""
            select column_type
            from information_schema.columns
            where table_schema = database()
              and table_name = 'coin_ledgers'
              and column_name = 'change_type'
            """, (rs, rowNum) -> rs.getString("column_type"));

        if (columnTypes.isEmpty()) {
            log.warn("Skipped coin_ledgers enum migration because coin_ledgers.change_type was not found");
            return;
        }

        String columnType = columnTypes.get(0).toLowerCase(Locale.ROOT);
        if (columnType.contains("'sign_in'")) {
            return;
        }

        jdbcTemplate.getJdbcOperations().execute("""
            ALTER TABLE `coin_ledgers`
            MODIFY COLUMN `change_type` ENUM(
              'sign_in',
              'reward_send',
              'reward_receive',
              'admin_adjust',
              'system_grant',
              'system_deduct',
              'refund'
            ) NOT NULL
            """);

        log.info("Updated coin_ledgers.change_type enum to include sign_in");
    }
}
