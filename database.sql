-- ALTER TABLE `users`
-- ADD COLUMN `token_version` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `password_hash`;

CREATE DATABASE IF NOT EXISTS `chat_forum`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `chat_forum`;

-- =========================================================
-- 1. 用户表
-- =========================================================
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '用户简介',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `token_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '登录 token 版本，改密后递增',
  `role` ENUM('admin', 'user') NOT NULL DEFAULT 'user' COMMENT '角色',
  `status` ENUM('active', 'banned') NOT NULL DEFAULT 'active' COMMENT '账号状态',
  `banned_until_at` DATETIME DEFAULT NULL COMMENT '封号截止时间，NULL表示未封或永久封禁',
  `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封号原因',
  `muted_until_at` DATETIME DEFAULT NULL COMMENT '禁言截止时间',
  `mute_reason` VARCHAR(255) DEFAULT NULL COMMENT '禁言原因',
  `coin_balance` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前硬币余额',
  `level` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '当前等级：1~6',
  `total_exp` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计总经验',
  `current_level_exp` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前等级内经验',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后一次登录时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  KEY `idx_users_role` (`role`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_muted_until_at` (`muted_until_at`),
  CONSTRAINT `chk_users_level_range` CHECK (`level` BETWEEN 1 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- =========================================================
-- 2. 等级规则表
-- =========================================================
CREATE TABLE `level_rules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level` TINYINT UNSIGNED NOT NULL COMMENT '等级：1~6',
  `level_name` VARCHAR(20) NOT NULL COMMENT '等级名称，例如 Lv1',
  `upgrade_need_exp` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '升到下一级所需经验，最高级为0',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_rules_level` (`level`),
  CONSTRAINT `chk_level_rules_level_range` CHECK (`level` BETWEEN 1 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级规则表';


-- =========================================================
-- 3. 用户关注关系表
-- =========================================================
CREATE TABLE `user_follows` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_user_id` BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
  `followee_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follows_pair` (`follower_user_id`, `followee_user_id`),
  KEY `idx_user_follows_followee_user_id` (`followee_user_id`),
  CONSTRAINT `fk_user_follows_follower_user_id`
    FOREIGN KEY (`follower_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_user_follows_followee_user_id`
    FOREIGN KEY (`followee_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';


-- =========================================================
-- 4. 板块表
-- =========================================================
CREATE TABLE `boards` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '板块ID',
  `name` VARCHAR(100) NOT NULL COMMENT '板块名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '板块简介',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` ENUM('enabled', 'disabled') NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_boards_name` (`name`),
  KEY `idx_boards_status_sort_order` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛板块表';


-- =========================================================
-- 5. 帖子表
-- =========================================================
CREATE TABLE `posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `board_id` BIGINT UNSIGNED NOT NULL COMMENT '所属板块ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发帖用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
  `content_text` MEDIUMTEXT DEFAULT NULL COMMENT '帖子正文文本，可为空（纯图片帖）',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `reward_coin_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计被打赏硬币数',
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `pinned_at` DATETIME DEFAULT NULL COMMENT '置顶时间',
  `pinned_by_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '置顶操作管理员ID',
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否精华',
  `featured_at` DATETIME DEFAULT NULL COMMENT '设为精华时间',
  `featured_by_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '精华操作管理员ID',
  `status` ENUM('normal', 'deleted') NOT NULL DEFAULT 'normal' COMMENT '帖子状态',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted_by_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除操作管理员ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_posts_board_status_created_at` (`board_id`, `status`, `created_at`),
  KEY `idx_posts_board_pinned_featured` (`board_id`, `status`, `is_pinned`, `is_featured`, `created_at`),
  KEY `idx_posts_user_id_created_at` (`user_id`, `created_at`),
  KEY `idx_posts_pinned_by_user_id` (`pinned_by_user_id`),
  KEY `idx_posts_featured_by_user_id` (`featured_by_user_id`),
  KEY `idx_posts_deleted_by_user_id` (`deleted_by_user_id`),
  CONSTRAINT `fk_posts_board_id`
    FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_posts_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_posts_pinned_by_user_id`
    FOREIGN KEY (`pinned_by_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_posts_featured_by_user_id`
    FOREIGN KEY (`featured_by_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_posts_deleted_by_user_id`
    FOREIGN KEY (`deleted_by_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';


-- =========================================================
-- 6. 帖子图片表
-- =========================================================
CREATE TABLE `post_images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_images_post_id_sort_order` (`post_id`, `sort_order`),
  CONSTRAINT `fk_post_images_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子图片表';


-- =========================================================
-- 7. 评论表
-- =========================================================
CREATE TABLE `post_comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
  `parent_comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID，NULL表示一级评论',
  `reply_to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被回复用户ID',
  `content_text` TEXT DEFAULT NULL COMMENT '评论文本，可为空（纯图片回复）',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reward_coin_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计被打赏硬币数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_comments_post_id_created_at` (`post_id`, `created_at`),
  KEY `idx_post_comments_post_parent` (`post_id`, `parent_comment_id`),
  KEY `idx_post_comments_user_id_created_at` (`user_id`, `created_at`),
  KEY `idx_post_comments_reply_to_user_id` (`reply_to_user_id`),
  CONSTRAINT `fk_post_comments_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_post_comments_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_post_comments_parent_comment_id`
    FOREIGN KEY (`parent_comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_post_comments_reply_to_user_id`
    FOREIGN KEY (`reply_to_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';


-- =========================================================
-- 8. 评论图片表
-- =========================================================
CREATE TABLE `comment_images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_images_comment_id_sort_order` (`comment_id`, `sort_order`),
  CONSTRAINT `fk_comment_images_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论图片表';


-- =========================================================
-- 9. 帖子点赞表
-- =========================================================
CREATE TABLE `post_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_likes_post_user` (`post_id`, `user_id`),
  KEY `idx_post_likes_user_id` (`user_id`),
  CONSTRAINT `fk_post_likes_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_post_likes_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';


-- =========================================================
-- 10. 评论点赞表
-- =========================================================
CREATE TABLE `comment_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_likes_comment_user` (`comment_id`, `user_id`),
  KEY `idx_comment_likes_user_id` (`user_id`),
  CONSTRAINT `fk_comment_likes_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_likes_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';


-- =========================================================
-- 11. 帖子收藏表
-- =========================================================
CREATE TABLE `post_favorites` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '收藏用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_favorites_post_user` (`post_id`, `user_id`),
  KEY `idx_post_favorites_user_id_created_at` (`user_id`, `created_at`),
  CONSTRAINT `fk_post_favorites_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_post_favorites_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表';


-- =========================================================
-- 12. 评论收藏表
-- =========================================================
CREATE TABLE `comment_favorites` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '收藏用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_favorites_comment_user` (`comment_id`, `user_id`),
  KEY `idx_comment_favorites_user_id_created_at` (`user_id`, `created_at`),
  CONSTRAINT `fk_comment_favorites_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_favorites_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论收藏表';


-- =========================================================
-- 13. 私信会话表（一对一）
-- 约定：user1_id 固定存较小ID，user2_id 固定存较大ID
-- =========================================================
CREATE TABLE `conversations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user1_id` BIGINT UNSIGNED NOT NULL COMMENT '会话用户1，固定存较小用户ID',
  `user2_id` BIGINT UNSIGNED NOT NULL COMMENT '会话用户2，固定存较大用户ID',
  `last_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversations_user_pair` (`user1_id`, `user2_id`),
  KEY `idx_conversations_user1_last_message_at` (`user1_id`, `last_message_at`),
  KEY `idx_conversations_user2_last_message_at` (`user2_id`, `last_message_at`),
  CONSTRAINT `fk_conversations_user1_id`
    FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_conversations_user2_id`
    FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';


-- =========================================================
-- 14. 私信会话成员状态表
-- =========================================================
CREATE TABLE `conversation_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入会话时间',
  `last_read_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后已读消息ID',
  `last_read_sequence_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最后已读消息在本会话内的序号',
  `last_read_at` DATETIME DEFAULT NULL COMMENT '最后已读时间',
  `unread_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '未读消息数',
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶该会话',
  `pinned_at` DATETIME DEFAULT NULL COMMENT '置顶时间',
  `is_muted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免打扰',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否仅自己删除该会话',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '仅自己删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_members_pair` (`conversation_id`, `user_id`),
  KEY `idx_conversation_members_user_id` (`user_id`),
  KEY `idx_conversation_members_user_state` (`user_id`, `is_deleted`, `is_pinned`, `last_read_at`),
  CONSTRAINT `fk_conversation_members_conversation_id`
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_conversation_members_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话成员状态表';


-- =========================================================
-- 15. 私信消息表
-- =========================================================
CREATE TABLE `messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `sender_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者用户ID',
  `sequence_no` INT UNSIGNED NOT NULL COMMENT '消息在会话内的递增序号，从1开始',
  `content_text` TEXT DEFAULT NULL COMMENT '消息文本，可为空（纯图片消息）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_messages_conversation_sequence_no` (`conversation_id`, `sequence_no`),
  KEY `idx_messages_conversation_id_created_at` (`conversation_id`, `created_at`),
  KEY `idx_messages_sender_user_id` (`sender_user_id`),
  CONSTRAINT `fk_messages_conversation_id`
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_messages_sender_user_id`
    FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';


-- =========================================================
-- 16. 私信消息图片表
-- =========================================================
CREATE TABLE `message_images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `message_id` BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_images_message_id_sort_order` (`message_id`, `sort_order`),
  CONSTRAINT `fk_message_images_message_id`
    FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息图片表';


-- =========================================================
-- 17. 打赏记录表
-- =========================================================
CREATE TABLE `rewards` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '打赏记录ID',
  `sender_user_id` BIGINT UNSIGNED NOT NULL COMMENT '打赏者用户ID',
  `recipient_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被打赏用户ID',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被打赏的帖子ID',
  `comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被打赏的评论ID',
  `coin_amount` BIGINT UNSIGNED NOT NULL COMMENT '打赏硬币数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
  PRIMARY KEY (`id`),
  KEY `idx_rewards_sender_user_id_created_at` (`sender_user_id`, `created_at`),
  KEY `idx_rewards_recipient_user_id_created_at` (`recipient_user_id`, `created_at`),
  KEY `idx_rewards_post_id` (`post_id`),
  KEY `idx_rewards_comment_id` (`comment_id`),
  CONSTRAINT `fk_rewards_sender_user_id`
    FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_rewards_recipient_user_id`
    FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_rewards_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rewards_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_rewards_coin_amount_positive` CHECK (`coin_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打赏记录表';


-- =========================================================
-- 18. 通知聚合表
-- =========================================================
CREATE TABLE `notification_groups` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知聚合ID',
  `recipient_user_id` BIGINT UNSIGNED NOT NULL COMMENT '通知接收用户ID',
  `event_type` ENUM(
    'private_message',
    'post_comment',
    'comment_reply',
    'post_like',
    'comment_like',
    'post_reward',
    'comment_reward'
  ) NOT NULL COMMENT '通知类型',
  `target_type` ENUM('conversation', 'post', 'comment') NOT NULL COMMENT '聚合目标类型',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '聚合目标ID',
  `latest_actor_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近一次触发者用户ID',
  `total_count` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '聚合内总通知数',
  `unread_count` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '聚合内未读通知数',
  `last_read_at` DATETIME DEFAULT NULL COMMENT '该聚合最后一次已读时间',
  `latest_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次触发时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_groups_unique`
    (`recipient_user_id`, `event_type`, `target_type`, `target_id`),
  KEY `idx_notification_groups_recipient_unread_latest`
    (`recipient_user_id`, `unread_count`, `latest_at`),
  KEY `idx_notification_groups_latest_actor_user_id` (`latest_actor_user_id`),
  CONSTRAINT `fk_notification_groups_recipient_user_id`
    FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_groups_latest_actor_user_id`
    FOREIGN KEY (`latest_actor_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知聚合表';


-- =========================================================
-- 19. 通知明细表
-- =========================================================
CREATE TABLE `notification_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知明细ID',
  `group_id` BIGINT UNSIGNED NOT NULL COMMENT '所属通知聚合ID',
  `recipient_user_id` BIGINT UNSIGNED NOT NULL COMMENT '通知接收用户ID',
  `actor_user_id` BIGINT UNSIGNED NOT NULL COMMENT '触发者用户ID',
  `event_type` ENUM(
    'private_message',
    'post_comment',
    'comment_reply',
    'post_like',
    'comment_like',
    'post_reward',
    'comment_reward'
  ) NOT NULL COMMENT '通知类型',
  `target_type` ENUM('conversation', 'post', 'comment') NOT NULL COMMENT '目标类型',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
  `conversation_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联会话ID',
  `message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联消息ID',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联帖子ID',
  `comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联评论ID',
  `reward_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联打赏ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '已读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_items_group_id_created_at` (`group_id`, `created_at`),
  KEY `idx_notification_items_recipient_is_read_created_at` (`recipient_user_id`, `is_read`, `created_at`),
  KEY `idx_notification_items_actor_user_id` (`actor_user_id`),
  KEY `idx_notification_items_conversation_id` (`conversation_id`),
  KEY `idx_notification_items_message_id` (`message_id`),
  KEY `idx_notification_items_post_id` (`post_id`),
  KEY `idx_notification_items_comment_id` (`comment_id`),
  KEY `idx_notification_items_reward_id` (`reward_id`),
  CONSTRAINT `fk_notification_items_group_id`
    FOREIGN KEY (`group_id`) REFERENCES `notification_groups` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_recipient_user_id`
    FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_actor_user_id`
    FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_conversation_id`
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_message_id`
    FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_items_reward_id`
    FOREIGN KEY (`reward_id`) REFERENCES `rewards` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知明细表';


-- =========================================================
-- 20. 硬币流水表
-- =========================================================
CREATE TABLE `coin_ledgers` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '余额变动所属用户ID',
  `change_type` ENUM(
    'sign_in',
    'reward_send',
    'reward_receive',
    'admin_adjust',
    'system_grant',
    'system_deduct',
    'refund'
  ) NOT NULL COMMENT '变动类型',
  `change_amount` BIGINT NOT NULL COMMENT '变动金额，可正可负',
  `balance_after` BIGINT UNSIGNED NOT NULL COMMENT '变动后的余额',
  `related_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联用户ID，例如打赏对方',
  `reward_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联打赏记录ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_coin_ledgers_user_id_created_at` (`user_id`, `created_at`),
  KEY `idx_coin_ledgers_related_user_id` (`related_user_id`),
  KEY `idx_coin_ledgers_reward_id` (`reward_id`),
  CONSTRAINT `fk_coin_ledgers_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_coin_ledgers_related_user_id`
    FOREIGN KEY (`related_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_coin_ledgers_reward_id`
    FOREIGN KEY (`reward_id`) REFERENCES `rewards` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_coin_ledgers_change_amount_not_zero`
    CHECK (`change_amount` <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='硬币流水表';


-- =========================================================
-- 21. 每日签到表
-- =========================================================
CREATE TABLE `check_ins` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `check_in_date` DATE NOT NULL COMMENT '签到日期',
  `exp_gain` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次签到获得经验',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_ins_user_date` (`user_id`, `check_in_date`),
  KEY `idx_check_ins_date` (`check_in_date`),
  CONSTRAINT `fk_check_ins_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日签到表';


-- =========================================================
-- 22. 用户经验流水表
-- =========================================================
CREATE TABLE `user_exp_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '经验流水ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '获得经验的用户ID',
  `change_type` ENUM(
    'sign_in',
    'create_post',
    'create_comment',
    'create_reply',
    'post_pinned',
    'post_featured',
    'admin_adjust'
  ) NOT NULL COMMENT '经验变动类型',
  `change_exp` INT NOT NULL COMMENT '经验变动值，通常为正数，后台调整可为负数',
  `total_exp_after` INT UNSIGNED NOT NULL COMMENT '变动后的累计总经验',
  `level_after` TINYINT UNSIGNED NOT NULL COMMENT '变动后的等级',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联帖子ID',
  `comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联评论ID',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID，系统为空，管理员调整时可记录管理员',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_exp_logs_user_id_created_at` (`user_id`, `created_at`),
  KEY `idx_user_exp_logs_change_type` (`change_type`),
  KEY `idx_user_exp_logs_post_id` (`post_id`),
  KEY `idx_user_exp_logs_comment_id` (`comment_id`),
  KEY `idx_user_exp_logs_operator_user_id` (`operator_user_id`),
  CONSTRAINT `fk_user_exp_logs_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_user_exp_logs_post_id`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_user_exp_logs_comment_id`
    FOREIGN KEY (`comment_id`) REFERENCES `post_comments` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_user_exp_logs_operator_user_id`
    FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户经验流水表';


-- =========================================================
-- 23. 补充循环引用外键
-- =========================================================
ALTER TABLE `conversations`
ADD CONSTRAINT `fk_conversations_last_message_id`
  FOREIGN KEY (`last_message_id`) REFERENCES `messages` (`id`)
  ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `conversation_members`
ADD CONSTRAINT `fk_conversation_members_last_read_message_id`
  FOREIGN KEY (`last_read_message_id`) REFERENCES `messages` (`id`)
  ON DELETE SET NULL ON UPDATE CASCADE;


-- =========================================================
-- 24. 初始化等级规则
-- =========================================================
INSERT INTO `level_rules` (`level`, `level_name`, `upgrade_need_exp`)
VALUES
(1, 'Lv1', 100),
(2, 'Lv2', 200),
(3, 'Lv3', 400),
(4, 'Lv4', 800),
(5, 'Lv5', 1600),
(6, 'Lv6', 0);


-- =========================================================
-- 25. 搜索历史（登录用户）
-- =========================================================
CREATE TABLE IF NOT EXISTS `search_history` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `keyword` VARCHAR(200) NOT NULL COMMENT '搜索关键词',
  `search_type` VARCHAR(20) NOT NULL COMMENT 'content / user / topic',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_history_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户搜索历史表';
