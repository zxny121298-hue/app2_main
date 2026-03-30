# 项目全量文档

## 1. 项目概览

这是一个“论坛 + 社交 + 私信 + 通知 + 硬币/经验 + 管理后台”的全栈项目。

- 前端目录：`frontend`
- 后端目录：`backend`
- 数据库脚本：`database.sql`

核心业务能力如下：

- 用户注册、登录、个人资料维护
- 论坛板块管理
- 发帖、评论、点赞、收藏、打赏
- 关注 / 粉丝
- 一对一私信
- 聚合通知
- 每日签到、经验等级、硬币流水
- 管理员封禁、禁言、调整经验和硬币、帖子置顶/加精/删除

## 2. 技术栈与运行方式

### 2.1 前端

- Nuxt 4
- Vue 3 + TypeScript
- `@nuxt/ui`
- Tailwind CSS 4
- Zod 表单校验

前端关键配置：

- `frontend/nuxt.config.ts`
  - `runtimeConfig.backendBase` 默认指向 `http://127.0.0.1:8080/api`
  - 浏览器只调用 `/api/*`
  - 真实转发由 `frontend/server/api/[...path].ts` 完成

### 2.2 后端

- Spring Boot 4
- Spring Web
- Spring Security
- Spring Validation
- MySQL
- 数据访问层基于 `NamedParameterJdbcTemplate`

注意：

- 当前代码虽然引入了 `spring-boot-starter-data-jpa` 和 `mybatis-plus-spring-boot4-starter`，但实际主数据访问方式是自定义 Repository + JDBC，不是 JPA Entity / MyBatis Mapper。

### 2.3 数据库

- MySQL
- 脚本文件：`database.sql`
- `spring.sql.init.mode=never`，后端不会自动建表，必须先手动执行 `database.sql`

### 2.4 启动要求

- 先导入 `database.sql`
- 再启动后端 `backend`
- 再启动前端 `frontend`

## 3. 整体请求链路

项目的标准请求路径如下：

1. 页面或组件调用 `useApi().request('/api/...')`
2. `useApi` 自动附带 `Authorization: Bearer <token>`
3. Nuxt 服务端代理 `frontend/server/api/[...path].ts` 将请求转发到后端 `runtimeConfig.backendBase`
4. 后端 Controller -> Service -> Repository
5. 后端统一返回 `ApiResponse<T>`
6. 前端 `useApi` 解包：
   - `code === 0` 视为成功
   - `code !== 0` 抛出 `ApiError`

这意味着：

- 前端不直接访问 `http://127.0.0.1:8080`
- 前端统一通过 `/api/*` 访问
- 业务错误主要通过响应体里的 `code/message` 传递，而不是依赖 HTTP 401/403

## 4. 统一接口契约

### 4.1 统一响应格式

后端统一响应：

```ts
type ApiResponse<T> = {
  code: number
  message: string
  data: T | null
}
```

成功返回：

- `code = 0`
- `message = 'ok'`

分页结构：

```ts
type PageResult<T> = {
  total: number
  page: number
  pageSize: number
  list: T[]
}
```

### 4.2 错误码

后端定义在 `backend/src/main/java/com/example/demo/common/ErrorCodes.java`：

- `4000` 参数错误
- `4001` 未登录 / token 无效
- `4003` 无权限
- `4004` 资源不存在
- `4009` 冲突
- `5000` 服务端异常

### 4.3 鉴权实现

认证链路如下：

- 登录/注册成功后，后端返回 JWT 风格 token
- 前端 `useAuth.ts` 将 token 和用户信息写入 Cookie
  - `forum-access-token`
  - `forum-user`
- `useApi.ts` 读取 token 并附加 `Authorization`
- 后端 `AuthTokenFilter` 解析 token，并把用户放进 `SecurityContext`

需要特别注意：

- `SecurityConfig` 里是 `auth.anyRequest().permitAll()`
- 真正的登录态/管理员校验是在 Service 层通过 `UserAccessService` 完成
- 所以接口权限是“业务层校验”，不是“路由层拦截”

### 4.4 当前认证的真实实现

当前项目并没有使用 `better-auth` 作为实际认证方案。  
前端认证的真实实现是：

