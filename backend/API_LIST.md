# Chat Forum 后端接口说明（给前端 / AI）

这份文档基于当前后端实现整理，目标是帮助前端或前端 AI 直接理解接口、权限、返回结构、分页排序和当前实现细节。

## 1. 全局约定

| 项目 | 说明 |
| --- | --- |
| 服务端口 | 默认 `8080` |
| 接口前缀 | `/api` |
| 鉴权方式 | `Authorization: Bearer <accessToken>` |
| Token 来源 | 注册和登录接口返回 |
| Token 有效期 | 默认 `30` 天 |
| 时间格式 | ISO-8601 字符串 |
| 应用时区 | `Asia/Shanghai` |
| 上传能力 | 没有文件上传接口，图片字段只能传 URL |
| 会话状态 | 无状态 JWT，没有 refresh token 和 logout 接口 |
| 分页参数 | `page` 默认 `1`，`pageSize` 默认 `20`，页码从 `1` 开始 |

统一响应结构：

```ts
type ApiResponse<T> = {
  code: number
  message: string
  data: T | null
}

type PageResult<T> = {
  total: number
  page: number
  pageSize: number
  list: T[]
}
```

示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

```json
{
  "code": 4001,
  "message": "请先登录",
  "data": null
}
```

错误码：

| code | 含义 |
| --- | --- |
| `0` | 成功 |
| `4000` | 参数错误或通用业务错误 |
| `4001` | 未登录或 token 无效 |
| `4003` | 无权限 |
| `4004` | 资源不存在 |
| `4009` | 资源冲突 |
| `5000` | 服务端异常 |

权限术语：

| 术语 | 含义 |
| --- | --- |
| `public` | 公开接口 |
| `login` | 需要登录 |
| `operate` | 需要登录且用户未被封禁 |
| `speak` | 需要登录且用户未被封禁、未被禁言 |
| `admin` | 需要管理员 |

实现说明：当前异常处理主要通过响应体里的 `code` 和 `message` 表达，前端不要只看 HTTP 状态码。公开读取接口如果带了合法 token，会返回当前用户维度的 `liked` / `favorited`；如果没带 token 或 token 无效，会按匿名访问处理，不会报错。被禁言用户仍然可以登录、点赞、收藏、关注、打赏和查看内容，但不能发帖、评论、发私信。

## 2. 枚举与固定值

```ts
type UserRole = "admin" | "user"
type UserStatus = "active" | "banned"
type BoardStatus = "enabled" | "disabled"
type PostStatus = "normal" | "deleted"

type NotificationEventType =
  | "private_message"
  | "post_comment"
  | "comment_reply"
  | "post_like"
  | "comment_like"
  | "post_reward"
  | "comment_reward"

type NotificationTargetType = "conversation" | "post" | "comment"

type ExpChangeType =
  | "sign_in"
  | "create_post"
  | "create_comment"
  | "create_reply"
  | "post_pinned"
  | "post_featured"
  | "admin_adjust"

type CoinChangeType =
  | "sign_in"
  | "reward_send"
  | "reward_receive"
  | "admin_adjust"
  | "system_grant"
  | "system_deduct"
  | "refund"
```

经验值配置：签到 `5`，发帖 `10`，一级评论 `5`，回复评论 `5`，首次置顶 `20`，首次加精 `50`。
硬币奖励配置：每日签到 `5`。

## 3. DTO 速查

### 3.1 用户

