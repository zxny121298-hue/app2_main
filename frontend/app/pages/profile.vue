<script setup lang="ts">
import { z } from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type {
  ChangePasswordRequest,
  CheckInView,
  CoinBalanceView,
  CoinLedgerView,
  CommentView,
  ExpLogView,
  FollowRelationView,
  PageResult,
  PostCardView,
  UpdateProfileRequest,
  UserProfileView
} from '~/types/forum'
import {
  coinChangeLabels,
  displayName,
  expChangeLabels,
  formatCount,
  formatDateTime,
  initialsOf,
  truncateText,
  userStatusLabels
} from '~/utils/forum'

definePageMeta({
  middleware: 'auth'
})

function createEmptyPage<T>(pageSize: number): PageResult<T> {
  return {
    total: 0,
    page: 1,
    pageSize,
    list: []
  }
}

const toast = useToast()
const auth = useAuth()
const api = useApi()
const { uploadImage, accept: imageAccept, maxBytes: maxUploadBytes } = useImageUpload()
const isAdmin = computed(() => auth.isAdmin.value)
const MAX_NICKNAME_LENGTH = 50
const MAX_AVATAR_URL_LENGTH = 255
const MAX_BIO_LENGTH = 500
const PASSWORD_MIN_LENGTH = 6
const PASSWORD_MAX_LENGTH = 64
const currentProfile = computed(() => auth.profile.value)
const currentStatus = computed(() => currentProfile.value?.status || 'active')

const passwordSchema = z.object({
  oldPassword: z
    .string()
    .min(PASSWORD_MIN_LENGTH, `当前密码至少 ${PASSWORD_MIN_LENGTH} 个字符`)
    .max(PASSWORD_MAX_LENGTH, `当前密码最多 ${PASSWORD_MAX_LENGTH} 个字符`),
  newPassword: z
    .string()
    .min(PASSWORD_MIN_LENGTH, `新密码至少 ${PASSWORD_MIN_LENGTH} 个字符`)
    .max(PASSWORD_MAX_LENGTH, `新密码最多 ${PASSWORD_MAX_LENGTH} 个字符`),
  confirmNewPassword: z
    .string()
    .min(PASSWORD_MIN_LENGTH, `确认新密码至少 ${PASSWORD_MIN_LENGTH} 个字符`)
    .max(PASSWORD_MAX_LENGTH, `确认新密码最多 ${PASSWORD_MAX_LENGTH} 个字符`)
}).superRefine((value, ctx) => {
  if (value.newPassword !== value.confirmNewPassword) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: '两次输入的新密码不一致',
      path: ['confirmNewPassword']
    })
  }

  if (value.oldPassword === value.newPassword) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: '新密码不能与当前密码相同',
      path: ['newPassword']
    })
  }
})

type PasswordSchema = z.output<typeof passwordSchema>

const overviewLoading = ref(false)
const savePending = ref(false)
const passwordPending = ref(false)
const checkInPending = ref(false)
const avatarUploadPending = ref(false)
const activeTab = ref('overview')
const tabItems = [
  { label: '总览', value: 'overview' },
  { label: '我的帖子', value: 'my-posts' },
  { label: '收藏帖子', value: 'favorite-posts' },
  { label: '收藏评论', value: 'favorite-comments' },
  { label: '我的关注', value: 'follows' },
  { label: '我的粉丝', value: 'fans' },
  { label: '经验记录', value: 'exp-logs' },
  { label: '硬币流水', value: 'coin-ledgers' }
]

const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
  bio: ''
})
const passwordForm = reactive<PasswordSchema>({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: ''
})
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarPreviewUrl = computed(() => profileForm.avatarUrl.trim() || '')
const maxAvatarUploadSizeMb = Math.floor(maxUploadBytes / (1024 * 1024))
const defaultCheckInExpGain = 5
const defaultCheckInCoinGain = 5
const displayedCheckInExpGain = computed(() => checkInInfo.value?.checkedInToday ? checkInInfo.value.expGain : defaultCheckInExpGain)
const displayedCheckInCoinGain = computed(() => checkInInfo.value?.checkedInToday ? checkInInfo.value.coinGain : defaultCheckInCoinGain)

