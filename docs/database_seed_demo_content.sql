-- =============================================================================
-- 校园论坛演示数据：用户 / 板块 / 帖子 / 点赞 / 收藏 / 关注
-- =============================================================================
-- 使用前请备份数据库。可重复执行：已存在同名用户或板块时跳过对应插入。
--
-- 演示账号（10 个）登录名与明文密码：
--   demo_li / demo_wang / demo_zhang / demo_liu / demo_chen
--   demo_yang / demo_zhao / demo_wu / demo_zhou / demo_xu
--   密码均为：password
-- （password_hash 与 Spring Security BCryptPasswordEncoder 兼容，见 Spring 测试向量）
-- =============================================================================

USE `chat_forum`;

-- 字符串去重用 BINARY 比较，避免列排序规则（unicode_ci / 0900_ai_ci 等）与连接默认混用报错。
-- 若只复制片段执行，请先：SET NAMES utf8mb4;（或带 COLLATE，对 BINARY 比较无影响）
SET NAMES utf8mb4;

SET @pwd := '$2a$10$9N8N35BVs5TLqGL3pspAte5OWWA2a2aZIs.EGp7At7txYakFERMue';

-- ----------------------------------------------------------------------------- 板块（6）
INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '学习交流', '课程作业、考试复习与自习组队', 10, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '学习交流');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '校园生活', '食堂、宿舍、校园八卦与日常', 20, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '校园生活');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '二手集市', '教材、数码、生活用品转让', 30, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '二手集市');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '社团活动', '招新、路演与社团资讯', 40, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '社团活动');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '失物招领', '寻物与失物登记', 50, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '失物招领');

INSERT INTO `boards` (`name`, `description`, `sort_order`, `status`)
SELECT '休闲灌水', '轻松闲聊与水区', 60, 'enabled'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `boards` WHERE BINARY `name` = BINARY '休闲灌水');

SET @b_study := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '学习交流' LIMIT 1);
SET @b_life := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '校园生活' LIMIT 1);
SET @b_used := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '二手集市' LIMIT 1);
SET @b_club := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '社团活动' LIMIT 1);
SET @b_lost := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '失物招领' LIMIT 1);
SET @b_chat := (SELECT `id` FROM `boards` WHERE BINARY `name` = BINARY '休闲灌水' LIMIT 1);

-- ----------------------------------------------------------------------------- 用户（10）
INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_li', '李小园', NULL, '计科大三，正在啃操作系统。', @pwd, 'user', 'active', 120, 3, 220, 40
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_li');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_wang', '王大锤', NULL, '经管学院，喜欢跑步和咖啡。', @pwd, 'user', 'active', 80, 2, 90, 20
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_wang');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_zhang', '张小鱼', NULL, '文学院，写短评也写长帖。', @pwd, 'user', 'active', 200, 4, 400, 80
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_zhang');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_liu', '刘一帆', NULL, '机械系，实验室常驻选手。', @pwd, 'user', 'active', 60, 2, 70, 15
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_liu');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_chen', '陈慢慢', NULL, '心理系，慢热型回复。', @pwd, 'user', 'active', 150, 3, 180, 35
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_chen');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_yang', '杨晴天', NULL, '设计学院，板绘与海报。', @pwd, 'user', 'active', 90, 2, 100, 25
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_yang');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_zhao', '赵行舟', NULL, '法学院，辩论队后备。', @pwd, 'user', 'active', 110, 3, 200, 45
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_zhao');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_wu', '吴南北', NULL, '医学院，作息极其规律。', @pwd, 'user', 'active', 70, 2, 85, 18
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_wu');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_zhou', '周星星', NULL, '新传，拍短片剪 vlog。', @pwd, 'user', 'active', 130, 3, 250, 50
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_zhou');

INSERT INTO `users` (`username`, `nickname`, `avatar_url`, `bio`, `password_hash`, `role`, `status`, `coin_balance`, `level`, `total_exp`, `current_level_exp`)
SELECT 'demo_xu', '徐清风', NULL, '环境学院，徒步协会。', @pwd, 'user', 'active', 100, 2, 95, 22
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE BINARY `username` = BINARY 'demo_xu');