```ts
type UserSimpleView = {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
  bio: string | null
  role: UserRole
  status: UserStatus
  level: number
}

type UserProfileView = {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
  bio: string | null
  role: UserRole
  status: UserStatus
  bannedUntilAt: string | null
  banReason: string | null
  mutedUntilAt: string | null
  muteReason: string | null
  coinBalance: number
  level: number
  totalExp: number
  currentLevelExp: number
  createdAt: string
  lastLoginAt: string | null
}

type UpdateProfileRequest = {
  nickname?: string | null
  avatarUrl?: string | null
  bio?: string | null
}

type ChangePasswordRequest = {
  oldPassword: string
  newPassword: string
}

type CheckInView = {
  checkedInToday: boolean
  checkedAt: string | null
  expGain: number
  coinGain: number
  level: number | null
  totalExp: number | null
  currentLevelExp: number | null
}

type ExpLogView = {
  id: number
  changeType: ExpChangeType
  changeExp: number
  totalExpAfter: number
  levelAfter: number
  postId: number | null
  commentId: number | null
  operatorUserId: number | null
  remark: string | null
  createdAt: string
}

type AdminBanRequest = {
  bannedUntilAt?: string | null
  reason?: string | null
}

type AdminMuteRequest = {
  mutedUntilAt?: string | null
  reason?: string | null
}

type AdminAdjustExpRequest = {
  changeExp: number
  remark?: string | null
}

type AdminAdjustCoinRequest = {
  changeAmount: number
  remark?: string | null
}
```

约束说明：`nickname` 最长 `50`；`avatarUrl` 最长 `255`；`bio` 最长 `500`；管理员 ban / mute / adjust 的 `remark` 或 `reason` 最长 `255`；`changeExp` 和 `changeAmount` 都不能为 `0`。

### 3.2 认证

```ts
type RegisterRequest = {
  username: string
  password: string
  nickname?: string | null
}

type LoginRequest = {
  username: string
  password: string
}

type LoginResponse = {
  tokenType: "Bearer"
  accessToken: string
  expiresDays: number
  user: UserProfileView
}
```

约束说明：`username` 长度 `3-50`；`password` 长度 `6-64`；`nickname` 最长 `50`。

### 3.3 板块

```ts
type BoardView = {
  id: number
  name: string
  description: string | null
  sortOrder: number
  status: BoardStatus
  createdAt: string
  updatedAt: string
}

type UpsertBoardRequest = {
  name: string
  description?: string | null
  sortOrder: number
  status: BoardStatus
}

type UpdateBoardStatusRequest = {
  status: BoardStatus
}
```

约束说明：`name` 最长 `100`；`description` 最长 `255`；`sortOrder <= 999999`。

### 3.4 帖子

```ts
type CreatePostRequest = {
  boardId: number
  title: string
  contentText?: string | null
  imageUrls?: string[] | null
}

type TogglePinRequest = {
  pinned: boolean
}

type ToggleFeaturedRequest = {
  featured: boolean
}

type RewardRequest = {
  coinAmount: number
}

type PostCardView = {
  id: number
  boardId: number
  boardName: string
  author: UserSimpleView
  title: string
  contentText: string | null
  imageUrls: string[]
  likeCount: number
  commentCount: number
  rewardCoinCount: number
  pinned: boolean
  featured: boolean
  status: PostStatus
  liked: boolean
  favorited: boolean
  createdAt: string
}

type PostDetailView = {
  id: number
  boardId: number
  boardName: string
  author: UserSimpleView
  title: string
  contentText: string | null
  imageUrls: string[]
  likeCount: number
  commentCount: number
  rewardCoinCount: number
  pinned: boolean
  pinnedAt: string | null
  pinnedByUserId: number | null
  featured: boolean
  featuredAt: string | null
  featuredByUserId: number | null
  status: PostStatus
  liked: boolean
  favorited: boolean
  createdAt: string
  updatedAt: string
}
```

约束说明：`title` 必填且最长 `200`；`imageUrls` 每项最长 `255`；`contentText` 和 `imageUrls` 不能同时为空；`coinAmount >= 1`。

### 3.5 评论

```ts
type CreateCommentRequest = {
  parentCommentId?: number | null
  replyToUserId?: number | null
  contentText?: string | null
  imageUrls?: string[] | null
}

type CommentView = {
  id: number
  postId: number
  parentCommentId: number | null
  replyToUserId: number | null
  author: UserSimpleView
  replyToUser: UserSimpleView | null
  contentText: string | null
  imageUrls: string[]
  likeCount: number
  rewardCoinCount: number
  liked: boolean
  favorited: boolean
  createdAt: string
  updatedAt: string
  children: CommentView[]
}
```

约束说明：`imageUrls` 每项最长 `255`；`contentText` 和 `imageUrls` 不能同时为空。

