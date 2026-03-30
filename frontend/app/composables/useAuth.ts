import type { LoginResponse, UserProfileView } from '~/types/forum'

const TOKEN_COOKIE_KEY = 'forum-access-token'
const USER_COOKIE_KEY = 'forum-user'

function parseUser(value?: string | null) {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value) as UserProfileView
  } catch {
    return null
  }
}

export function useAuth() {
  const tokenCookie = useCookie<string | null>(TOKEN_COOKIE_KEY, {
    sameSite: 'lax',
    default: () => null
  })
  const userCookie = useCookie<string | null>(USER_COOKIE_KEY, {
    sameSite: 'lax',
    default: () => null
  })

  const ready = useState('auth:ready', () => false)
  const token = useState<string | null>('auth:token', () => null)
  const profile = useState<UserProfileView | null>('auth:profile', () => null)

  function bootstrap() {
    if (ready.value) {
      return
    }

    token.value = tokenCookie.value ?? null
    profile.value = parseUser(userCookie.value)
    ready.value = true
  }

  function persist() {
    tokenCookie.value = token.value
    userCookie.value = profile.value ? JSON.stringify(profile.value) : null
  }

  function applyLogin(response: LoginResponse) {
    token.value = response.accessToken
    profile.value = response.user
    persist()
  }

  function setProfile(value: UserProfileView | null) {
    profile.value = value
    persist()
  }

  function clear() {
    token.value = null
    profile.value = null
    persist()
  }

  async function logout() {
    clear()
    await navigateTo('/login')
  }

  bootstrap()

  const isLoggedIn = computed(() => Boolean(token.value && profile.value))
  const isAdmin = computed(() => profile.value?.role === 'admin')

  return {
    ready,
    token,
    profile,
    isLoggedIn,
    isAdmin,
    bootstrap,
    applyLogin,
    setProfile,
    clear,
    logout
  }
}
