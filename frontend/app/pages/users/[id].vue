<script setup lang="ts">
import type {
  ConversationDetailView,
  FollowStatusView,
  PageResult,
  PostCardView,
  UserProfileView
} from '~/types/forum'
import {
  displayName,
  formatCount,
  formatDateTime,
  initialsOf,
  toDateTimeLocal,
  toIsoOrNull,
  truncateText,
  userStatusLabels
} from '~/utils/forum'

const route = useRoute()
const toast = useToast()
const auth = useAuth()
const api = useApi()
const MAX_ADMIN_NOTE_LENGTH = 255
const isLoggedIn = computed(() => auth.isLoggedIn.value)
const isAdmin = computed(() => auth.isAdmin.value)

const user = ref<UserProfileView | null>(null)
const followState = ref<FollowStatusView | null>(null)
const postsPage = ref(1)
const pageSize = 8
const posts = ref<PageResult<PostCardView>>({
  total: 0,
  page: 1,
  pageSize,
  list: []
})

const banForm = reactive({
  bannedUntilAt: '',
  reason: ''
})

const muteForm = reactive({
  mutedUntilAt: '',
  reason: ''
})

const adjustExpForm = reactive({
  changeExp: 0,
  remark: ''
})

const adjustCoinForm = reactive({
  changeAmount: 0,
  remark: ''
})

const pending = ref(false)
const errorMessage = ref('')

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => auth.profile.value?.id === user.value?.id)

function validateAdminTextLength(value: string, fieldLabel: string) {
  if (value.trim().length <= MAX_ADMIN_NOTE_LENGTH) {
    return true
  }

  toast.add({
    title: `${fieldLabel}不能超过 ${MAX_ADMIN_NOTE_LENGTH} 个字符`,
    color: 'warning'
  })
  return false
}

watch(user, (profile) => {
  banForm.bannedUntilAt = toDateTimeLocal(profile?.bannedUntilAt)
  banForm.reason = profile?.banReason || ''
  muteForm.mutedUntilAt = toDateTimeLocal(profile?.mutedUntilAt)
  muteForm.reason = profile?.muteReason || ''
})

async function loadUser() {
  pending.value = true
  errorMessage.value = ''

  try {
    user.value = await api.request<UserProfileView>(`/api/users/${userId.value}`)
  } catch (error) {
    const apiError = error as { message?: string }
    errorMessage.value = apiError.message || '用户信息加载失败'
  } finally {
    pending.value = false
  }
}

async function loadPosts() {
  posts.value = await api.request<PageResult<PostCardView>>(`/api/users/${userId.value}/posts`, {
    query: {
      page: postsPage.value,
      pageSize
    }
  })
}

async function loadFollowState() {
  if (!auth.isLoggedIn.value || isSelf.value) {
    followState.value = null
    return
  }

  followState.value = await api.request<FollowStatusView>(`/api/users/${userId.value}/followed`)
}

async function refreshPage() {
  await Promise.all([loadUser(), loadPosts()])
  await loadFollowState()
}