### 3.6 关注

```ts
type FollowRelationView = {
  user: UserSimpleView
  followedAt: string
  following: boolean
}

type FollowStatusView = {
  targetUserId: number
  following: boolean
}
```

### 3.7 私信

```ts
type CreateConversationRequest = {
  targetUserId: number
}

type SendMessageRequest = {
  contentText?: string | null
  imageUrls?: string[] | null
}

type ConversationSettingRequest = {
  value: boolean
}

type MessageView = {
  id: number
  conversationId: number
  sequenceNo: number
  senderUserId: number
  contentText: string | null
  imageUrls: string[]
  createdAt: string
}

type ConversationListView = {
  id: number
  peerUser: UserSimpleView
  lastMessage: MessageView | null
  unreadCount: number
  pinned: boolean
  muted: boolean
  updatedAt: string | null
}

type ConversationDetailView = {
  id: number
  peerUser: UserSimpleView
  unreadCount: number
  pinned: boolean
  muted: boolean
  lastReadMessageId: number | null
  lastReadSequenceNo: number | null
  lastReadAt: string | null
  messages: MessageView[]
}
```

约束说明：`SendMessageRequest.contentText` 和 `imageUrls` 不能同时为空；`imageUrls` 每项最长 `255`。

### 3.8 通知

```ts
type NotificationGroupView = {
  id: number
  eventType: NotificationEventType
  targetType: NotificationTargetType
  targetId: number
  totalCount: number
  unreadCount: number
  latestActor: UserSimpleView | null
  latestAt: string
  lastReadAt: string | null
}

type NotificationItemView = {
  id: number
  groupId: number
  eventType: NotificationEventType
  targetType: NotificationTargetType
  targetId: number
  actor: UserSimpleView
  conversationId: number | null
  messageId: number | null
  postId: number | null
  commentId: number | null
  rewardId: number | null
  read: boolean
  readAt: string | null
  createdAt: string
}

type NotificationUnreadView = {
  unreadCount: number
}
```

### 3.9 硬币

```ts
type CoinBalanceView = {
  coinBalance: number
}

type CoinLedgerView = {
  id: number
  changeType: CoinChangeType
  changeAmount: number
  balanceAfter: number
  relatedUserId: number | null
  rewardId: number | null
  description: string | null
  createdAt: string
}
```

## 4. 接口清单

### 4.1 认证

| Method | Path | Auth | Request | Returns |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | `public` | `RegisterRequest` | `LoginResponse` |
| `POST` | `/api/auth/login` | `public` | `LoginRequest` | `LoginResponse` |

实现说明：注册时 `username` 会被 `trim()`；如果没传 `nickname` 或只传空白，后端会把昵称设成 `username`；注册成功后直接返回 token；新用户默认 `role = "user"`、`status = "active"`、`coinBalance = 0`、`level = 1`；登录时会拒绝已被封禁的用户。

### 4.2 用户、签到、经验、管理员用户操作

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `GET` | `/api/users/me` | `login` | 无 | `UserProfileView` |
| `GET` | `/api/users/{userId}` | `public` | 无 | `UserProfileView` |
| `PUT` | `/api/users/me` | `operate` | `UpdateProfileRequest` | `UserProfileView` |
| `PUT` | `/api/users/me/password` | `login` | `ChangePasswordRequest` | `Void` |

Password change notes:
- `oldPassword` and `newPassword` both use the existing `6-64` password length rule.
- The endpoint only requires login. A banned-but-still-logged-in user can still change password.
- Successful password change increments `users.token_version`, which invalidates all previously issued tokens.
- Older tokens without `tokenVersion` are treated as version `0` for backward compatibility.
| `POST` | `/api/check-ins` | `operate` | 无 | `CheckInView` |
| `GET` | `/api/check-ins/today` | `login` | 无 | `CheckInView` |
| `GET` | `/api/users/me/exp-logs` | `login` | `page`, `pageSize` | `PageResult<ExpLogView>` |
| `POST` | `/api/admin/users/{userId}/ban` | `admin` | `AdminBanRequest` | `Void` |
| `POST` | `/api/admin/users/{userId}/unban` | `admin` | 无 | `Void` |
| `POST` | `/api/admin/users/{userId}/mute` | `admin` | `AdminMuteRequest` | `Void` |
| `POST` | `/api/admin/users/{userId}/unmute` | `admin` | 无 | `Void` |
| `POST` | `/api/admin/users/{userId}/exp-adjust` | `admin` | `AdminAdjustExpRequest` | `UserProfileView` |
| `POST` | `/api/admin/users/{userId}/coin-adjust` | `admin` | `AdminAdjustCoinRequest` | `UserProfileView` |