- 前端 Cookie 持久化
- 后端自定义 HMAC-SHA256 token
- Service 层鉴权

## 5. 前端结构

### 5.1 目录结构

```text
frontend/
├─ nuxt.config.ts                # Nuxt 配置，声明 backendBase
├─ package.json                  # 前端依赖与脚本
├─ server/
│  └─ api/
│     └─ [...path].ts            # /api/* 统一代理到后端
└─ app/
   ├─ app.vue                    # UApp / NuxtLayout / NuxtPage 根入口
   ├─ app.config.ts              # Nuxt UI 主题色
   ├─ assets/css/main.css        # 全局样式
   ├─ layouts/
   │  ├─ default.vue             # 主站布局、导航、板块切换、未读轮询
   │  └─ auth.vue                # 登录/注册页面布局
   ├─ middleware/
   │  ├─ auth.ts                 # 需要登录
   │  └─ admin.ts                # 需要管理员
   ├─ composables/
   │  ├─ useApi.ts               # 统一请求封装
   │  ├─ useAuth.ts              # 登录态管理
   │  └─ useForumMeta.ts         # 板块列表、通知未读数
   ├─ types/
   │  └─ forum.ts                # 前端接口类型总定义
   ├─ utils/
   │  └─ forum.ts                # 展示辅助、标签映射、字符串处理
   ├─ components/
   │  ├─ PostComposerModal.vue   # 发帖弹窗
   │  ├─ CommentThread.vue       # 评论树
   │  ├─ EmptyState.vue          # 空状态
   │  ├─ AppLogo.vue             # 预留组件，当前未实际使用
   │  └─ TemplateMenu.vue        # 预留组件，当前未实际使用
   └─ pages/
      ├─ index.vue               # 首页/帖子列表
      ├─ login.vue               # 登录
      ├─ register.vue            # 注册
      ├─ profile.vue             # 我的资料
      ├─ admin.vue               # 板块管理后台
      ├─ messages.vue            # 私信
      ├─ notifications.vue       # 通知中心
      ├─ posts/[id].vue          # 帖子详情
      └─ users/[id].vue          # 用户主页
```

### 5.2 前端核心模块

#### `useApi.ts`

职责：

- 包装 `$fetch`
- 自动附带 Bearer Token
- 自动识别 `ApiResponse<T>`
- 将业务错误转换为 `ApiError`
- 当 `code = 4001` 时自动清空登录态

#### `useAuth.ts`

职责：

- 从 Cookie 恢复登录状态
- 管理 `token` 与 `profile`
- 提供 `applyLogin / setProfile / clear / logout`

特点：

- 不使用 Pinia
- 通过 Nuxt `useState` + `useCookie` 管理状态

#### `useForumMeta.ts`

职责：

- 全局缓存板块列表
- 统一拉取通知未读数

默认布局中会：

- 首次加载板块
- 用户登录后刷新资料和未读数
- 每 30 秒轮询一次未读数

### 5.3 页面职责

| 路由 | 页面文件 | 权限 | 主要功能 |
|---|---|---|---|
| `/` | `frontend/app/pages/index.vue` | 公开 | 板块筛选、帖子分页、点赞/收藏、打开发帖弹窗 |
| `/login` | `frontend/app/pages/login.vue` | 公开 | 登录，支持 `redirect` 回跳 |
| `/register` | `frontend/app/pages/register.vue` | 公开 | 注册并自动登录 |
| `/profile` | `frontend/app/pages/profile.vue` | 登录 | 个人资料、修改密码、签到、我的帖子、收藏、关注、粉丝、经验、硬币 |
| `/admin` | `frontend/app/pages/admin.vue` | 管理员 | 板块创建、编辑、启用/停用 |
| `/messages` | `frontend/app/pages/messages.vue` | 登录 | 会话列表、会话详情、发消息、置顶/免打扰/删除 |
| `/notifications` | `frontend/app/pages/notifications.vue` | 登录 | 通知分组、通知明细、已读操作 |
| `/posts/:id` | `frontend/app/pages/posts/[id].vue` | 公开 | 帖子详情、评论树、点赞、收藏、打赏、评论、管理员操作 |
| `/users/:id` | `frontend/app/pages/users/[id].vue` | 公开 | 用户主页、用户帖子、关注/取关、发起私信、管理员处罚/调整 |

