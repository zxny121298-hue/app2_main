-- =============================================================================
-- 精简演示数据：4 个板块 / 5 个用户 / 12 个帖子 / 若干评论与打赏
-- 可重复执行：通过 BINARY + NOT EXISTS 避免重复插入
-- =============================================================================

USE `chat_forum`;

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------- 板块（4）
INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '学习交流', '课程作业、考试复习与自习组队', 10, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '学习交流');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '校园生活', '食堂、宿舍、校园日常与吐槽', 20, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '校园生活');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '二手集市', '教材、数码、生活用品转让', 30, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '二手集市');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '休闲灌水', '随便聊聊、分享心情', 40, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '休闲灌水');

SET @b_study := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '学习交流' LIMIT 1);
SET @b_life  := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '校园生活' LIMIT 1);
SET @b_used  := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '二手集市' LIMIT 1);
SET @b_chat  := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '休闲灌水' LIMIT 1);

-- ----------------------------------------------------------------------------- 用户（5）
-- 密码统一：明文 password，对应 Spring Security 兼容 BCrypt
SET @pwd := '$2a$10$9N8N35BVs5TLqGL3pspAte5OWWA2a2aZIs.EGp7At7txYakFERMue';

INSERT INTO `users`
(`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`,
 `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_a', '李小河', NULL,
       '计科大二，后端和算法打基础中。',
       @pwd, 'user', 'active', 260, 3, 380, 60
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_a');

INSERT INTO `users`
(`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`,
 `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_b', '王晴', NULL,
       '经管学院，爱记账也爱参加社团活动。',
       @pwd, 'user', 'active', 180, 2, 140, 40
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_b');

INSERT INTO `users`
(`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`,
 `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_c', '周十月', NULL,
       '文学院，写影评和随笔，比起刷视频更爱刷图书馆。',
       @pwd, 'user', 'active', 320, 4, 520, 110
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_c');

INSERT INTO `users`
(`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`,
 `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_d', '赵一帆', NULL,
       '物理学院，实验室与篮球场两头跑。',
       @pwd, 'user', 'active', 95, 2, 95, 20
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_d');

INSERT INTO `users`
(`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`,
 `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_e', '陈北北', NULL,
       '新闻与传播专业，喜欢拍 vlog 和做播客。',
       @pwd, 'user', 'active', 210, 3, 260, 55
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_e');

SET @u1 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_a' LIMIT 1);
SET @u2 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_b' LIMIT 1);
SET @u3 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_c' LIMIT 1);
SET @u4 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_d' LIMIT 1);
SET @u5 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_e' LIMIT 1);

-- ----------------------------------------------------------------------------- 帖子标记：便于以后批量删除或统计
SET @mark := '[simple_seed] ';

-- ============================= 帖子（12 条） =============================

-- 学习交流：4 条（共用 @b_study）
INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_study, @u1, CONCAT(@mark, '期末周自习室经验分享'),
'这学期期末周基本天天泡在自习室，总结几条小经验：第一，尽量固定一个座位，减少每天找位置的时间；第二，提前把要复习的章节拆成小块，每块 40～50 分钟；第三，别硬扛困意，犯困就出去走两圈或者洗把脸。比起熬到两三点，稳定在十二点之前睡觉，第二天效率会更高。',
5, 3, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '期末周自习室经验分享'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_study, @u2, CONCAT(@mark, '高数刷题安排建议'),
'高数后半学期题量很大，可以按「每天两道大题 + 五道选择」的节奏推进。遇到不会的题，先自己在草稿纸上写出所有已知条件和要证明的结论，再翻参考答案，只看关键一步，剩下的自己补齐，印象会更深。',
3, 2, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '高数刷题安排建议'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_study, @u3, CONCAT(@mark, '英语四级听力练习方式'),
'听力最重要的是每天保持输入时间，而不是一次性刷很多套题。可以先用旧题库做精听，把自己听不出来的句子反复听三到五遍，再对照原文标记生词和连读弱读的地方。一周之后再做同一套题，基本能感觉到进步。',
4, 2, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '英语四级听力练习方式'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_study, @u4, CONCAT(@mark, '和室友一起制定复习计划'),
'我们宿舍四个人专业不同，但约了一份「安静协议」：白天谁有网课就提前在群里说，其他人戴耳机；晚上十点之后只允许小声说话或用手机聊天。每个人在墙上贴了自己的复习进度表，偶尔互相打个气，比一个人闷头复习轻松一些。',
2, 1, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '和室友一起制定复习计划'));

-- 校园生活：3 条
INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_life, @u2, CONCAT(@mark, '第一次在学校操场夜跑的感受'),
'昨天晚上第一次跟着同学去操场夜跑，本来以为会很累，结果跑完反而更清醒了。操场灯光不刺眼，风也刚刚好，绕着跑道一圈一圈转的时候，白天的烦心事似乎都被甩在身后了。最意外的是，跑完回宿舍洗完澡，睡得特别踏实。',
6, 2, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '第一次在学校操场夜跑的感受'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_life, @u5, CONCAT(@mark, '食堂哪家窗口最适合赶时间'),
'个人感觉一食堂一楼右侧那两个快餐窗口出菜最快，尤其是中午十二点二十以后，人流会明显少很多。如果你只想迅速吃完回教室，可以优先考虑这两个；但如果在意口味，二楼的拌面和石锅拌饭会更香，就是要多排一会儿队。',
3, 1, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '食堂哪家窗口最适合赶时间'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_life, @u1, CONCAT(@mark, '宿舍小阳台的种植计划'),
'春天到了打算在阳台摆几盆小植物，最好是耐阴、好活的那种，比如绿萝、多肉或者罗勒。既能装饰宿舍，又能在写作业时闻到一点点青草味。欢迎有种植经验的同学推荐一下好养又不招蚊虫的品种。',
1, 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '宿舍小阳台的种植计划'));

-- 二手集市：3 条
INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_used, @u1, CONCAT(@mark, '出一台九成新电纸书'),
'准备换大尺寸的电纸书，把现在这台六英寸的出掉。屏幕没有划痕，边框有一处轻微磕碰，送原装保护套和一根数据线。主要用来看专业书和英文原版，眼睛比看手机舒服很多。有意可以私信，价格可以小刀。',
4, 1, 5, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '出一台九成新电纸书'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_used, @u4, CONCAT(@mark, '求购二手人体工学椅'),
'最近腰有点扛不住了，想入一把坐着舒服一点的人体工学椅，但预算有限，先在校内问问有没有同学想出闲置的。要求是靠背和腰托能调节，扶手最好也是可调的，品牌不限，只要稳定不晃、坐感别太塌就行。',
2, 1, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '求购二手人体工学椅'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_used, @u5, CONCAT(@mark, '出一套大一高数教材和笔记本'),
'书上有不少课堂板书和习题的思路标注，如果你刚好是同一个老师的课应该会比较有参考价值。笔记本里主要是我自己整理的公式和常见题型，可以一起打包带走。价格不高，主要是希望对学弟学妹有点帮助。',
3, 2, 3, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '出一套大一高数教材和笔记本'));

-- 休闲灌水：2 条
INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_chat, @u2, CONCAT(@mark, '最近一次让你感觉「好像长大了一点」的瞬间'),
'对我来说，是那天晚上在宿舍阳台一个人晾衣服，突然意识到很多事情不再有人催我去做了：按时吃饭、洗衣服、复习考试，都得靠自己安排。那一刻有点害怕，又有一点点小小的成就感。大家有没有类似的瞬间？',
7, 3, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '最近一次让你感觉「好像长大了一点」的瞬间'));

INSERT INTO `posts`
(`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `reward_coin_count`, `status`)
SELECT @b_chat, @u3, CONCAT(@mark, '如果有一天可以不用上课你会怎么安排'),
'假设明天突然放假一天，不用上课也没有作业，你会怎么安排？是睡到自然醒，还是早起去城里逛一圈？我可能会先在操场走两圈，然后去图书馆坐一上午，下午去校门外吃一顿平时舍不得点的火锅。',
5, 2, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '如果有一天可以不用上课你会怎么安排'));

