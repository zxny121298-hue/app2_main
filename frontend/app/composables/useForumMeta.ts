import type { BoardView, NotificationUnreadView } from '~/types/forum'

export function useForumMeta() {
  const api = useApi()
  const auth = useAuth()
  const boards = useState<BoardView[]>('forum:boards', () => [])
  const boardsLoaded = useState('forum:boards-loaded', () => false)
  const unreadCount = useState('forum:unread-count', () => 0)

  async function loadBoards(force = false) {
    if (boardsLoaded.value && !force) {
      return boards.value
    }

    boards.value = await api.request<BoardView[]>('/api/boards')
    boardsLoaded.value = true

    return boards.value
  }

  async function refreshUnreadCount() {
    if (!auth.isLoggedIn.value) {
      unreadCount.value = 0
      return unreadCount.value
    }

    const data = await api.request<NotificationUnreadView>('/api/notifications/unread-count')
    unreadCount.value = data.unreadCount

    return unreadCount.value
  }

  return {
    boards,
    boardsLoaded,
    unreadCount,
    loadBoards,
    refreshUnreadCount
  }
}