### 5.4 重点页面与实际接口映射

#### 首页 `index.vue`

调用接口：

- `GET /api/boards`
- `GET /api/posts`
- `POST /api/posts`
- `POST/DELETE /api/posts/{postId}/like`
- `POST/DELETE /api/posts/{postId}/favorite`

页面行为：

- 通过查询参数 `boardId` 过滤板块
- 通过查询参数 `compose=1` 打开发帖弹窗

#### 个人中心 `profile.vue`

标签页：

- 总览
- 我的帖子
- 收藏帖子
- 收藏评论
- 我的关注
- 我的粉丝
- 经验记录
- 硬币流水

调用接口：

- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/me/password`
- `POST /api/check-ins`
- `GET /api/check-ins/today`
- `GET /api/coins/balance`
- `GET /api/posts/mine`
- `GET /api/favorites/posts`
- `GET /api/favorites/comments`
- `GET /api/follows`
- `GET /api/fans`
- `GET /api/users/me/exp-logs`
- `GET /api/coins/ledgers`

补充说明：
- 资料页改密需要输入当前密码和新密码。
- 改密成功后前端会清空本地登录态并跳回登录页。
- 后端通过 `users.token_version` 让旧 token 在改密后全部失效。

#### 用户主页 `users/[id].vue`

调用接口：

- `GET /api/users/{userId}`
- `GET /api/users/{userId}/posts`
- `GET /api/users/{userId}/followed`
- `POST/DELETE /api/users/{userId}/follow`
- `POST /api/conversations`
- 管理员接口：
  - `POST /api/admin/users/{userId}/ban`
  - `POST /api/admin/users/{userId}/unban`
  - `POST /api/admin/users/{userId}/mute`
  - `POST /api/admin/users/{userId}/unmute`
  - `POST /api/admin/users/{userId}/exp-adjust`
  - `POST /api/admin/users/{userId}/coin-adjust`

说明：

- 管理功能不在 `/admin` 页面里，而是放在用户主页中

#### 私信页 `messages.vue`

调用接口：

- `GET /api/conversations`
- `GET /api/conversations/{conversationId}`
- `POST /api/conversations`
- `POST /api/conversations/{conversationId}/messages`
- `POST /api/conversations/{conversationId}/read`
- `PUT /api/conversations/{conversationId}/pin`
- `PUT /api/conversations/{conversationId}/mute`
- `DELETE /api/conversations/{conversationId}`

#### 通知页 `notifications.vue`

调用接口：

- `GET /api/notifications/groups`
- `GET /api/notifications/groups/{groupId}/items`
- `POST /api/notifications/items/{itemId}/read`
- `POST /api/notifications/groups/{groupId}/read`
- `POST /api/notifications/read-all`
- `GET /api/notifications/unread-count`

#### 帖子详情页 `posts/[id].vue`

调用接口：

- `GET /api/posts/{postId}`
- `GET /api/posts/{postId}/comments`
- `POST /api/posts/{postId}/comments`
- `POST/DELETE /api/posts/{postId}/like`
- `POST/DELETE /api/posts/{postId}/favorite`
- `POST /api/posts/{postId}/reward`
- 管理员接口：
  - `PUT /api/admin/posts/{postId}/pin`
  - `PUT /api/admin/posts/{postId}/feature`
  - `DELETE /api/admin/posts/{postId}`

### 5.5 组件职责

| 组件 | 作用 |
|---|---|
| `PostComposerModal.vue` | 发布帖子，输入标题、正文、图片 URL 列表 |
| `CommentThread.vue` | 递归评论树，支持点赞/收藏/打赏/回复 |
| `EmptyState.vue` | 空状态提示 |
| `AppLogo.vue` | 预留展示组件，当前未见被页面引用 |
| `TemplateMenu.vue` | 预留组件，当前未见被页面引用 |

### 5.6 前端数据模型

前端所有接口类型统一定义在：

- `frontend/app/types/forum.ts`

它是前端接口字段的“单一事实来源”，包含：

- 通用响应：`ApiResponse`、`PageResult`
- 用户：`UserSimpleView`、`UserProfileView`
- 认证：`LoginRequest`、`RegisterRequest`、`LoginResponse`
- 板块：`BoardView`
- 帖子：`PostCardView`、`PostDetailView`
- 评论：`CommentView`
- 关注：`FollowRelationView`、`FollowStatusView`
- 私信：`ConversationListView`、`ConversationDetailView`、`MessageView`
- 通知：`NotificationGroupView`、`NotificationItemView`
- 硬币：`CoinBalanceView`、`CoinLedgerView`

## 6. 后端结构

### 6.1 目录结构

```text
backend/src/main/java/com/example/demo/
├─ common/        # 通用响应、异常、分页、断言、校验
├─ config/        # 安全配置、Jackson 配置、业务配置
├─ controller/    # REST 接口层
├─ dto/           # 请求/响应 DTO
├─ model/         # 领域枚举与 Repository 记录模型
├─ repository/    # JDBC 数据访问层
├─ security/      # Token 解析、Principal、Security 工具
├─ service/       # 业务服务层
└─ DemoApplication.java
```

### 6.2 分层职责

#### Controller

职责：

- 声明 REST 路由
- 参数绑定与基础校验
- 调用 Service
- 统一包装成 `ApiResponse`

#### Service

职责：

- 业务规则控制
- 登录态与权限判断
- 事务控制
- 拼装多 Repository 查询
- 驱动通知、经验、硬币等跨域逻辑

#### Repository

职责：

- 直接写 SQL
- 基于 `NamedParameterJdbcTemplate`
- 返回轻量 record 模型

说明：

- 没有 JPA Entity
- 没有 MyBatis XML Mapper
- 主要通过 `BaseRepository` 提供公共能力

### 6.3 公共核心类

| 文件 | 作用 |
|---|---|
| `common/ApiResponse.java` | 统一响应包装 |
| `common/PageResult.java` | 统一分页返回 |
| `common/BusinessException.java` | 业务异常 |
| `common/GlobalExceptionHandler.java` | 统一异常转响应 |
| `common/BizAssert.java` | 业务断言 |
| `common/RequestValidator.java` | 文本/图片至少一项必填校验 |
| `service/ViewMapper.java` | Repository 行对象 -> DTO View |
| `service/UserAccessService.java` | 当前用户、管理员、封禁/禁言状态归一化 |

### 6.4 认证与权限

#### Token

`security/TokenService.java` 负责：

- 生成 token
- 解析 token
- HMAC-SHA256 签名
- payload 包含：
  - `userId`
  - `username`
  - `role`
  - `exp`

#### 过滤器

`security/AuthTokenFilter.java` 负责：

- 从 `Authorization: Bearer ...` 中取 token
- 解析 token
- 把用户写入 `SecurityContext`

#### 权限控制的真实位置

权限并不是通过 `HttpSecurity` 路由规则完成，而是在 Service 层通过以下方法完成：

- `requireCurrentUser()`
- `requireAdmin()`
- `assertCanLogin()`
- `assertCanOperate()`
- `assertCanSpeak()`

业务含义：

- 被封禁用户不能登录、不能操作
- 被禁言用户不能发帖、评论、私信
- 管理员专有接口由 `requireAdmin()` 约束

### 6.5 配置项

配置文件：`backend/src/main/resources/application.yml`

关键配置：

- 服务端口：`8080`
- 数据库默认地址：`jdbc:mysql://localhost:3306/chat_forum`
- 默认数据库账号：`root / 1234`
- 登录 token 有效期：`forum.auth.expires-days = 30`
- 经验值配置：
  - 签到 `5`
  - 发帖 `10`
  - 评论 `5`
  - 回复 `5`
  - 帖子置顶 `20`
  - 帖子加精 `50`