实现补充：签到成功会同时奖励 `5` 经验和 `5` 硬币；`CheckInView.coinGain` 返回本次或今日签到对应的硬币奖励，未签到时为 `0`。
实现说明：`GET /api/users/{userId}` 是公开接口，且当前实现会返回完整 `UserProfileView`；公开用户资料里也包含 `coinBalance`、封禁信息和禁言信息；更新资料时空白字符串会被转成 `null`；签到一天只能成功一次；经验日志按 `createdAt desc, id desc` 排序；`ban` 接口里 `bannedUntilAt = null` 表示永久封禁；`mute` 接口里只有未来时间才会形成真正的禁言效果；管理员调整经验后总经验不会低于 `0`；管理员调整硬币后余额不会低于 `0`。

### 4.3 板块

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `GET` | `/api/boards` | `public` | `includeDisabled?: boolean` | `BoardView[]` |
| `POST` | `/api/admin/boards` | `admin` | `UpsertBoardRequest` | `BoardView` |
| `PUT` | `/api/admin/boards/{boardId}` | `admin` | `UpsertBoardRequest` | `BoardView` |
| `PUT` | `/api/admin/boards/{boardId}/status` | `admin` | `UpdateBoardStatusRequest` | `Void` |

实现说明：默认只返回 `enabled` 板块；`includeDisabled=true` 只有管理员能用；板块排序是 `sortOrder asc, id asc`；板块名必须唯一。

### 4.4 帖子

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `POST` | `/api/posts` | `speak` | `CreatePostRequest` | `PostDetailView` |
| `GET` | `/api/posts` | `public` | `boardId?`, `page`, `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/posts/{postId}` | `public` | 无 | `PostDetailView` |
| `GET` | `/api/posts/mine` | `login` | `page`, `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/users/{userId}/posts` | `public` | `page`, `pageSize` | `PageResult<PostCardView>` |
| `GET` | `/api/favorites/posts` | `login` | `page`, `pageSize` | `PageResult<PostCardView>` |
| `DELETE` | `/api/admin/posts/{postId}` | `admin` | 无 | `Void` |
| `PUT` | `/api/admin/posts/{postId}/pin` | `admin` | `TogglePinRequest` | `Void` |
| `PUT` | `/api/admin/posts/{postId}/feature` | `admin` | `ToggleFeaturedRequest` | `Void` |
| `POST` | `/api/posts/{postId}/like` | `operate` | 无 | `Void` |
| `DELETE` | `/api/posts/{postId}/like` | `login` | 无 | `Void` |
| `POST` | `/api/posts/{postId}/favorite` | `login` | 无 | `Void` |
| `DELETE` | `/api/posts/{postId}/favorite` | `login` | 无 | `Void` |
| `POST` | `/api/posts/{postId}/reward` | `operate` | `RewardRequest` | `Void` |

实现说明：发帖时 `title` 必填；`contentText` 和 `imageUrls` 不能同时为空；`boardId` 必须存在且板块状态必须是 `enabled`；帖子列表只返回 `status = "normal"` 的帖子；帖子列表排序是 `pinned desc, featured desc, createdAt desc, id desc`；收藏帖子列表按收藏时间倒序；公开读取帖子接口在匿名访问时 `liked` 和 `favorited` 恒为 `false`；删除帖子是软删除；取消点赞和取消收藏是幂等的；重复点赞或重复收藏会返回 `4009`；打赏不能打给自己且余额必须足够；首次置顶和首次加精会给帖子作者发经验，同一帖子只发一次。

