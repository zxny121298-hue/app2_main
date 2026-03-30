export type ApiResponse<T> = {
  code: number
  message: string
  data: T | null
}

export type PageResult<T> = {
  total: number
  page: number
  pageSize: number
  list: T[]
}

export type SearchType = 'content' | 'user' | 'topic'

export type UnifiedSearchResponse = {
  type: SearchType
  posts?: PageResult<PostCardView>
  users?: PageResult<UserProfileView>
  topics?: PageResult<BoardView>
}

export type SearchHistoryView = {
  id: number
  keyword: string
  searchType: SearchType
  createdAt: string
}

export type AiChatMessageRole = 'user' | 'assistant'

export type AiChatMessagePayload = {
  role: AiChatMessageRole | 'system'
  content: string
}

export type AiChatReply = {
  reply: string
}

/** POST /api/ai/chat；后两项可选，省略时后端默认注入最近帖子摘要 */
export type AiChatRequestPayload = {
  messages: AiChatMessagePayload[]
  contextPostId?: number
  includeRecentPostSummaries?: boolean
}

export type UserRole = 'admin' | 'user'
export type UserStatus = 'active' | 'banned'
export type BoardStatus = 'enabled' | 'disabled'
export type PostStatus = 'normal' | 'deleted'

export type NotificationEventType =
  | 'private_message'
  | 'post_comment'
  | 'comment_reply'
  | 'post_like'
  | 'comment_like'
  | 'post_reward'
  | 'comment_reward'

export type NotificationTargetType = 'conversation' | 'post' | 'comment'

export type ExpChangeType =
  | 'sign_in'
  | 'create_post'
  | 'create_comment'
  | 'create_reply'
  | 'post_pinned'
  | 'post_featured'
  | 'admin_adjust'

export type CoinChangeType =
  | 'sign_in'
  | 'reward_send'
  | 'reward_receive'
  | 'admin_adjust'
  | 'system_grant'
  | 'system_deduct'
  | 'refund'

export type UserSimpleView = {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
  bio: string | null
  role: UserRole
  status: UserStatus
  level: number
}

export type UserProfileView = {
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

export type UpdateProfileRequest = {
  nickname?: string | null
  avatarUrl?: string | null
  bio?: string | null
}

export type ChangePasswordRequest = {
  oldPassword: string
  newPassword: string
}

export type CheckInView = {
  checkedInToday: boolean
  checkedAt: string | null
  expGain: number
  coinGain: number
  level: number | null
  totalExp: number | null
  currentLevelExp: number | null
}

export type ExpLogView = {
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

export type AdminBanRequest = {
  bannedUntilAt?: string | null
  reason?: string | null
}

export type AdminMuteRequest = {
  mutedUntilAt?: string | null
  reason?: string | null
}

export type AdminAdjustExpRequest = {
  changeExp: number
  remark?: string | null
}

export type AdminAdjustCoinRequest = {
  changeAmount: number
  remark?: string | null
}

export type RegisterRequest = {
  username: string
  password: string
  nickname?: string | null
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  tokenType: 'Bearer'
  accessToken: string
  expiresDays: number
  user: UserProfileView
}

export type BoardView = {
  id: number
  name: string
  description: string | null
  sortOrder: number
  status: BoardStatus
  createdAt: string
  updatedAt: string
}

export type UpsertBoardRequest = {
  name: string
  description?: string | null
  sortOrder: number
  status: BoardStatus
}

export type UpdateBoardStatusRequest = {
  status: BoardStatus
}

export type CreatePostRequest = {
  boardId: number
  title: string
  contentText?: string | null
  imageUrls?: string[] | null
}

export type TogglePinRequest = {
  pinned: boolean
}

export type ToggleFeaturedRequest = {
  featured: boolean
}

export type RewardRequest = {
  coinAmount: number
}

export type PostCardView = {
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

export type PostDetailView = {
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

export type CreateCommentRequest = {
  parentCommentId?: number | null
  replyToUserId?: number | null
  contentText?: string | null
  imageUrls?: string[] | null
}

export type CommentView = {
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

export type FollowRelationView = {
  user: UserSimpleView
  followedAt: string
  following: boolean
}

export type FollowStatusView = {
  targetUserId: number
  following: boolean
}

export type CreateConversationRequest = {
  targetUserId: number
}

export type SendMessageRequest = {
  contentText?: string | null
  imageUrls?: string[] | null
}

export type ConversationSettingRequest = {
  value: boolean
}

export type MessageView = {
  id: number
  conversationId: number
  sequenceNo: number
  senderUserId: number
  contentText: string | null
  imageUrls: string[]
  createdAt: string
}

export type ConversationListView = {
  id: number
  peerUser: UserSimpleView
  lastMessage: MessageView | null
  unreadCount: number
  pinned: boolean
  muted: boolean
  updatedAt: string | null
}

export type ConversationDetailView = {
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

export type NotificationGroupView = {
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

export type NotificationItemView = {
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

export type NotificationUnreadView = {
  unreadCount: number
}

export type CoinBalanceView = {
  coinBalance: number
}

export type CoinLedgerView = {
  id: number
  changeType: CoinChangeType
  changeAmount: number
  balanceAfter: number
  relatedUserId: number | null
  rewardId: number | null
  description: string | null
  createdAt: string
}