## 7. 后端接口清单

说明：

- “权限”按 Service 的真实校验结果描述
- “公开(可带登录态)”表示未登录也可调用，但登录后会返回更完整的个性化字段，如 `liked/favorited/following`

### 7.1 认证

| 方法 | 路径 | 权限 | 请求体 | 返回 |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | 公开 | `AuthDtos.RegisterRequest` | `AuthDtos.LoginResponse` |
| `POST` | `/api/auth/login` | 公开 | `AuthDtos.LoginRequest` | `AuthDtos.LoginResponse` |

### 7.2 用户与管理员

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `GET` | `/api/users/me` | 登录 | 无 | `UserProfileView` |
| `GET` | `/api/users/{userId}` | 公开 | `userId` | `UserProfileView` |
| `PUT` | `/api/users/me` | 登录，可操作 | `UpdateProfileRequest` | `UserProfileView` |
| `PUT` | `/api/users/me/password` | 登录 | `ChangePasswordRequest` | `void` |
| `POST` | `/api/check-ins` | 登录，可操作 | 无 | `CheckInView` |
| `GET` | `/api/check-ins/today` | 登录 | 无 | `CheckInView` |
| `GET` | `/api/users/me/exp-logs` | 登录 | `page` `pageSize` | `PageResult<ExpLogView>` |
| `POST` | `/api/admin/users/{userId}/ban` | 管理员 | `AdminBanRequest` | `void` |
| `POST` | `/api/admin/users/{userId}/unban` | 管理员 | 无 | `void` |
| `POST` | `/api/admin/users/{userId}/mute` | 管理员 | `AdminMuteRequest` | `void` |
| `POST` | `/api/admin/users/{userId}/unmute` | 管理员 | 无 | `void` |
| `POST` | `/api/admin/users/{userId}/exp-adjust` | 管理员 | `AdminAdjustExpRequest` | `UserProfileView` |
| `POST` | `/api/admin/users/{userId}/coin-adjust` | 管理员 | `AdminAdjustCoinRequest` | `UserProfileView` |