async function toggleFollow() {
  if (!auth.isLoggedIn.value) {
    await navigateTo(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  try {
    await api.request<void>(`/api/users/${userId.value}/follow`, {
      method: followState.value?.following ? 'DELETE' : 'POST'
    })

    followState.value = {
      targetUserId: userId.value,
      following: !followState.value?.following
    }

    toast.add({
      title: followState.value.following ? '关注成功' : '已取消关注',
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '关注操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function startConversation() {
  if (!auth.isLoggedIn.value) {
    await navigateTo(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  try {
    const conversation = await api.request<ConversationDetailView>('/api/conversations', {
      method: 'POST',
      body: {
        targetUserId: userId.value
      }
    })

    await navigateTo(`/messages?id=${conversation.id}`)
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '创建会话失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function submitBan() {
  if (!validateAdminTextLength(banForm.reason, '封禁原因')) {
    return
  }

  try {
    await api.request<void>(`/api/admin/users/${userId.value}/ban`, {
      method: 'POST',
      body: {
        bannedUntilAt: toIsoOrNull(banForm.bannedUntilAt),
        reason: banForm.reason.trim() || null
      }
    })

    toast.add({
      title: '封禁设置已更新',
      color: 'success'
    })

    await refreshPage()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '封禁设置失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function unban() {
  try {
    await api.request<void>(`/api/admin/users/${userId.value}/unban`, {
      method: 'POST'
    })

    toast.add({
      title: '已解除封禁',
      color: 'success'
    })

    await refreshPage()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '解除封禁失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function submitMute() {
  if (!muteForm.mutedUntilAt) {
    toast.add({
      title: '请填写禁言截止时间',
      description: '根据当前后端实现，留空不会形成真正的禁言效果。',
      color: 'warning'
    })
    return
  }

  if (!validateAdminTextLength(muteForm.reason, '禁言原因')) {
    return
  }

  const mutedUntilAt = toIsoOrNull(muteForm.mutedUntilAt)

  if (!mutedUntilAt) {
    toast.add({
      title: '禁言截止时间格式无效',
      color: 'warning'
    })
    return
  }

  try {
    await api.request<void>(`/api/admin/users/${userId.value}/mute`, {
      method: 'POST',
      body: {
        mutedUntilAt,
        reason: muteForm.reason.trim() || null
      }
    })

    toast.add({
      title: '禁言设置已更新',
      color: 'success'
    })

    await refreshPage()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '禁言设置失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function unmute() {
  try {
    await api.request<void>(`/api/admin/users/${userId.value}/unmute`, {
      method: 'POST'
    })

    toast.add({
      title: '已解除禁言',
      color: 'success'
    })

    await refreshPage()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '解除禁言失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function adjustExp() {
  const changeExp = Number(adjustExpForm.changeExp)

  if (!Number.isFinite(changeExp) || changeExp === 0) {
    toast.add({
      title: '经验调整值不能为 0',
      color: 'warning'
    })
    return
  }

  if (!validateAdminTextLength(adjustExpForm.remark, '经验调整备注')) {
    return
  }

  try {
    const profile = await api.request<UserProfileView>(`/api/admin/users/${userId.value}/exp-adjust`, {
      method: 'POST',
      body: {
        changeExp,
        remark: adjustExpForm.remark.trim() || null
      }
    })

    user.value = profile
    adjustExpForm.changeExp = 0
    adjustExpForm.remark = ''

    toast.add({
      title: '经验调整成功',
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '经验调整失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function adjustCoin() {
  const changeAmount = Number(adjustCoinForm.changeAmount)

  if (!Number.isFinite(changeAmount) || changeAmount === 0) {
    toast.add({
      title: '硬币调整值不能为 0',
      color: 'warning'
    })
    return
  }

  if (!validateAdminTextLength(adjustCoinForm.remark, '硬币调整备注')) {
    return
  }

  try {
    const profile = await api.request<UserProfileView>(`/api/admin/users/${userId.value}/coin-adjust`, {
      method: 'POST',
      body: {
        changeAmount,
        remark: adjustCoinForm.remark.trim() || null
      }
    })

    user.value = profile
    adjustCoinForm.changeAmount = 0
    adjustCoinForm.remark = ''

    toast.add({
      title: '硬币调整成功',
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '硬币调整失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

watch(
  () => route.params.id,
  async () => {
    postsPage.value = 1
    await refreshPage()
  },
  { immediate: true }
)

watch(postsPage, loadPosts)
</script>

<template>
  <div class="space-y-6">
    <UAlert
      v-if="errorMessage"
      color="error"
      variant="soft"
      icon="i-lucide-circle-alert"
      :title="errorMessage"
    />

    <template v-if="user">
      <section class="overflow-hidden rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-xl shadow-slate-900/5">
        <div class="grid gap-6 lg:grid-cols-[1.15fr,0.85fr]">
          <div>
            <div class="flex flex-col gap-5 sm:flex-row sm:items-start">
              <div class="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border border-default bg-primary text-3xl font-semibold text-white shadow-sm shadow-slate-900/10">
                <img
                  v-if="user.avatarUrl"
                  :src="user.avatarUrl"
                  :alt="displayName(user)"
                  class="h-full w-full object-cover"
                >
                <span v-else>{{ initialsOf(user) }}</span>
              </div>

              <div class="min-w-0 flex-1">
                <div class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                  用户主页
                </div>
                <h1 class="mt-4 text-4xl font-semibold tracking-tight text-default">
                  {{ displayName(user) }}
                </h1>
                <div class="mt-3 text-sm font-medium text-muted">
                  @{{ user.username }}
                </div>
                <p class="mt-3 text-sm leading-7 text-muted">
                  {{ user.bio || '这个用户还没有填写个人简介。' }}
                </p>

                <div class="mt-6 flex flex-wrap gap-2">
                  <UButton
                    v-if="isLoggedIn && !isSelf"
                    color="neutral"
                    variant="outline"
                    class="rounded-full"
                    @click="toggleFollow"
                  >
                    {{ followState?.following ? '取消关注' : '关注 Ta' }}
                  </UButton>
                  <UButton
                    v-if="isLoggedIn && !isSelf"
                    class="rounded-full bg-primary text-white hover:bg-primary/90"
                    @click="startConversation"
                  >
                    发私信
                  </UButton>
                  <UButton
                    v-if="isSelf"
                    to="/profile"
                    color="neutral"
                    variant="outline"
                    class="rounded-full"
                  >
                    回到我的资料
                  </UButton>
                </div>
              </div>
            </div>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                角色
              </div>
              <div class="mt-2 text-xl font-semibold text-default">
                {{ user.role === 'admin' ? '管理员' : '普通用户' }}
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                状态
              </div>
              <div class="mt-2 text-xl font-semibold text-default">
                {{ userStatusLabels[user.status] }}
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                等级 / 经验
              </div>
              <div class="mt-2 text-sm font-semibold text-default">
                Lv.{{ user.level }} · {{ formatCount(user.totalExp) }} EXP
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                硬币余额
              </div>
              <div class="mt-2 text-sm font-semibold text-default">
                {{ formatCount(user.coinBalance) }}
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                注册时间
              </div>
              <div class="mt-2 text-sm font-semibold text-default">
                {{ formatDateTime(user.createdAt) }}
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-5 py-4">
              <div class="text-sm text-muted">
                最近登录
              </div>
              <div class="mt-2 text-sm font-semibold text-default">
                {{ formatDateTime(user.lastLoginAt) }}
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 grid gap-3 lg:grid-cols-2">
          <section class="rounded-[2rem] border border-default bg-muted/50 p-5">
            <div class="text-sm font-semibold text-default">
              封禁信息
            </div>
            <div class="mt-3 text-lg font-semibold text-default">
              {{ user.bannedUntilAt ? formatDateTime(user.bannedUntilAt) : '当前未封禁' }}
            </div>
            <div class="mt-2 text-sm leading-6 text-muted">
              {{ user.banReason || '暂无封禁原因' }}
            </div>
          </section>

          <section class="rounded-[2rem] border border-default bg-muted/50 p-5">
            <div class="text-sm font-semibold text-default">
              禁言信息
            </div>
            <div class="mt-3 text-lg font-semibold text-default">
              {{ user.mutedUntilAt ? formatDateTime(user.mutedUntilAt) : '当前未禁言' }}
            </div>
            <div class="mt-2 text-sm leading-6 text-muted">
              {{ user.muteReason || '暂无禁言原因' }}
            </div>
          </section>
        </div>
      </section>

      <section class="space-y-4">
        <div class="flex items-center justify-between gap-3">
          <h2 class="text-2xl font-semibold text-default">
            用户帖子
          </h2>
          <div class="text-sm text-muted">
            共 {{ formatCount(posts.total) }} 篇
          </div>
        </div>

        <article
          v-for="post in posts.list"
          :key="post.id"
          class="rounded-[2rem] border border-default bg-elevated/90 p-5 shadow-lg shadow-slate-900/5"
        >
          <NuxtLink
            :to="`/posts/${post.id}`"
            class="text-xl font-semibold text-default transition hover:text-primary"
          >
            {{ post.title }}
          </NuxtLink>
          <p class="mt-3 text-sm leading-7 text-muted">
            {{ truncateText(post.contentText, 200) || '该帖子仅包含图片内容。' }}
          </p>
          <div class="mt-4 flex flex-wrap gap-4 text-sm text-muted">
            <span>板块 {{ post.boardName }}</span>
            <span>评论 {{ formatCount(post.commentCount) }}</span>
            <span>点赞 {{ formatCount(post.likeCount) }}</span>
            <span>{{ formatDateTime(post.createdAt) }}</span>
          </div>
        </article>

        <EmptyState
          v-if="!posts.list.length"
          title="这个用户还没有帖子"
          description="发过的帖子会展示在这里。"
          icon="i-lucide-files"
        />

        <div v-if="posts.total > pageSize" class="flex justify-end">
          <UPagination
            :page="postsPage"
            :items-per-page="pageSize"
            :total="posts.total"
            @update:page="postsPage = $event"
          />
        </div>
      </section>

      <section
        v-if="isAdmin"
        class="rounded-[2rem] border border-amber-200 bg-amber-50 p-6 shadow-lg shadow-amber-900/5"
      >
        <div class="mb-5 text-2xl font-semibold text-amber-950">
          管理员操作
        </div>

        <div class="grid gap-6 lg:grid-cols-2">
          <section class="rounded-[2rem] border border-amber-200 bg-default/80 p-5">
            <div class="text-lg font-semibold text-default">
              封禁 / 解封
            </div>
            <div class="mt-4 grid gap-3">
              <UInput v-model="banForm.bannedUntilAt" type="datetime-local" :ui="{ base: 'rounded-2xl' }" />
              <UTextarea v-model="banForm.reason" :rows="3" placeholder="封禁原因" :ui="{ base: 'rounded-[2rem]' }" />
              <p class="text-xs leading-6 text-muted">
                留空表示永久封禁；原因最多 {{ MAX_ADMIN_NOTE_LENGTH }} 个字符。
              </p>
              <div class="flex flex-wrap gap-2">
                <UButton color="warning" class="rounded-full" @click="submitBan">
                  提交封禁
                </UButton>
                <UButton color="neutral" variant="outline" class="rounded-full" @click="unban">
                  解除封禁
                </UButton>
              </div>
            </div>
          </section>

          <section class="rounded-[2rem] border border-amber-200 bg-default/80 p-5">
            <div class="text-lg font-semibold text-default">
              禁言 / 解禁
            </div>
            <div class="mt-4 grid gap-3">
              <UInput v-model="muteForm.mutedUntilAt" type="datetime-local" :ui="{ base: 'rounded-2xl' }" />
              <UTextarea v-model="muteForm.reason" :rows="3" placeholder="禁言原因" :ui="{ base: 'rounded-[2rem]' }" />
              <p class="text-xs leading-6 text-muted">
                请填写未来时间；留空不会形成实际禁言效果。原因最多 {{ MAX_ADMIN_NOTE_LENGTH }} 个字符。
              </p>
              <div class="flex flex-wrap gap-2">
                <UButton color="warning" class="rounded-full" @click="submitMute">
                  提交禁言
                </UButton>
                <UButton color="neutral" variant="outline" class="rounded-full" @click="unmute">
                  解除禁言
                </UButton>
              </div>
            </div>
          </section>

          <section class="rounded-[2rem] border border-amber-200 bg-default/80 p-5">
            <div class="text-lg font-semibold text-default">
              调整经验
            </div>
            <div class="mt-4 grid gap-3">
              <UInput v-model="adjustExpForm.changeExp" type="number" :ui="{ base: 'rounded-2xl' }" />
              <UTextarea v-model="adjustExpForm.remark" :rows="3" placeholder="备注" :ui="{ base: 'rounded-[2rem]' }" />
              <p class="text-xs leading-6 text-muted">
                调整值不能为 0，备注最多 {{ MAX_ADMIN_NOTE_LENGTH }} 个字符。
              </p>
              <UButton color="warning" class="w-fit rounded-full" @click="adjustExp">
                提交经验调整
              </UButton>
            </div>
          </section>

          <section class="rounded-[2rem] border border-amber-200 bg-default/80 p-5">
            <div class="text-lg font-semibold text-default">
              调整硬币
            </div>
            <div class="mt-4 grid gap-3">
              <UInput v-model="adjustCoinForm.changeAmount" type="number" :ui="{ base: 'rounded-2xl' }" />
              <UTextarea v-model="adjustCoinForm.remark" :rows="3" placeholder="备注" :ui="{ base: 'rounded-[2rem]' }" />
              <p class="text-xs leading-6 text-muted">
                调整值不能为 0，备注最多 {{ MAX_ADMIN_NOTE_LENGTH }} 个字符。
              </p>
              <UButton color="warning" class="w-fit rounded-full" @click="adjustCoin">
                提交硬币调整
              </UButton>
            </div>
          </section>
        </div>
      </section>
    </template>

    <EmptyState
      v-else-if="!pending"
      title="用户不存在"
      description="该用户可能不存在，或当前链接参数不正确。"
      icon="i-lucide-user-x"
    />
  </div>
</template>