### 4.5 评论

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `POST` | `/api/posts/{postId}/comments` | `speak` | `CreateCommentRequest` | `CommentView` |
| `GET` | `/api/posts/{postId}/comments` | `public` | `page`, `pageSize` | `PageResult<CommentView>` |
| `GET` | `/api/favorites/comments` | `login` | `page`, `pageSize` | `PageResult<CommentView>` |
| `POST` | `/api/comments/{commentId}/like` | `operate` | 无 | `Void` |
| `DELETE` | `/api/comments/{commentId}/like` | `login` | 无 | `Void` |
| `POST` | `/api/comments/{commentId}/favorite` | `login` | 无 | `Void` |
| `DELETE` | `/api/comments/{commentId}/favorite` | `login` | 无 | `Void` |
| `POST` | `/api/comments/{commentId}/reward` | `operate` | `RewardRequest` | `Void` |

实现说明：评论和回复要求 `contentText` 与 `imageUrls` 至少有一个有内容；一级评论通常传 `parentCommentId = null`、`replyToUserId = null`；回复评论时如果没传 `replyToUserId`，后端会自动使用父评论作者；评论树先按 `createdAt asc, id asc` 取出，再在服务层组装；`page` / `pageSize` 只作用于根评论；`total` 只统计根评论数量；每个根评论会连带返回完整 `children` 子树，因此单页返回体积可能较大；收藏评论列表是扁平结构，`children` 为空数组；取消点赞和取消收藏是幂等的。

### 4.6 关注

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `POST` | `/api/users/{userId}/follow` | `operate` | 无 | `Void` |
| `DELETE` | `/api/users/{userId}/follow` | `login` | 无 | `Void` |
| `GET` | `/api/follows` | `login` | `page`, `pageSize` | `PageResult<FollowRelationView>` |
| `GET` | `/api/fans` | `login` | `page`, `pageSize` | `PageResult<FollowRelationView>` |
| `GET` | `/api/users/{userId}/followed` | `login` | 无 | `FollowStatusView` |

实现说明：不能关注自己；重复关注会返回 `4009`；取消关注是幂等的；我的关注列表按关注时间倒序；我的粉丝列表按粉丝关注我的时间倒序；`/api/fans` 返回里的 `following` 表示我是否回关这个粉丝。

### 4.7 私信 / 会话

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `POST` | `/api/conversations` | `operate` | `CreateConversationRequest` | `ConversationDetailView` |
| `POST` | `/api/conversations/{conversationId}/messages` | `speak` | `SendMessageRequest` | `MessageView` |
| `GET` | `/api/conversations` | `login` | `page`, `pageSize` | `PageResult<ConversationListView>` |
| `GET` | `/api/conversations/{conversationId}` | `login` | `page`, `pageSize` | `ConversationDetailView` |
| `POST` | `/api/conversations/{conversationId}/read` | `login` | 无 | `Void` |
| `PUT` | `/api/conversations/{conversationId}/pin` | `login` | `ConversationSettingRequest` | `Void` |
| `PUT` | `/api/conversations/{conversationId}/mute` | `login` | `ConversationSettingRequest` | `Void` |
| `DELETE` | `/api/conversations/{conversationId}` | `login` | 无 | `Void` |

实现说明：当前只支持一对一会话；`POST /api/conversations` 如果双方已有会话，会直接复用旧会话；发消息要求 `contentText` 和 `imageUrls` 至少有一个有内容；会话列表排序是 `pinned desc, lastMessageAt desc, id desc`；仓库层消息分页按 `sequenceNo desc`，所以 `page=1` 是最新一页；Service 在返回前会把当前页消息反转成升序；前端拿到的 `messages` 仍然是从旧到新，但它只代表当前页切片；拉取会话详情不会自动已读，需要前端显式调用 `/read`；删除会话只是当前用户自己的软删除，不是全局删除；新消息会把双方的会话成员记录恢复为未删除状态；`ConversationListView.lastMessage` 可能为 `null`。