### 7.3 板块

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `GET` | `/api/boards` | 公开；`includeDisabled=true` 需管理员 | `includeDisabled` | `BoardView[]` |
| `POST` | `/api/admin/boards` | 管理员 | `UpsertBoardRequest` | `BoardView` |
| `PUT` | `/api/admin/boards/{boardId}` | 管理员 | `UpsertBoardRequest` | `BoardView` |
| `PUT` | `/api/admin/boards/{boardId}/status` | 管理员 | `UpdateBoardStatusRequest` | `void` |

### 7.4 帖子

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `POST` | `/api/posts` | 登录，可发言 | `CreatePostRequest` | `PostDetailView` |
| `GET` | `/api/posts` | 公开(可带登录态) | `boardId` `page` `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/posts/{postId}` | 公开(可带登录态) | `postId` | `PostDetailView` |
| `GET` | `/api/posts/mine` | 登录 | `page` `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/users/{userId}/posts` | 公开(可带登录态) | `userId` `page` `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/favorites/posts` | 登录 | `page` `pageSize` | `PageResult<PostCardView>` |
| `DELETE` | `/api/admin/posts/{postId}` | 管理员 | `postId` | `void` |
| `PUT` | `/api/admin/posts/{postId}/pin` | 管理员 | `TogglePinRequest` | `void` |
| `PUT` | `/api/admin/posts/{postId}/feature` | 管理员 | `ToggleFeaturedRequest` | `void` |
| `POST` | `/api/posts/{postId}/like` | 登录，可操作 | `postId` | `void` |
| `DELETE` | `/api/posts/{postId}/like` | 登录 | `postId` | `void` |
| `POST` | `/api/posts/{postId}/favorite` | 登录 | `postId` | `void` |
| `DELETE` | `/api/posts/{postId}/favorite` | 登录 | `postId` | `void` |
| `POST` | `/api/posts/{postId}/reward` | 登录，可操作 | `RewardRequest` | `void` |

