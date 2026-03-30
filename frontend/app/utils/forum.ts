import type {
  BoardStatus,
  CoinChangeType,
  ExpChangeType,
  NotificationEventType,
  NotificationItemView,
  UserSimpleView,
  UserStatus
} from '~/types/forum'

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

const numberFormatter = new Intl.NumberFormat('zh-CN')

export const boardStatusLabels: Record<BoardStatus, string> = {
  enabled: '启用中',
  disabled: '已停用'
}

export const userStatusLabels: Record<UserStatus, string> = {
  active: '正常',
  banned: '已封禁'
}

export const expChangeLabels: Record<ExpChangeType, string> = {
  sign_in: '签到',
  create_post: '发布帖子',
  create_comment: '发表评论',
  create_reply: '回复评论',
  post_pinned: '帖子置顶',
  post_featured: '帖子加精',
  admin_adjust: '管理员调整'
}

export const coinChangeLabels: Record<CoinChangeType, string> = {
  sign_in: '签到奖励',
  reward_send: '打赏支出',
  reward_receive: '收到打赏',
  admin_adjust: '管理员调整',
  system_grant: '系统发放',
  system_deduct: '系统扣除',
  refund: '退款'
}

export const notificationEventLabels: Record<NotificationEventType, string> = {
  private_message: '私信',
  post_comment: '帖子评论',
  comment_reply: '评论回复',
  post_like: '帖子点赞',
  comment_like: '评论点赞',
  post_reward: '帖子打赏',
  comment_reward: '评论打赏'
}

export function displayName(user?: Pick<UserSimpleView, 'nickname' | 'username'> | null) {
  if (!user) {
    return '未知用户'
  }

  return user.nickname?.trim() || user.username
}

export function initialsOf(user?: Pick<UserSimpleView, 'nickname' | 'username'> | null) {
  return displayName(user).slice(0, 1).toUpperCase()
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return '未记录'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return dateTimeFormatter.format(date)
}

export function formatRelativeTime(value?: string | null) {
  if (!value) {
    return '刚刚'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  const diff = date.getTime() - Date.now()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (Math.abs(diff) < minute) {
    return '刚刚'
  }

  if (Math.abs(diff) < hour) {
    return `${Math.round(diff / minute)} 分钟`
  }

  if (Math.abs(diff) < day) {
    return `${Math.round(diff / hour)} 小时`
  }

  return `${Math.round(diff / day)} 天`
}

export function formatCount(value?: number | null) {
  return numberFormatter.format(value || 0)
}

export function normalizeNullableText(value?: string | null) {
  const normalized = value?.trim() || ''
  return normalized || null
}

export function linesToArray(value?: string | null) {
  return (value || '')
    .split(/\r?\n/g)
    .map(line => line.trim())
    .filter(Boolean)
}

export function findFirstTooLongEntry(values: string[] | null | undefined, max: number) {
  return (values || []).find(value => value.length > max) || null
}

export function arrayToLines(value?: string[] | null) {
  return (value || []).join('\n')
}

export function truncateText(value?: string | null, max = 140) {
  if (!value) {
    return ''
  }

  return value.length > max ? `${value.slice(0, max)}...` : value
}

export function toDateTimeLocal(value?: string | null) {
  if (!value) {
    return ''
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const offset = date.getTimezoneOffset()
  const localDate = new Date(date.getTime() - offset * 60 * 1000)
  return localDate.toISOString().slice(0, 16)
}

export function toIsoOrNull(value?: string | null) {
  if (!value) {
    return null
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

export function getNotificationLink(item: NotificationItemView) {
  if (item.conversationId) {
    return `/messages?id=${item.conversationId}`
  }

  if (item.postId) {
    return `/posts/${item.postId}`
  }

  if (item.targetType === 'post') {
    return `/posts/${item.targetId}`
  }

  if (item.targetType === 'conversation') {
    return `/messages?id=${item.targetId}`
  }

  return '/'
}