SET @u1 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_li' LIMIT 1);
SET @u2 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_wang' LIMIT 1);
SET @u3 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_zhang' LIMIT 1);
SET @u4 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_liu' LIMIT 1);
SET @u5 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_chen' LIMIT 1);
SET @u6 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_yang' LIMIT 1);
SET @u7 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_zhao' LIMIT 1);
SET @u8 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_wu' LIMIT 1);
SET @u9 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_zhou' LIMIT 1);
SET @u10 := (SELECT `id` FROM `users` WHERE BINARY `username` = BINARY 'demo_xu' LIMIT 1);

-- ----------------------------------------------------------------------------- 帖子标记：用标题前缀避免重复执行时叠帖
SET @mark := '[seed_demo] ';

-- 仅当尚无任何种子帖时批量插入（检查一条代表标题）
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u1, CONCAT( '操作系统调度算法期末整理'), CONCAT(
'本学期把进程调度几种经典策略又梳理了一遍，主要是 FCFS、SJF、时间片轮转和多级反馈队列。实际考试里常考对比题：要写出适用场景、饥饿问题、上下文切换开销等。',
REPEAT(' 我把自己整理的对比表贴在下面逻辑：交互式系统更偏向轮转，批处理可考虑 SJF 的变体。', 8)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '操作系统调度算法期末整理'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u2, CONCAT( '高数下册重积分复习路线'), CONCAT(
'建议按曲线积分、曲面积分、格林公式与高斯公式的顺序过一遍，每章挑三道综合题手写推导。',
REPEAT(' 重点是把方向与正负号写清楚，考场上最容易在这里丢分。', 25)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '高数下册重积分复习路线'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u3, CONCAT( '现代文学史名词解释背诵技巧'), '把每个流派按时间轴串起来，比孤立背词条效率高。每晚睡前过十条，一周能过完半本书。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '现代文学史名词解释背诵技巧'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u4, CONCAT( '材料力学实验报告注意事项'), CONCAT(
'实验报告里原始数据表格、误差分析和思考题三段缺一不可。曲线图坐标轴单位写清楚，老师会逐份看。',
REPEAT(' 附图尽量矢量导出，打印不糊。', 40)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '材料力学实验报告注意事项'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u5, CONCAT( '心理学统计 SPSS 入门踩坑'), CONCAT(
'变量视图与数据视图别混，标签值要在值里先定义。相关分析前记得做正态性检验，报告里写清楚显著性水平。',
REPEAT(' 作业数据集建议另存备份，避免覆盖原始文件。', 60)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '心理学统计 SPSS 入门踩坑'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u7, CONCAT( '法理学案例分析写作模板'), CONCAT(
'三段论：事实归纳、规范适用、结论。引用法条用中文全称加条款号，避免口语化表述。',
REPEAT(' 字数不够就展开构成要件逐条讨论。', 45)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '法理学案例分析写作模板'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u8, CONCAT( '生理学循环章节思维导图分享'), '体循环与肺循环画成闭环图，标注压力变化与瓣膜开闭时刻，记忆负担会小很多。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '生理学循环章节思维导图分享'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u9, CONCAT( '新闻采访与写作：同期声怎么用'), CONCAT(
'同期声要服务叙事，不宜堆长段。每段留气口，写稿时标注时间码方便剪辑同学对接。',
REPEAT(' 现场记笔记比全靠录音更稳。', 70)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '新闻采访与写作：同期声怎么用'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u10, CONCAT( '环境学概论：碳足迹小组作业分工'), '我们组按数据收集、模型简化、报告撰写和 PPT 四块拆分，每周固定对齐一次进度，避免最后熬夜。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '环境学概论：碳足迹小组作业分工'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u6, CONCAT( '设计色彩构成作业互评区'), CONCAT(
'本周课题是互补色对比，贴几张过程稿求拍砖。更关心节奏与面积比例，不玻璃心。',
REPEAT(' 附色环截图与灵感来源链接。', 35)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '设计色彩构成作业互评区'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_study, @u1, CONCAT( '数据结构二叉树遍历手写代码'), CONCAT(
'前中后序非递归版本用栈模拟，层序用队列。面试常让现场写 Morris 遍历，建议背熟边界条件。',
REPEAT(' 注释写清楚指针回溯点。', 55)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '数据结构二叉树遍历手写代码'));

-- 校园生活 11
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u2, CONCAT( '一食堂二楼新窗口测评（持续更新）'), CONCAT(
'本周试了麻辣香锅与牛肉面，出餐速度尚可，辣度默认偏温和。价格与外卖比仍有优势。',
REPEAT(' 欢迎补充避雷菜品。', 20)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '一食堂二楼新窗口测评（持续更新）'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u3, CONCAT( '宿舍限电 800W 实测哪些电器稳'), '小型吹风机低档可用，锅类基本会跳闸。别抱侥幸心理，处分记录会进档案。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '宿舍限电 800W 实测哪些电器稳'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u4, CONCAT( '图书馆四楼靠窗座位抢座攻略'), CONCAT(
'早八前二十分钟到基本有座，周三下午公休最卷。静音区别敲机械键盘，会被管理员提醒。',
REPEAT(' 预约系统偶尔卡顿，多刷新。', 30)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '图书馆四楼靠窗座位抢座攻略'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u5, CONCAT( '校医院报销流程 2026 版整理'), CONCAT(
'先挂号开转诊单，保留发票与费用清单原件。研究生与本科生窗口不同，别排错队。',
REPEAT(' 补充医疗保险材料多复印一份备用。', 28)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '校医院报销流程 2026 版整理'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u6, CONCAT( '操场夜跑组队，配速六分左右'), '每周二四六晚九点东门集合，热身两圈再开跑，新手可跟半程。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '操场夜跑组队，配速六分左右'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u7, CONCAT( '校园网断连问题汇总与自助排查'), CONCAT(
'先重启光猫与路由器，检查 DHCP 是否拿到地址。仍不行报修时附上宿舍号与断线时间点。',
REPEAT(' 有线比无线稳定，写论文建议插网线。', 50)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '校园网断连问题汇总与自助排查'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u8, CONCAT( '医学院期末月作息讨论（非养生版）'), '承认大家都会在考前压缩睡眠，但至少保证水分和简单拉伸，晕堂比挂科更难办。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '医学院期末月作息讨论（非养生版）'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u9, CONCAT( '毕业季微电影取景地征集'), CONCAT(
'想拍图书馆长廊与梧桐大道，需要同学协助清场五分钟。可署名片尾感谢，无酬优先换奶茶。',
REPEAT(' 设备自有稳定器与收音棒。', 15)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '毕业季微电影取景地征集'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u10, CONCAT( '后山步道落叶季拍照时间'), '下午四点到日落前色温最舒服，注意别踩草坪，保卫处会巡逻。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '后山步道落叶季拍照时间'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u1, CONCAT( '快递中心双十一排队实测'), CONCAT(
'中午十二点到一点人最少，傍晚下课高峰可能要排四十分钟。大件建议借小推车。',
REPEAT(' 取件码截图保存，防止手机没电。', 22)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '快递中心双十一排队实测'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_life, @u2, CONCAT( '洗衣房烘干机使用礼仪'), '烘完及时取走，别占着桶去吃饭。袜子与外套分开烘，味道真的会串。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '洗衣房烘干机使用礼仪'));

-- 二手集市 11
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u3, CONCAT( '出线性代数教材第九版，笔记较少'), '扉页有名字，内页少量划线，十五元南区宿舍楼下自提。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '出线性代数教材第九版，笔记较少'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u4, CONCAT( '机械制图板与丁字尺打包'), CONCAT(
'毕业清宿舍，功能正常，略有使用痕迹。不单拆，整套四十。可送到工科楼下。',
REPEAT(' 附购买记录截图。', 12)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '机械制图板与丁字尺打包'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u5, CONCAT( '收二手 iPad 用于看课件'), '预算一千五左右，屏幕无碎即可，电池健康别太低。走校内当面验机。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '收二手 iPad 用于看课件'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u6, CONCAT( '显示器 24 寸 1080p，毕业出'), CONCAT(
'戴尔入门款，无坏点，箱说全。二百八，仅自提。试机可带笔记本现场点亮。',
REPEAT(' 线材齐全。', 18)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '显示器 24 寸 1080p，毕业出'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u7, CONCAT( '小冰箱五十升，宿舍可用'), '一级能效，运行声不大。毕业转让，价格可议，需上门搬。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '小冰箱五十升，宿舍可用'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u8, CONCAT( '人体工学椅求推荐（预算五百内）'), CONCAT(
'腰托可调即可，滚轮要静音。有没有学长学姐出闲置也可以联系。',
REPEAT(' 久坐复习真的需要。', 24)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '人体工学椅求推荐（预算五百内）'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u9, CONCAT( '摄影三脚架轻装出行款'), '反折后四十厘米左右，承重够微单。二百转，送快装板备用螺丝。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '摄影三脚架轻装出行款'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u10, CONCAT( '露营蛋卷桌与两把克米特椅'), CONCAT(
'用过两次团建，无明显污渍。整套一百二，西区操场附近交易。',
REPEAT(' 可拆卖商议。', 10)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '露营蛋卷桌与两把克米特椅'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u1, CONCAT( '键盘红轴机械，换青轴故出'), '键帽可拔，已清灰。九十元，支持当面敲击试听。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '键盘红轴机械，换青轴故出'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u2, CONCAT( '台灯护眼款，可调色温'), CONCAT(
'国 AA 照度，底座稳。五十出。南区七号楼。',
REPEAT(' 原包装在。', 8)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '台灯护眼款，可调色温'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_used, @u3, CONCAT( '出考研英语黄皮书全套'), '只写过一两套真题，其余全新。价格私聊，可拆册。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '出考研英语黄皮书全套'));

-- 社团活动 10
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u4, CONCAT( '街舞社春季招新补录通知'), CONCAT(
'本周五晚活动中心 201 试课，穿宽松衣物与运动鞋即可。零基础友好。',
REPEAT(' 关注公众号获取音乐清单。', 14)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '街舞社春季招新补录通知'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u5, CONCAT( '辩论队模辩观摩开放日'), '周日晚上七点半，议题与赛制提前一天公布，欢迎旁听后提问交流。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '辩论队模辩观摩开放日'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u6, CONCAT( '动漫社漫展摊位志愿者招募'), CONCAT(
'需要布展与撤展帮手，可开志愿时长。周六全天，包午餐券。',
REPEAT(' 私聊备注可出勤时段。', 16)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '动漫社漫展摊位志愿者招募'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u7, CONCAT( '法律援助社团咨询日'), '下周三中午广场摊位，民刑行政分流接待，复杂问题登记后邮件回复。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '法律援助社团咨询日'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u8, CONCAT( '医学院科普志愿队进社区'), CONCAT(
'测血压血糖与基础健康宣教，需志愿者十名。车接车回，周六上午。',
REPEAT(' 穿白大褂由社团统一借。', 12)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '医学院科普志愿队进社区'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u9, CONCAT( '摄影协会外拍：老城区人文线'), CONCAT(
'周日上午集合地点东门公交站，自备存储卡与备用电池。雨天顺延。',
REPEAT(' 作品可参加学期展。', 20)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '摄影协会外拍：老城区人文线'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u10, CONCAT( '环保协会旧衣回收周'), '宿舍楼大厅设箱，干净干燥即可。捐赠去向会在公众号公示。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '环保协会旧衣回收周'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u1, CONCAT( '合唱团校庆联排时间调整'), CONCAT(
'因场地冲突，周三改到周四同时间段。声部负责人会单独通知。',
REPEAT(' 请假提前一天报备。', 8)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '合唱团校庆联排时间调整'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u2, CONCAT( '滑板社平地招新安全须知'), '必须佩戴护具，场地内礼让行人。新手区与进阶区分开，别逞强下碗池。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '滑板社平地招新安全须知'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_club, @u3, CONCAT( '书法社临帖交流：九成宫'), CONCAT(
'本周临摹欧体结构，带作品来互评。纸笔墨自备，现场有镇纸借用。',
REPEAT(' 零基础可从描红开始。', 18)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '书法社临帖交流：九成宫'));

-- 失物招领 10
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u5, CONCAT( '寻：黑色雨伞图书馆门口'), '长柄自动伞，伞面有小贴纸。上周五傍晚遗失，捡到请联系。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '寻：黑色雨伞图书馆门口'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u6, CONCAT( '拾：学生卡一张，姓周'), '已交一楼值班台，失主带证件领取。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '拾：学生卡一张，姓周'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u7, CONCAT( '寻：AirPods 充电盒单独丢失'), CONCAT(
'右耳耳机还在，盒子上刻了缩写 YQ。篮球场更衣室可能落下。',
REPEAT(' 有酬谢。', 6)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '寻：AirPods 充电盒单独丢失'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u8, CONCAT( '拾：实验报告册（无机化学）'), '封面写了班级与学号后三位，交到理学院门卫。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '拾：实验报告册（无机化学）'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u9, CONCAT( '寻：相机镜头盖 67mm'), '外拍结束发现丢了，可能在校车倒数第二排。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '寻：相机镜头盖 67mm'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u10, CONCAT( '拾：保温杯米色带贴纸'), CONCAT(
'食堂到宿舍沿路捡到，无异味已清洗。认领请描述贴纸图案。',
REPEAT(' 三天后无人认领捐义卖。', 10)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '拾：保温杯米色带贴纸'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u1, CONCAT( '寻：有线耳机 Type-C'), '白色扁线，昨晚自习室离开忘拿。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '寻：有线耳机 Type-C'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u2, CONCAT( '拾：宿舍钥匙一串两把'), '挂绳蓝色，交到宿管阿姨处登记。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '拾：宿舍钥匙一串两把'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u3, CONCAT( '寻：红色帆布包内有课本'), CONCAT(
'高数与 C 语言两本，包侧有小徽章。可能在报告厅遗失。',
REPEAT(' 捡到请务必联系，谢谢。', 14)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '寻：红色帆布包内有课本'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_lost, @u4, CONCAT( '拾：校园卡背面贴了卡通'), '交到失物招领中心窗口，请失主核实卡号后四位。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '拾：校园卡背面贴了卡通'));

-- 休闲灌水 10
INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u5, CONCAT( '今天食堂阿姨多给了一勺菜'), '心情变好，特此记录。希望大家都能遇到手不抖的窗口。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '今天食堂阿姨多给了一勺菜'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u6, CONCAT( '猫咪学长今天又在晒太阳'), CONCAT(
'长椅旁边那只橘白，拍照请别开闪光灯。投喂别喂人类零食。',
REPEAT(' 毕业照想带它入镜的提前想好构图。', 20)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '猫咪学长今天又在晒太阳'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u7, CONCAT( '推荐三部适合下饭的老剧'), '节奏不快、对白有趣，适合一边吃一边看，不耽误夹菜。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '推荐三部适合下饭的老剧'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u8, CONCAT( '熬夜冠军争霸赛（玩笑贴）'), CONCAT(
'三点还没睡的来报个到，明天没课的再举手。健康提醒：偶尔可以，别天天。',
REPEAT(' 拒绝内卷到身体。', 25)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '熬夜冠军争霸赛（玩笑贴）'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u9, CONCAT( '你最喜欢校园哪个角落发呆'), '我先说：旧教学楼天台，风大但视野好。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '你最喜欢校园哪个角落发呆'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u10, CONCAT( '雨天听什么歌单'), CONCAT(
'轻爵士与钢琴独奏更配雨声，电子太吵容易心烦。',
REPEAT(' 欢迎反向安利摇滚。', 15)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '雨天听什么歌单'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u1, CONCAT( '如果给你多一天假期你会干嘛'), '我会补觉，然后整理硬盘里半年没动的照片。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '如果给你多一天假期你会干嘛'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u2, CONCAT( '吐槽：自动售货机又吞钱了'), CONCAT(
'已打服务电话登记，同学说换一台机器更稳。记录单号防扯皮。',
REPEAT(' 尽量用扫码支付留痕。', 12)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '吐槽：自动售货机又吞钱了'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u3, CONCAT( '许愿池：这学期不挂科'), '心诚则灵，评论区接力。', 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '许愿池：这学期不挂科'));

INSERT INTO `posts` (`board_id`, `user_id`, `title`, `content_text`, `like_count`, `comment_count`, `status`)
SELECT @b_chat, @u4, CONCAT( '分享一张今天拍到的晚霞'), CONCAT(
'手机直出无滤镜，西边云层裂开一条金边。看到的人这周顺利。',
REPEAT(' 图在评论区脑补（文字贴）。', 10)
), 0, 0, 'normal'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `posts` WHERE BINARY `title` = BINARY CONCAT( '分享一张今天拍到的晚霞'));

-- ----------------------------------------------------------------------------- 点赞（去重：同一用户同一帖仅一条）
INSERT IGNORE INTO `post_likes` (`post_id`, `user_id`)
SELECT p.`id`, u.`uid`
FROM `posts` p
JOIN (
  SELECT @u1 AS uid UNION SELECT @u2 UNION SELECT @u3 UNION SELECT @u4 UNION SELECT @u5
  UNION SELECT @u6 UNION SELECT @u7 UNION SELECT @u8 UNION SELECT @u9 UNION SELECT @u10
) u
WHERE BINARY p.`title` LIKE BINARY CONCAT( '%')
  AND p.`id` MOD 10 = (u.uid MOD 10)
  AND p.`user_id` <> u.uid;

INSERT IGNORE INTO `post_likes` (`post_id`, `user_id`)
SELECT p.`id`, u.`uid`
FROM `posts` p
JOIN (
  SELECT @u1 AS uid UNION SELECT @u2 UNION SELECT @u3 UNION SELECT @u4
) u
WHERE BINARY p.`title` LIKE BINARY CONCAT( '%')
  AND p.`id` MOD 7 = (u.uid MOD 7)
  AND p.`user_id` <> u.uid;

-- ----------------------------------------------------------------------------- 收藏
INSERT IGNORE INTO `post_favorites` (`post_id`, `user_id`)
SELECT p.`id`, u.`uid`
FROM `posts` p
JOIN (
  SELECT @u2 AS uid UNION SELECT @u4 UNION SELECT @u6 UNION SELECT @u8 UNION SELECT @u10
) u
WHERE BINARY p.`title` LIKE BINARY CONCAT( '%')
  AND p.`id` MOD 6 = (u.uid MOD 6)
  AND p.`user_id` <> u.uid;

INSERT IGNORE INTO `post_favorites` (`post_id`, `user_id`)
SELECT p.`id`, @u1
FROM `posts` p
WHERE BINARY p.`title` LIKE BINARY CONCAT( '%')
  AND p.`id` MOD 5 = 0
  AND p.`user_id` <> @u1;

-- ----------------------------------------------------------------------------- 关注（互关少量、单向若干）
INSERT IGNORE INTO `user_follows` (`follower_user_id`, `followee_user_id`)
VALUES
(@u1, @u2), (@u1, @u3), (@u1, @u5),
(@u2, @u1), (@u2, @u4),
(@u3, @u1), (@u3, @u6), (@u3, @u9),
(@u4, @u5), (@u4, @u7),
(@u5, @u2), (@u5, @u8),
(@u6, @u1), (@u6, @u10),
(@u7, @u3), (@u7, @u8),
(@u8, @u4), (@u8, @u9),
(@u9, @u2), (@u9, @u10),
(@u10, @u1), (@u10, @u5);

-- ----------------------------------------------------------------------------- 同步帖子点赞数（与 post_likes 一致）
UPDATE `posts` p
LEFT JOIN (
  SELECT `post_id`, COUNT(*) AS c FROM `post_likes` GROUP BY `post_id`
) t ON p.`id` = t.`post_id`
SET p.`like_count` = IFNULL(t.c, 0)
WHERE BINARY p.`title` LIKE BINARY CONCAT( '%');

-- 结束：共 63 帖（学习11 + 生活11 + 二手11 + 社团10 + 失物10 + 灌水10），用户10，板块6