### 7.5 评论

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `POST` | `/api/posts/{postId}/comments` | 登录，可发言 | `CreateCommentRequest` | `CommentView` |
| `GET` | `/api/posts/{postId}/comments` | 公开(可带登录态) | `postId` `page` `pageSize` | `PageResult<CommentView>` |
| `GET` | `/api/favorites/comments` | 登录 | `page` `pageSize` | `PageResult<CommentView>` |
| `POST` | `/api/comments/{commentId}/like` | 登录，可操作 | `commentId` | `void` |
| `DELETE` | `/api/comments/{commentId}/like` | 登录 | `commentId` | `void` |
| `POST` | `/api/comments/{commentId}/favorite` | 登录 | `commentId` | `void` |
| `DELETE` | `/api/comments/{commentId}/favorite` | 登录 | `commentId` | `void` |
| `POST` | `/api/comments/{commentId}/reward` | 登录，可操作 | `RewardRequest` | `void` |

### 7.6 关注关系

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `POST` | `/api/users/{userId}/follow` | 登录，可操作 | `userId` | `void` |
| `DELETE` | `/api/users/{userId}/follow` | 登录 | `userId` | `void` |
| `GET` | `/api/follows` | 登录 | `page` `pageSize` | `PageResult<FollowRelationView>` |
| `GET` | `/api/fans` | 登录 | `page` `pageSize` | `PageResult<FollowRelationView>` |
| `GET` | `/api/users/{userId}/followed` | 登录 | `userId` | `FollowStatusView` |

### 7.7 私信

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `POST` | `/api/conversations` | 登录，可操作 | `CreateConversationRequest` | `ConversationDetailView` |
| `POST` | `/api/conversations/{conversationId}/messages` | 登录，可发言 | `SendMessageRequest` | `MessageView` |
| `GET` | `/api/conversations` | 登录 | `page` `pageSize` | `PageResult<ConversationListView>` |
| `GET` | `/api/conversations/{conversationId}` | 登录 | `page` `pageSize` | `ConversationDetailView` |
| `POST` | `/api/conversations/{conversationId}/read` | 登录 | `conversationId` | `void` |
| `PUT` | `/api/conversations/{conversationId}/pin` | 登录 | `ConversationSettingRequest` | `void` |
| `PUT` | `/api/conversations/{conversationId}/mute` | 登录 | `ConversationSettingRequest` | `void` |
| `DELETE` | `/api/conversations/{conversationId}` | 登录 | `conversationId` | `void` |

### 7.8 通知

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `GET` | `/api/notifications/groups` | 登录 | `page` `pageSize` | `PageResult<NotificationGroupView>` |
| `GET` | `/api/notifications/groups/{groupId}/items` | 登录 | `groupId` `page` `pageSize` | `PageResult<NotificationItemView>` |
| `POST` | `/api/notifications/items/{itemId}/read` | 登录 | `itemId` | `void` |
| `POST` | `/api/notifications/groups/{groupId}/read` | 登录 | `groupId` | `void` |
| `POST` | `/api/notifications/read-all` | 登录 | 无 | `void` |
| `GET` | `/api/notifications/unread-count` | 登录 | 无 | `NotificationUnreadView` |

### 7.9 硬币

| 方法 | 路径 | 权限 | 请求体 / 参数 | 返回 |
|---|---|---|---|---|
| `GET` | `/api/coins/balance` | 登录 | 无 | `CoinBalanceView` |
| `GET` | `/api/coins/ledgers` | 登录 | `page` `pageSize` | `PageResult<CoinLedgerView>` |

## 8. 核心业务规则

### 8.1 文本与图片

以下内容都要求“文本或图片至少有一项”：

- 帖子
- 评论 / 回复
- 私信消息

图片字段都不是文件上传，而是 `imageUrls: string[]`。  
当前项目仅保存图片 URL，不包含上传服务。

### 8.2 封禁与禁言

- 封禁用户不能登录，也不能执行操作
- 禁言用户不能发帖、评论、私信
- 到期的封禁/禁言会在 `UserAccessService.normalize()` 中自动清理

### 8.3 私信

- 当前只支持一对一会话
- `conversations` 里固定存储 `user1_id < user2_id`
- 某用户“删除会话”只是软删除自己的成员记录，不是全局删除
- 如果重新发起同一个会话，会恢复该用户的成员状态

### 8.4 通知

通知按 `(recipient_user_id, event_type, target_type, target_id)` 聚合到 `notification_groups`：