const checkInInfo = ref<CheckInView | null>(null)
const balance = ref(0)

const myPostsPageSize = 8
const favoritePostsPageSize = 8
const favoriteCommentsPageSize = 8
const followsPageSize = 10
const fansPageSize = 10
const expLogsPageSize = 10
const coinLedgersPageSize = 10

const myPostsPage = ref(1)
const favoritePostsPage = ref(1)
const favoriteCommentsPage = ref(1)
const followsPage = ref(1)
const fansPage = ref(1)
const expLogsPage = ref(1)
const coinLedgersPage = ref(1)

const myPosts = ref(createEmptyPage<PostCardView>(myPostsPageSize))
const favoritePosts = ref(createEmptyPage<PostCardView>(favoritePostsPageSize))
const favoriteComments = ref(createEmptyPage<CommentView>(favoriteCommentsPageSize))
const follows = ref(createEmptyPage<FollowRelationView>(followsPageSize))
const fans = ref(createEmptyPage<FollowRelationView>(fansPageSize))
const expLogs = ref(createEmptyPage<ExpLogView>(expLogsPageSize))
const coinLedgers = ref(createEmptyPage<CoinLedgerView>(coinLedgersPageSize))

watch(
  () => auth.profile.value,
  (profile) => {
    profileForm.nickname = profile?.nickname || ''
    profileForm.avatarUrl = profile?.avatarUrl || ''
    profileForm.bio = profile?.bio || ''
  },
  { immediate: true }
)

async function loadOverview() {
  overviewLoading.value = true

  try {
    const [profile, todayCheckIn, coinBalance] = await Promise.all([
      api.request<UserProfileView>('/api/users/me'),
      api.request<CheckInView>('/api/check-ins/today'),
      api.request<CoinBalanceView>('/api/coins/balance')
    ])

    auth.setProfile(profile)
    checkInInfo.value = todayCheckIn
    balance.value = coinBalance.coinBalance
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '个人资料加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    overviewLoading.value = false
  }
}