-- ----------------------------------------------------------------------------- 建立一些关注关系
INSERT IGNORE INTO `user_follows` (`follower_user_id`, `followee_user_id`)
VALUES
(@u1, @u3),
(@u1, @u5),
(@u2, @u1),
(@u2, @u3),
(@u3, @u5),
(@u4, @u1),
(@u5, @u2);

-- ----------------------------------------------------------------------------- 评论（含少量对话）
-- 需要用到帖子 ID，先取出部分帖子 ID 变量
SET @p_study1 := (SELECT `id` FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '期末周自习室经验分享') LIMIT 1);
SET @p_used1  := (SELECT `id` FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '出一台九成新电纸书') LIMIT 1);
SET @p_used3  := (SELECT `id` FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '出一套大一高数教材和笔记本') LIMIT 1);
SET @p_chat1  := (SELECT `id` FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '最近一次让你感觉「好像长大了一点」的瞬间') LIMIT 1);
SET @p_chat2  := (SELECT `id` FROM `posts` WHERE BINARY `title` = BINARY CONCAT(@mark, '如果有一天可以不用上课你会怎么安排') LIMIT 1);

-- 学习贴下的一段对话
INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_study1, @u2, NULL, NULL,
'感觉你这个「固定座位」真的很重要，我之前老是换位置，结果每次都要重新适应环境。',
1, 0
FROM DUAL WHERE @p_study1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_study1 AND BINARY `content_text` = BINARY '感觉你这个「固定座位」真的很重要，我之前老是换位置，结果每次都要重新适应环境。'
);

SET @c_study1 := (SELECT `id` FROM `post_comments`
                  WHERE `post_id` = @p_study1 AND `parent_comment_id` IS NULL
                  ORDER BY `id` ASC LIMIT 1);

INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_study1, @u1, @c_study1, @u2,
'是的，而且固定一个角落之后，看到同一批人一起学习，也会更有仪式感。',
0, 0
FROM DUAL WHERE @p_study1 IS NOT NULL AND @c_study1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_study1 AND `parent_comment_id` = @c_study1 AND `user_id` = @u1
);

-- 电纸书帖子下的问价与回复
INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_used1, @u3, NULL, NULL,
'想问一下大概什么价位？支持夜间模式吗？',
0, 0
FROM DUAL WHERE @p_used1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_used1 AND `user_id` = @u3
);

SET @c_used1 := (SELECT `id` FROM `post_comments`
                 WHERE `post_id` = @p_used1 AND `user_id` = @u3
                 ORDER BY `id` ASC LIMIT 1);

INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_used1, @u1, @c_used1, @u3,
'价格私信可以商量，支持暖色夜间模式，长时间看眼睛还挺舒服的。',
0, 0
FROM DUAL WHERE @p_used1 IS NOT NULL AND @c_used1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_used1 AND `parent_comment_id` = @c_used1 AND `user_id` = @u1
);

-- 高数教材帖子下的感谢
INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_used3, @u2, NULL, NULL,
'已经私信联系啦，如果能一起看看你当时的笔记就更好了。',
0, 0
FROM DUAL WHERE @p_used3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_used3 AND `user_id` = @u2
);

-- 灌水贴「长大了一点」下面的互相分享
INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_chat1, @u3, NULL, NULL,
'我是在第一次自己去校医院看病的时候，挂号、排队、拿药全都一个人搞定，那天回宿舍突然觉得自己离「大人」更近了一点。',
2, 0
FROM DUAL WHERE @p_chat1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_chat1 AND `user_id` = @u3
);

INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_chat1, @u5, NULL, NULL,
'我是在社团里第一次主持活动，明明很紧张但全程没翻车，结束之后长舒一口气的那一刻。',
1, 0
FROM DUAL WHERE @p_chat1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_chat1 AND `user_id` = @u5
);

-- 灌水贴「不用上课」下面的小对话
INSERT INTO `post_comments`
(`post_id`, `user_id`, `parent_comment_id`, `reply_to_user_id`, `content_text`, `like_count`, `reward_coin_count`)
SELECT @p_chat2, @u4, NULL, NULL,
'我会先睡到自然醒，然后去操场打两个小时球，下午把一直想看的电影补上。',
0, 0
FROM DUAL WHERE @p_chat2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM `post_comments`
  WHERE `post_id` = @p_chat2 AND `user_id` = @u4
);

-- ----------------------------------------------------------------------------- 简单打赏示例（帖子与评论）
-- 取一条评论作为被打赏对象
SET @c_reward_post := (SELECT `id` FROM `post_comments`
                       WHERE `post_id` = @p_used3
                       ORDER BY `id` ASC LIMIT 1);

-- demo_c 打赏 demo_e 的「高数教材」帖子 3 硬币（打在帖子本身）
INSERT INTO `rewards`
(`sender_user_id`, `recipient_user_id`, `post_id`, `comment_id`, `coin_amount`)
SELECT @u3, @u5, @p_used3, NULL, 3
FROM DUAL
WHERE @p_used3 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `rewards`
    WHERE `sender_user_id` = @u3
      AND `recipient_user_id` = @u5
      AND `post_id` = @p_used3
      AND `coin_amount` = 3
  );

-- demo_b 打赏「期末周自习室经验分享」下的回复评论 2 硬币
INSERT INTO `rewards`
(`sender_user_id`, `recipient_user_id`, `post_id`, `comment_id`, `coin_amount`)
SELECT @u2, @u1, @p_study1, @c_study1, 2
FROM DUAL
WHERE @p_study1 IS NOT NULL AND @c_study1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `rewards`
    WHERE `sender_user_id` = @u2
      AND `recipient_user_id` = @u1
      AND `comment_id` = @c_study1
      AND `coin_amount` = 2
  );

-- 简单同步被打赏金额到帖子与评论的 reward_coin_count（仅针对本次种子数据）
UPDATE `posts` p
JOIN (
  SELECT `post_id`, SUM(`coin_amount`) AS c
  FROM `rewards`
  WHERE `post_id` IS NOT NULL
  GROUP BY `post_id`
) t ON p.`id` = t.`post_id`
SET p.`reward_coin_count` = t.c
WHERE BINARY p.`title` LIKE BINARY CONCAT(@mark, '%');

UPDATE `post_comments` c
JOIN (
  SELECT `comment_id`, SUM(`coin_amount`) AS c
  FROM `rewards`
  WHERE `comment_id` IS NOT NULL
  GROUP BY `comment_id`
) t ON c.`id` = t.`comment_id`
SET c.`reward_coin_count` = t.c;

-- 结束：板块 4，用户 5，帖子 12，带少量评论、关注与打赏