- 私信消息
- 帖子评论
- 评论回复
- 帖子点赞
- 评论点赞
- 帖子打赏
- 评论打赏

### 8.5 管理规则

- 板块停用后不能发帖
- 帖子删除是软删除
- 帖子置顶/加精首次发生时会奖励经验

## 9. 数据库结构

### 9.1 表总览

当前数据库共 22 张主表，外加 2 条补充外键语句和 1 组等级初始化数据。

#### 用户与等级

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `users` | 用户主表 | `username` `password_hash` `role` `status` `coin_balance` `level` | 被几乎所有业务表引用 |
| `level_rules` | 等级规则 | `level` `level_name` `upgrade_need_exp` | 经验升级计算使用 |
| `check_ins` | 每日签到 | `user_id` `check_in_date` `exp_gain` | 关联 `users` |
| `user_exp_logs` | 经验流水 | `user_id` `change_type` `change_exp` `level_after` | 可关联帖子、评论、操作员 |

#### 社交关系

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `user_follows` | 关注关系 | `follower_user_id` `followee_user_id` | 双外键都指向 `users` |

#### 板块与帖子

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `boards` | 论坛板块 | `name` `sort_order` `status` | 被 `posts` 引用 |
| `posts` | 帖子主表 | `board_id` `user_id` `title` `status` `is_pinned` `is_featured` | 关联板块、作者、管理员操作人 |
| `post_images` | 帖子图片 | `post_id` `image_url` `sort_order` | 关联 `posts` |
| `post_likes` | 帖子点赞 | `post_id` `user_id` | 关联 `posts` `users` |
| `post_favorites` | 帖子收藏 | `post_id` `user_id` | 关联 `posts` `users` |

#### 评论

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `post_comments` | 评论主表 | `post_id` `user_id` `parent_comment_id` `reply_to_user_id` | 支持树状评论 |
| `comment_images` | 评论图片 | `comment_id` `image_url` `sort_order` | 关联 `post_comments` |
| `comment_likes` | 评论点赞 | `comment_id` `user_id` | 关联评论和用户 |
| `comment_favorites` | 评论收藏 | `comment_id` `user_id` | 关联评论和用户 |

#### 私信

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `conversations` | 会话主表 | `user1_id` `user2_id` `last_message_id` `last_message_at` | 一对一会话 |
| `conversation_members` | 会话成员状态 | `conversation_id` `user_id` `last_read_message_id` `unread_count` `is_pinned` `is_muted` `is_deleted` | 每个用户各自的会话状态 |
| `messages` | 私信消息 | `conversation_id` `sender_user_id` `sequence_no` `content_text` | 关联会话 |
| `message_images` | 私信图片 | `message_id` `image_url` `sort_order` | 关联消息 |

#### 打赏、通知、硬币

| 表名 | 作用 | 关键字段 | 主要关系 |
|---|---|---|---|
| `rewards` | 打赏记录 | `sender_user_id` `recipient_user_id` `post_id` `comment_id` `coin_amount` | 关联用户/帖子/评论 |
| `notification_groups` | 通知分组 | `recipient_user_id` `event_type` `target_type` `target_id` `unread_count` | 通知聚合 |
| `notification_items` | 通知明细 | `group_id` `actor_user_id` `conversation_id` `message_id` `post_id` `comment_id` `reward_id` | 通知明细项 |
| `coin_ledgers` | 硬币流水 | `user_id` `change_type` `change_amount` `balance_after` | 可关联用户和打赏记录 |

### 9.2 关键表关系说明

#### 用户侧

- `users` 是核心主表
- `user_follows`、`posts`、`post_comments`、`conversations`、`messages`、`rewards`、`notification_*`、`coin_ledgers`、`check_ins`、`user_exp_logs` 都依赖 `users`

#### 帖子侧

- 一个 `board` 有多个 `posts`
- 一个 `post` 有多个图片、评论、点赞、收藏、打赏

#### 评论侧

- `post_comments.parent_comment_id` 指向自己，实现评论树
- `reply_to_user_id` 记录被回复用户

#### 私信侧

- `conversations` 记录会话
- `conversation_members` 记录每个成员自己的未读、置顶、免打扰、软删除状态
- `messages` 记录消息
- `message_images` 记录消息图片