async function saveProfile() {
  const nickname = profileForm.nickname.trim()
  const avatarUrl = profileForm.avatarUrl.trim()
  const bio = profileForm.bio.trim()

  if (nickname.length > MAX_NICKNAME_LENGTH) {
    toast.add({
      title: `昵称不能超过 ${MAX_NICKNAME_LENGTH} 个字符`,
      color: 'warning'
    })
    return
  }

  if (avatarUrl.length > MAX_AVATAR_URL_LENGTH) {
    toast.add({
      title: `头像地址不能超过 ${MAX_AVATAR_URL_LENGTH} 个字符`,
      color: 'warning'
    })
    return
  }

  if (bio.length > MAX_BIO_LENGTH) {
    toast.add({
      title: `个人简介不能超过 ${MAX_BIO_LENGTH} 个字符`,
      color: 'warning'
    })
    return
  }

  savePending.value = true

  try {
    const payload: UpdateProfileRequest = {
      nickname: nickname || null,
      avatarUrl: avatarUrl || null,
      bio: bio || null
    }

    const profile = await api.request<UserProfileView>('/api/users/me', {
      method: 'PUT',
      body: payload
    })

    auth.setProfile(profile)

    toast.add({
      title: '资料已更新',
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '更新资料失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    savePending.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmNewPassword = ''
}

async function submitPasswordForm(event: FormSubmitEvent<PasswordSchema>) {
  if (passwordPending.value) {
    return
  }

  passwordPending.value = true

  try {
    const payload: ChangePasswordRequest = {
      oldPassword: event.data.oldPassword,
      newPassword: event.data.newPassword
    }

    await api.request<void>('/api/users/me/password', {
      method: 'PUT',
      body: payload
    })

    resetPasswordForm()
    toast.add({
      title: '密码已修改，请重新登录',
      color: 'success'
    })
    auth.clear()
    await navigateTo(`/login?redirect=${encodeURIComponent('/profile')}`)
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '修改密码失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    passwordPending.value = false
  }
}

function openAvatarPicker() {
  avatarInput.value?.click()
}

function clearAvatar() {
  profileForm.avatarUrl = ''
}

async function handleAvatarSelected(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''

  if (!file) {
    return
  }

  avatarUploadPending.value = true

  try {
    const uploaded = await uploadImage(file, 'avatar')
    profileForm.avatarUrl = uploaded.url

    toast.add({
      title: '头像上传成功',
      description: '头像已写入资料表单，保存资料后生效',
      color: 'success'
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : '请稍后重试'

    toast.add({
      title: '头像上传失败',
      description: message,
      color: 'error'
    })
  } finally {
    avatarUploadPending.value = false
  }
}

async function submitCheckIn() {
  checkInPending.value = true

  try {
    const result = await api.request<CheckInView>('/api/check-ins', {
      method: 'POST'
    })
    checkInInfo.value = result

    await loadOverview()

    toast.add({
      title: '签到成功',
      description: `获得 ${result.expGain} 经验值和 ${result.coinGain} 枚硬币`,
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '签到失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    checkInPending.value = false
  }
}

async function loadMyPosts() {
  myPosts.value = await api.request<PageResult<PostCardView>>('/api/posts/mine', {
    query: {
      page: myPostsPage.value,
      pageSize: myPostsPageSize
    }
  })
}

async function loadFavoritePosts() {
  favoritePosts.value = await api.request<PageResult<PostCardView>>('/api/favorites/posts', {
    query: {
      page: favoritePostsPage.value,
      pageSize: favoritePostsPageSize
    }
  })
}

async function loadFavoriteComments() {
  favoriteComments.value = await api.request<PageResult<CommentView>>('/api/favorites/comments', {
    query: {
      page: favoriteCommentsPage.value,
      pageSize: favoriteCommentsPageSize
    }
  })
}

async function loadFollows() {
  follows.value = await api.request<PageResult<FollowRelationView>>('/api/follows', {
    query: {
      page: followsPage.value,
      pageSize: followsPageSize
    }
  })
}

async function loadFans() {
  fans.value = await api.request<PageResult<FollowRelationView>>('/api/fans', {
    query: {
      page: fansPage.value,
      pageSize: fansPageSize
    }
  })
}

async function loadExpLogs() {
  expLogs.value = await api.request<PageResult<ExpLogView>>('/api/users/me/exp-logs', {
    query: {
      page: expLogsPage.value,
      pageSize: expLogsPageSize
    }
  })
}

async function loadCoinLedgers() {
  coinLedgers.value = await api.request<PageResult<CoinLedgerView>>('/api/coins/ledgers', {
    query: {
      page: coinLedgersPage.value,
      pageSize: coinLedgersPageSize
    }
  })
}

async function loadActiveTab() {
  try {
    switch (activeTab.value) {
      case 'overview':
        await loadOverview()
        break
      case 'my-posts':
        await loadMyPosts()
        break
      case 'favorite-posts':
        await loadFavoritePosts()
        break
      case 'favorite-comments':
        await loadFavoriteComments()
        break
      case 'follows':
        await loadFollows()
        break
      case 'fans':
        await loadFans()
        break
      case 'exp-logs':
        await loadExpLogs()
        break
      case 'coin-ledgers':
        await loadCoinLedgers()
        break
    }
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '数据加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

watch(activeTab, loadActiveTab, { immediate: true })

watch(myPostsPage, async () => {
  if (activeTab.value === 'my-posts') {
    await loadMyPosts()
  }
})

watch(favoritePostsPage, async () => {
  if (activeTab.value === 'favorite-posts') {
    await loadFavoritePosts()
  }
})

watch(favoriteCommentsPage, async () => {
  if (activeTab.value === 'favorite-comments') {
    await loadFavoriteComments()
  }
})

watch(followsPage, async () => {
  if (activeTab.value === 'follows') {
    await loadFollows()
  }
})

watch(fansPage, async () => {
  if (activeTab.value === 'fans') {
    await loadFans()
  }
})

watch(expLogsPage, async () => {
  if (activeTab.value === 'exp-logs') {
    await loadExpLogs()
  }
})

watch(coinLedgersPage, async () => {
  if (activeTab.value === 'coin-ledgers') {
    await loadCoinLedgers()
  }
})
</script>

<template>
  <div class="space-y-6">
    <section class="overflow-hidden rounded-[2rem] border border-white/70 bg-white/90 p-6 shadow-xl shadow-slate-900/5 backdrop-blur">
      <div class="grid gap-6 lg:grid-cols-[1.1fr,0.9fr] lg:items-end">
        <div>
          <div class="inline-flex rounded-full border border-teal-200 bg-teal-50 px-3 py-1 text-xs font-medium text-teal-800">
            我的空间
          </div>
          <h1 class="mt-4 text-4xl font-semibold tracking-tight text-slate-900">
            {{ displayName(currentProfile) }}
          </h1>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
            这里集中接入 `/api/users/me`、签到、经验记录、硬币流水、我的帖子、收藏、关注和粉丝接口。
          </p>
        </div>

        <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <div class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4">
            <div class="text-sm text-slate-500">
              当前等级
            </div>
            <div class="mt-2 text-3xl font-semibold text-slate-900">
              Lv.{{ currentProfile?.level || 1 }}
            </div>
          </div>
          <div class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4">
            <div class="text-sm text-slate-500">
              总经验
            </div>
            <div class="mt-2 text-3xl font-semibold text-slate-900">
              {{ formatCount(currentProfile?.totalExp) }}
            </div>
          </div>
          <div class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4">
            <div class="text-sm text-slate-500">
              当前等级经验
            </div>
            <div class="mt-2 text-3xl font-semibold text-slate-900">
              {{ formatCount(currentProfile?.currentLevelExp) }}
            </div>
          </div>
          <div class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4">
            <div class="text-sm text-slate-500">
              硬币余额
            </div>
            <div class="mt-2 text-3xl font-semibold text-slate-900">
              {{ formatCount(balance) }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-[2rem] border border-white/70 bg-white/90 p-6 shadow-xl shadow-slate-900/5">
      <UTabs
        v-model="activeTab"
        :items="tabItems"
        value-key="value"
        :unmount-on-hide="false"
        class="space-y-6"
      >
        <template #content="{ item }">
          <div v-if="item.value === 'overview'" class="grid gap-6 lg:grid-cols-[1.1fr,0.9fr]">
            <div class="space-y-6">
              <section class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5">
                <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div class="text-lg font-semibold text-slate-900">
                        基本资料
                      </div>
                      <div class="mt-1 text-sm text-slate-500">
                        更新昵称、头像和个人简介。
                      </div>
                    </div>
                  <UButton
                    :loading="savePending"
                    class="rounded-full bg-slate-900 text-white hover:bg-slate-800"
                    @click="saveProfile"
                  >
                    保存资料
                  </UButton>
                </div>

                <div class="mt-5 grid gap-4">
                  <div class="rounded-[2rem] border border-slate-200 bg-white p-5">
                    <div class="flex flex-col gap-5 lg:flex-row lg:items-center">
                      <div class="flex flex-col items-center gap-3">
                        <div class="flex h-28 w-28 items-center justify-center overflow-hidden rounded-full border border-slate-200 bg-slate-900 text-2xl font-semibold text-white shadow-sm">
                          <img
                            v-if="avatarPreviewUrl"
                            :src="avatarPreviewUrl"
                            :alt="displayName(currentProfile)"
                            class="h-full w-full object-cover"
                          >
                          <span v-else>{{ initialsOf(currentProfile) }}</span>
                        </div>

                        <input
                          ref="avatarInput"
                          type="file"
                          class="hidden"
                          :accept="imageAccept"
                          @change="handleAvatarSelected"
                        >

                        <div class="flex flex-wrap justify-center gap-2">
                          <UButton
                            color="neutral"
                            variant="outline"
                            class="rounded-full"
                            :loading="avatarUploadPending"
                            @click="openAvatarPicker"
                          >
                            上传头像
                          </UButton>
                          <UButton
                            v-if="profileForm.avatarUrl"
                            color="neutral"
                            variant="ghost"
                            class="rounded-full"
                            @click="clearAvatar"
                          >
                            移除头像
                          </UButton>
                        </div>
                      </div>

                      <div class="min-w-0 flex-1 space-y-2">
                        <div class="text-lg font-semibold text-slate-900">
                          头像预览
                        </div>
                        <p class="text-sm leading-7 text-slate-500">
                          支持 JPG、PNG、WEBP、GIF、AVIF，最大 {{ maxAvatarUploadSizeMb }} MB。上传成功后会自动更新头像，点击“保存资料”后正式生效。
                        </p>
                      </div>
                    </div>
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium text-slate-700">昵称</label>
                    <UInput v-model="profileForm.nickname" :ui="{ base: 'rounded-2xl bg-white' }" />
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium text-slate-700">个人简介</label>
                    <UTextarea
                      v-model="profileForm.bio"
                      :rows="5"
                      :ui="{ base: 'rounded-[2rem] bg-white' }"
                    />
                  </div>
                </div>
              </section>

              <section class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5">
                <div>
                  <div class="text-lg font-semibold text-slate-900">
                    修改密码
                  </div>
                  <div class="mt-1 text-sm text-slate-500">
                    需要先输入当前密码。修改成功后会立即退出当前登录状态。
                  </div>
                </div>

                <UForm
                  :schema="passwordSchema"
                  :state="passwordForm"
                  class="mt-5 space-y-4"
                  @submit="submitPasswordForm"
                >
                  <UFormField label="当前密码" name="oldPassword" required>
                    <UInput
                      v-model="passwordForm.oldPassword"
                      type="password"
                      autocomplete="current-password"
                      :ui="{ base: 'rounded-2xl bg-white' }"
                    />
                  </UFormField>

                  <UFormField label="新密码" name="newPassword" required>
                    <UInput
                      v-model="passwordForm.newPassword"
                      type="password"
                      autocomplete="new-password"
                      :ui="{ base: 'rounded-2xl bg-white' }"
                    />
                  </UFormField>

                  <UFormField label="确认新密码" name="confirmNewPassword" required>
                    <UInput
                      v-model="passwordForm.confirmNewPassword"
                      type="password"
                      autocomplete="new-password"
                      :ui="{ base: 'rounded-2xl bg-white' }"
                    />
                  </UFormField>

                  <div class="flex flex-wrap items-center gap-3 pt-2">
                    <UButton
                      type="submit"
                      :loading="passwordPending"
                      class="rounded-full bg-slate-900 text-white hover:bg-slate-800"
                    >
                      更新密码
                    </UButton>
                    <UButton
                      type="button"
                      color="neutral"
                      variant="outline"
                      class="rounded-full"
                      @click="resetPasswordForm"
                    >
                      清空
                    </UButton>
                  </div>
                </UForm>
              </section>

              <section class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5">
                <div class="text-lg font-semibold text-slate-900">
                  账户状态
                </div>
                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      用户角色
                    </div>
                    <div class="mt-2 text-xl font-semibold text-slate-900">
                      {{ isAdmin ? '管理员' : '普通用户' }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      账户状态
                    </div>
                    <div class="mt-2 text-xl font-semibold text-slate-900">
                      {{ userStatusLabels[currentStatus] }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      禁言截至
                    </div>
                    <div class="mt-2 text-sm font-medium text-slate-900">
                      {{ formatDateTime(currentProfile?.mutedUntilAt) }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      封禁截至
                    </div>
                    <div class="mt-2 text-sm font-medium text-slate-900">
                      {{ formatDateTime(currentProfile?.bannedUntilAt) }}
                    </div>
                  </div>
                </div>

                <div class="mt-4 space-y-3">
                  <UAlert
                    v-if="currentProfile?.muteReason"
                    color="warning"
                    variant="soft"
                    icon="i-lucide-message-square-warning"
                    title="当前禁言原因"
                    :description="currentProfile.muteReason"
                  />
                  <UAlert
                    v-if="currentProfile?.banReason"
                    color="error"
                    variant="soft"
                    icon="i-lucide-shield-alert"
                    title="当前封禁原因"
                    :description="currentProfile.banReason"
                  />
                </div>
              </section>
            </div>

            <div class="space-y-6">
              <section class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <div class="text-lg font-semibold text-slate-900">
                      今日签到
                    </div>
                    <div class="mt-1 text-sm text-slate-500">
                      每天成功一次，获得 {{ displayedCheckInExpGain }} 点经验和 {{ displayedCheckInCoinGain }} 枚硬币。
                    </div>
                  </div>
                  <UButton
                    :disabled="checkInInfo?.checkedInToday"
                    :loading="checkInPending"
                    class="rounded-full bg-slate-900 text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    @click="submitCheckIn"
                  >
                    {{ checkInInfo?.checkedInToday ? '已签到' : '立即签到' }}
                  </UButton>
                </div>

                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      签到状态
                    </div>
                    <div class="mt-2 text-xl font-semibold text-slate-900">
                      {{ checkInInfo?.checkedInToday ? '今日已完成' : '尚未签到' }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      签到时间
                    </div>
                    <div class="mt-2 text-sm font-medium text-slate-900">
                      {{ formatDateTime(checkInInfo?.checkedAt) }}
                    </div>
                  </div>
                </div>
              </section>

              <section class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5">
                <div class="text-lg font-semibold text-slate-900">
                  账户概览
                </div>
                <div class="mt-4 grid gap-3">
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      用户名
                    </div>
                    <div class="mt-2 text-lg font-semibold text-slate-900">
                      {{ currentProfile?.username }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      注册时间
                    </div>
                    <div class="mt-2 text-sm font-medium text-slate-900">
                      {{ formatDateTime(currentProfile?.createdAt) }}
                    </div>
                  </div>
                  <div class="rounded-3xl bg-white px-4 py-4">
                    <div class="text-sm text-slate-500">
                      最近登录
                    </div>
                    <div class="mt-2 text-sm font-medium text-slate-900">
                      {{ formatDateTime(currentProfile?.lastLoginAt) }}
                    </div>
                  </div>
                </div>
              </section>
            </div>
          </div>

          <div v-else-if="item.value === 'my-posts'" class="space-y-4">
            <article
              v-for="post in myPosts.list"
              :key="post.id"
              class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5"
            >
              <NuxtLink
                :to="`/posts/${post.id}`"
                class="text-xl font-semibold text-slate-900 transition hover:text-teal-700"
              >
                {{ post.title }}
              </NuxtLink>
              <p class="mt-3 text-sm leading-7 text-slate-500">
                {{ truncateText(post.contentText, 180) || '该帖子仅包含图片内容。' }}
              </p>
              <div class="mt-4 flex flex-wrap gap-4 text-sm text-slate-500">
                <span>板块 {{ post.boardName }}</span>
                <span>评论 {{ formatCount(post.commentCount) }}</span>
                <span>点赞 {{ formatCount(post.likeCount) }}</span>
                <span>{{ formatDateTime(post.createdAt) }}</span>
              </div>
            </article>

            <EmptyState
              v-if="!myPosts.list.length"
              title="还没有发布过帖子"
              description="你发布的帖子会出现在这里。"
              icon="i-lucide-file-text"
            />

            <div v-if="myPosts.total > myPostsPageSize" class="flex justify-end">
              <UPagination
                :page="myPostsPage"
                :items-per-page="myPostsPageSize"
                :total="myPosts.total"
                @update:page="myPostsPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'favorite-posts'" class="space-y-4">
            <article
              v-for="post in favoritePosts.list"
              :key="post.id"
              class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5"
            >
              <NuxtLink
                :to="`/posts/${post.id}`"
                class="text-xl font-semibold text-slate-900 transition hover:text-teal-700"
              >
                {{ post.title }}
              </NuxtLink>
              <p class="mt-3 text-sm leading-7 text-slate-500">
                {{ truncateText(post.contentText, 180) || '该帖子仅包含图片内容。' }}
              </p>
              <div class="mt-4 flex flex-wrap gap-4 text-sm text-slate-500">
                <span>作者 {{ displayName(post.author) }}</span>
                <span>评论 {{ formatCount(post.commentCount) }}</span>
                <span>点赞 {{ formatCount(post.likeCount) }}</span>
              </div>
            </article>

            <EmptyState
              v-if="!favoritePosts.list.length"
              title="还没有收藏帖子"
              description="你收藏过的帖子会出现在这里。"
              icon="i-lucide-bookmark"
            />

            <div v-if="favoritePosts.total > favoritePostsPageSize" class="flex justify-end">
              <UPagination
                :page="favoritePostsPage"
                :items-per-page="favoritePostsPageSize"
                :total="favoritePosts.total"
                @update:page="favoritePostsPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'favorite-comments'" class="space-y-4">
            <article
              v-for="comment in favoriteComments.list"
              :key="comment.id"
              class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5"
            >
              <div class="flex flex-wrap items-center gap-2 text-sm text-slate-500">
                <NuxtLink
                  :to="`/users/${comment.author.id}`"
                  class="font-semibold text-slate-900 transition hover:text-teal-700"
                >
                  {{ displayName(comment.author) }}
                </NuxtLink>
                <span>发表于 {{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <p class="mt-3 text-sm leading-7 text-slate-600">
                {{ truncateText(comment.contentText, 240) || '该评论仅包含图片内容。' }}
              </p>
              <div class="mt-4 flex flex-wrap gap-2">
                <UButton
                  :to="`/posts/${comment.postId}`"
                  color="neutral"
                  variant="outline"
                  class="rounded-full"
                >
                  查看原帖
                </UButton>
              </div>
            </article>

            <EmptyState
              v-if="!favoriteComments.list.length"
              title="还没有收藏评论"
              description="你收藏过的评论会出现在这里。"
              icon="i-lucide-message-square-heart"
            />

            <div v-if="favoriteComments.total > favoriteCommentsPageSize" class="flex justify-end">
              <UPagination
                :page="favoriteCommentsPage"
                :items-per-page="favoriteCommentsPageSize"
                :total="favoriteComments.total"
                @update:page="favoriteCommentsPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'follows'" class="space-y-4">
            <article
              v-for="relation in follows.list"
              :key="relation.user.id"
              class="flex flex-col gap-4 rounded-[2rem] border border-slate-200 bg-slate-50 p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <div class="text-lg font-semibold text-slate-900">
                  {{ displayName(relation.user) }}
                </div>
                <div class="mt-2 text-sm text-slate-500">
                  关注时间 {{ formatDateTime(relation.followedAt) }}
                </div>
              </div>
              <UButton
                :to="`/users/${relation.user.id}`"
                color="neutral"
                variant="outline"
                class="rounded-full"
              >
                查看主页
              </UButton>
            </article>

            <EmptyState
              v-if="!follows.list.length"
              title="你还没有关注任何人"
              description="关注的用户会出现在这里。"
              icon="i-lucide-user-plus"
            />

            <div v-if="follows.total > followsPageSize" class="flex justify-end">
              <UPagination
                :page="followsPage"
                :items-per-page="followsPageSize"
                :total="follows.total"
                @update:page="followsPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'fans'" class="space-y-4">
            <article
              v-for="relation in fans.list"
              :key="relation.user.id"
              class="flex flex-col gap-4 rounded-[2rem] border border-slate-200 bg-slate-50 p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <div class="text-lg font-semibold text-slate-900">
                  {{ displayName(relation.user) }}
                </div>
                <div class="mt-2 text-sm text-slate-500">
                  成为粉丝于 {{ formatDateTime(relation.followedAt) }} · {{ relation.following ? '已回关' : '未回关' }}
                </div>
              </div>
              <UButton
                :to="`/users/${relation.user.id}`"
                color="neutral"
                variant="outline"
                class="rounded-full"
              >
                查看主页
              </UButton>
            </article>

            <EmptyState
              v-if="!fans.list.length"
              title="你还没有粉丝"
              description="其他用户关注你之后会在这里显示。"
              icon="i-lucide-users"
            />

            <div v-if="fans.total > fansPageSize" class="flex justify-end">
              <UPagination
                :page="fansPage"
                :items-per-page="fansPageSize"
                :total="fans.total"
                @update:page="fansPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'exp-logs'" class="space-y-3">
            <article
              v-for="log in expLogs.list"
              :key="log.id"
              class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="text-lg font-semibold text-slate-900">
                  {{ expChangeLabels[log.changeType] }}
                </div>
                <div class="text-sm font-semibold text-teal-700">
                  {{ log.changeExp > 0 ? '+' : '' }}{{ log.changeExp }} EXP
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-4 text-sm text-slate-500">
                <span>总经验 {{ formatCount(log.totalExpAfter) }}</span>
                <span>等级 Lv.{{ log.levelAfter }}</span>
                <span>{{ formatDateTime(log.createdAt) }}</span>
              </div>
              <div v-if="log.remark" class="mt-3 text-sm text-slate-500">
                备注：{{ log.remark }}
              </div>
            </article>

            <EmptyState
              v-if="!expLogs.list.length"
              title="还没有经验记录"
              description="签到、发帖、评论和管理员调整都会产生经验流水。"
              icon="i-lucide-sparkles"
            />

            <div v-if="expLogs.total > expLogsPageSize" class="flex justify-end">
              <UPagination
                :page="expLogsPage"
                :items-per-page="expLogsPageSize"
                :total="expLogs.total"
                @update:page="expLogsPage = $event"
              />
            </div>
          </div>

          <div v-else-if="item.value === 'coin-ledgers'" class="space-y-3">
            <article
              v-for="ledger in coinLedgers.list"
              :key="ledger.id"
              class="rounded-[2rem] border border-slate-200 bg-slate-50 p-5"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="text-lg font-semibold text-slate-900">
                  {{ coinChangeLabels[ledger.changeType] }}
                </div>
                <div
                  class="text-sm font-semibold"
                  :class="ledger.changeAmount >= 0 ? 'text-teal-700' : 'text-rose-700'"
                >
                  {{ ledger.changeAmount > 0 ? '+' : '' }}{{ ledger.changeAmount }}
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-4 text-sm text-slate-500">
                <span>余额 {{ formatCount(ledger.balanceAfter) }}</span>
                <span>{{ formatDateTime(ledger.createdAt) }}</span>
              </div>
              <div v-if="ledger.description" class="mt-3 text-sm text-slate-500">
                说明：{{ ledger.description }}
              </div>
            </article>

            <EmptyState
              v-if="!coinLedgers.list.length"
              title="还没有硬币流水"
              description="打赏、系统发放和管理员调整都会在这里留下记录。"
              icon="i-lucide-coins"
            />

            <div v-if="coinLedgers.total > coinLedgersPageSize" class="flex justify-end">
              <UPagination
                :page="coinLedgersPage"
                :items-per-page="coinLedgersPageSize"
                :total="coinLedgers.total"
                @update:page="coinLedgersPage = $event"
              />
            </div>
          </div>
        </template>
      </UTabs>
    </section>

    <div v-if="overviewLoading && activeTab === 'overview'" class="h-40 animate-pulse rounded-[2rem] border border-white/70 bg-white/70" />
  </div>
</template>