### 4.8 通知

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `GET` | `/api/notifications/groups` | `login` | `page`, `pageSize` | `PageResult<NotificationGroupView>` |
| `GET` | `/api/notifications/groups/{groupId}/items` | `login` | `page`, `pageSize` | `PageResult<NotificationItemView>` |
| `POST` | `/api/notifications/items/{itemId}/read` | `login` | 无 | `Void` |
| `POST` | `/api/notifications/groups/{groupId}/read` | `login` | 无 | `Void` |
| `POST` | `/api/notifications/read-all` | `login` | 无 | `Void` |
| `GET` | `/api/notifications/unread-count` | `login` | 无 | `NotificationUnreadView` |

实现说明：通知 group 是按 `(recipientUserId, eventType, targetType, targetId)` 聚合的；同一帖子上的多次点赞会进入同一个 group；同一评论上的多次回复会进入同一个 group；同一会话的私信通知会进入同一个 group；通知组列表按 `latestAt desc, id desc` 排序；通知项列表按 `createdAt desc, id desc` 排序；`NotificationGroupView.latestActor` 只是最近一次触发该组通知的人；`NotificationUnreadView.unreadCount` 是所有 group 未读数之和。

### 4.9 硬币

| Method | Path | Auth | Request / Query | Returns |
| --- | --- | --- | --- | --- |
| `GET` | `/api/coins/balance` | `login` | 无 | `CoinBalanceView` |
| `GET` | `/api/coins/ledgers` | `login` | `page`, `pageSize` | `PageResult<CoinLedgerView>` |
| `POST` | `/api/admin/users/{userId}/coin-adjust` | `admin` | `AdminAdjustCoinRequest` | `UserProfileView` |

实现补充：每日签到成功会新增一条 `changeType = "sign_in"` 的硬币流水。
实现说明：硬币流水按 `createdAt desc, id desc` 排序；打赏帖子或评论时，后端会同时修改双方余额；打赏会生成 `rewards` 记录；打赏会生成两条 `coin_ledgers`；打赏会更新帖子或评论的 `rewardCoinCount`；打赏会给被打赏人发通知。

## 5. 列表排序与分页速查

| 场景 | 规则 |
| --- | --- |
| 板块列表 | `sortOrder asc, id asc` |
| 帖子列表 | `pinned desc, featured desc, createdAt desc, id desc` |
| 收藏帖子 | 收藏时间倒序 |
| 评论树 | 根评论按 `createdAt asc, id asc` |
| 收藏评论 | 收藏时间倒序 |
| 我的关注 | 关注时间倒序 |
| 我的粉丝 | 粉丝关注我的时间倒序 |
| 会话列表 | `pinned desc, lastMessageAt desc, id desc` |
| 会话消息查询 | 查询按 `sequenceNo desc`，返回前翻成升序 |
| 通知组 | `latestAt desc, id desc` |
| 通知项 | `createdAt desc, id desc` |
| 经验日志 | `createdAt desc, id desc` |
| 硬币流水 | `createdAt desc, id desc` |

## 6. 前端接入建议

实现建议：所有请求都按 `ApiResponse<T>` 解析；所有业务失败都优先读取 `code` / `message`；公共读取接口在登录态下建议带 token，这样能得到正确的 `liked` / `favorited`；发帖、评论、私信发送前，前端应自己先校验“文本和图片不能同时为空”；图片和头像都只是 URL，没有上传接口；会话页面如果要实现“打开即已读”，拿到详情后要主动调用 `/api/conversations/{conversationId}/read`；当前没有 WebSocket / SSE 推送，消息和通知需要轮询或主动刷新。

## 7. 当前实现中的坑点

实现坑点：`GET /api/users/{userId}` 是公开接口，但会返回完整 `UserProfileView`，包括硬币余额和封禁 / 禁言信息；业务异常通常不会映射成标准 HTTP 4xx/5xx，前端必须看响应体里的 `code`；`POST /api/conversations` 在“会话已存在但当前用户此前软删除过它”的场景下，当前实现可能无法顺利重新打开旧会话；`POST /api/admin/users/{userId}/mute` 如果传 `mutedUntilAt = null`，当前实现不会形成真正的禁言效果；评论分页是“根评论分页 + 子树整包返回”，不是扁平评论流。