#### 通知侧

- `notification_groups` 是聚合层
- `notification_items` 是明细层
- 组内未读数由业务逻辑维护

### 9.3 关键约束与索引

主要唯一约束：

- `users.username`
- `level_rules.level`
- `user_follows(follower_user_id, followee_user_id)`
- `boards.name`
- `conversations(user1_id, user2_id)`
- `conversation_members(conversation_id, user_id)`
- `messages(conversation_id, sequence_no)`
- `post_likes(post_id, user_id)`
- `comment_likes(comment_id, user_id)`
- `post_favorites(post_id, user_id)`
- `comment_favorites(comment_id, user_id)`
- `check_ins(user_id, check_in_date)`
- `notification_groups(recipient_user_id, event_type, target_type, target_id)`

主要特点：

- 点赞、收藏、关注都通过唯一索引保证幂等
- 会话通过固定用户对唯一化，避免重复私聊线程
- 消息通过 `sequence_no` 保证会话内严格递增

### 9.4 枚举字段

数据库中的关键枚举与后端 `ForumEnums.java` 保持一致：

- 用户角色：`admin` `user`
- 用户状态：`active` `banned`
- 板块状态：`enabled` `disabled`
- 帖子状态：`normal` `deleted`
- 通知事件：
  - `private_message`
  - `post_comment`
  - `comment_reply`
  - `post_like`
  - `comment_like`
  - `post_reward`
  - `comment_reward`
- 通知目标：
  - `conversation`
  - `post`
  - `comment`
- 硬币流水：
  - `reward_send`
  - `reward_receive`
  - `admin_adjust`
  - `system_grant`
  - `system_deduct`
  - `refund`
- 经验流水：
  - `sign_in`
  - `create_post`
  - `create_comment`
  - `create_reply`
  - `post_pinned`
  - `post_featured`
  - `admin_adjust`

### 9.5 初始化数据

`database.sql` 最后会初始化 `level_rules`：

- Lv1 -> 升级到 Lv2 需 100 exp
- Lv2 -> 升级到 Lv3 需 200 exp
- Lv3 -> 升级到 Lv4 需 400 exp
- Lv4 -> 升级到 Lv5 需 800 exp
- Lv5 -> 升级到 Lv6 需 1600 exp
- Lv6 为顶级

### 9.6 建表顺序与循环外键

由于 `conversations.last_message_id` 和 `conversation_members.last_read_message_id` 依赖 `messages`，脚本采用：

1. 先创建 `conversations`
2. 再创建 `conversation_members`
3. 再创建 `messages`
4. 最后通过 `ALTER TABLE` 补充这两个外键

## 10. 前后端对齐结论

当前项目已经形成比较清晰的前后端契约：

- 前端类型定义：`frontend/app/types/forum.ts`
- 后端返回 DTO：`backend/src/main/java/com/example/demo/dto/*`
- 数据库表结构：`database.sql`

对齐方式如下：

- 字段命名统一走 camelCase DTO，而数据库仍保持 snake_case
- `ViewMapper` 负责 Repository 行对象 -> DTO
- 前端页面不直接依赖数据库字段，只依赖 DTO 契约
- 前端页面通过 `useApi` 统一处理错误

仍然值得注意的实现特征：

- 图片全部是 URL 字符串，不存在上传 API
- 鉴权在 Service 层，不在路由配置层
- 当前没有单独的“后台管理首页聚合运营数据”模块，`/admin` 仅管理板块
- 用户处罚和经验/硬币调整入口在用户主页

## 11. 建议的阅读顺序

如果要继续开发，推荐按以下顺序阅读：

1. `frontend/app/types/forum.ts`
2. `frontend/app/composables/useApi.ts`
3. `frontend/server/api/[...path].ts`
4. `backend/src/main/java/com/example/demo/controller/*`
5. `backend/src/main/java/com/example/demo/service/*`
6. `backend/src/main/java/com/example/demo/repository/*`
7. `database.sql`

这样可以最快建立“页面 -> 接口 -> 业务 -> SQL -> 表结构”的完整认知链路。
