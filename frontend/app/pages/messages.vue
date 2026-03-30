<script setup lang="ts">
import type {
  ConversationDetailView,
  ConversationListView,
  MessageView,
  PageResult
} from '~/types/forum'
import {
  arrayToLines,
  displayName,
  findFirstTooLongEntry,
  formatDateTime,
  linesToArray,
  normalizeNullableText
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

const route = useRoute()
const toast = useToast()
const auth = useAuth()
const api = useApi()
const { refreshUnreadCount } = useForumMeta()
const currentUserId = computed(() => auth.profile.value?.id || 0)
const autoRefreshMs = 20000

const listPageSize = 12
const detailPageSize = 20

const listPage = ref(1)
const detailPage = ref(1)
const conversations = ref(createEmptyPage<ConversationListView>(listPageSize))
const selectedConversationId = ref<number | null>(null)
const conversationDetail = ref<ConversationDetailView | null>(null)
const listLoading = ref(false)
const detailLoading = ref(false)
const lastSyncedAt = ref<string | null>(null)
const composer = reactive({
  contentText: '',
  imageUrlsText: ''
})
let pollingTimer: number | null = null

function getSelectedIdFromRoute() {
  const rawValue = Array.isArray(route.query.id) ? route.query.id[0] : route.query.id
  const parsed = Number(rawValue || 0)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

async function selectConversation(id: number) {
  await navigateTo({
    path: '/messages',
    query: {
      id
    }
  })
}

async function loadConversations(showToast = true) {
  listLoading.value = true

  try {
    conversations.value = await api.request<PageResult<ConversationListView>>('/api/conversations', {
      query: {
        page: listPage.value,
        pageSize: listPageSize
      }
    })

    if (!selectedConversationId.value && conversations.value.list[0]) {
      await selectConversation(conversations.value.list[0].id)
    }
  } catch (error) {
    if (showToast) {
      const apiError = error as { message?: string }

      toast.add({
        title: '会话列表加载失败',
        description: apiError.message || '请稍后重试',
        color: 'error'
      })
    }
  } finally {
    listLoading.value = false
  }
}

async function loadConversationDetail(showToast = true) {
  if (!selectedConversationId.value) {
    conversationDetail.value = null
    return
  }

  detailLoading.value = true

  try {
    const detail = await api.request<ConversationDetailView>(`/api/conversations/${selectedConversationId.value}`, {
      query: {
        page: detailPage.value,
        pageSize: detailPageSize
      }
    })

    conversationDetail.value = detail

    if (detail.unreadCount > 0) {
      await api.request<void>(`/api/conversations/${selectedConversationId.value}/read`, {
        method: 'POST'
      })

      conversationDetail.value.unreadCount = 0
      await Promise.all([loadConversations(), refreshUnreadCount().catch(() => 0)])
    }
  } catch (error) {
    if (showToast) {
      const apiError = error as { message?: string }

      toast.add({
        title: '会话详情加载失败',
        description: apiError.message || '请稍后重试',
        color: 'error'
      })
    }
  } finally {
    detailLoading.value = false
  }
}

async function sendMessage() {
  if (!selectedConversationId.value) {
    return
  }

  const imageUrls = linesToArray(composer.imageUrlsText)
  const contentText = normalizeNullableText(composer.contentText)
  const tooLongImageUrl = findFirstTooLongEntry(imageUrls, 255)

  if (!contentText && !imageUrls.length) {
    toast.add({
      title: '消息正文和图片 URL 不能同时为空',
      color: 'warning'
    })
    return
  }

  if (tooLongImageUrl) {
    toast.add({
      title: '图片 URL 不能超过 255 个字符',
      description: tooLongImageUrl,
      color: 'warning'
    })
    return
  }

  try {
    await api.request<MessageView>(`/api/conversations/${selectedConversationId.value}/messages`, {
      method: 'POST',
      body: {
        contentText,
        imageUrls: imageUrls.length ? imageUrls : null
      }
    })

    composer.contentText = ''
    composer.imageUrlsText = ''

    await refreshMessages()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '发送消息失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function toggleSetting(type: 'pin' | 'mute') {
  if (!conversationDetail.value) {
    return
  }

  try {
    await api.request<void>(`/api/conversations/${conversationDetail.value.id}/${type}`, {
      method: 'PUT',
      body: {
        value: type === 'pin' ? !conversationDetail.value.pinned : !conversationDetail.value.muted
      }
    })

    await Promise.all([loadConversations(), loadConversationDetail()])
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: type === 'pin' ? '会话置顶失败' : '免打扰设置失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function deleteConversation() {
  if (!conversationDetail.value) {
    return
  }

  try {
    await api.request<void>(`/api/conversations/${conversationDetail.value.id}`, {
      method: 'DELETE'
    })

    conversationDetail.value = null
    selectedConversationId.value = null

    await navigateTo('/messages')
    await Promise.all([loadConversations(), refreshUnreadCount().catch(() => 0)])
    lastSyncedAt.value = new Date().toISOString()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '删除会话失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function refreshMessages(showToast = true) {
  await Promise.all([
    loadConversations(showToast),
    loadConversationDetail(showToast),
    refreshUnreadCount().catch(() => 0)
  ])
  lastSyncedAt.value = new Date().toISOString()
}

function stopPolling() {
  if (!pollingTimer) {
    return
  }

  clearInterval(pollingTimer)
  pollingTimer = null
}

function startPolling() {
  if (!import.meta.client || pollingTimer) {
    return
  }

  pollingTimer = window.setInterval(async () => {
    if (document.hidden || listLoading.value || detailLoading.value) {
      return
    }

    await refreshMessages(false)
  }, autoRefreshMs)
}

watch(
  () => route.query.id,
  async () => {
    selectedConversationId.value = getSelectedIdFromRoute()
    detailPage.value = 1
    await loadConversationDetail()
    lastSyncedAt.value = new Date().toISOString()
  },
  { immediate: true }
)

watch(listPage, () => loadConversations())
watch(detailPage, () => loadConversationDetail())

onMounted(async () => {
  await refreshMessages()
  startPolling()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="grid gap-6 xl:grid-cols-[22rem,1fr]">
    <section class="rounded-[2rem] border border-default bg-elevated/90 p-5 shadow-xl shadow-slate-900/5">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <div class="text-xl font-semibold text-default">
            会话列表
          </div>
          <div class="mt-1 text-sm text-muted">
            排序规则：置顶优先，其次最新消息时间。页面每 20 秒自动轮询一次。
          </div>
        </div>

        <UButton color="neutral" variant="outline" class="rounded-full" @click="refreshMessages()">
          刷新
        </UButton>
      </div>

      <div class="space-y-3">
        <button
          v-for="conversation in conversations.list"
          :key="conversation.id"
          type="button"
          class="w-full rounded-[1.5rem] border px-4 py-4 text-left transition"
          :class="selectedConversationId === conversation.id ? 'border-primary bg-primary text-white shadow-lg shadow-primary/20' : 'border-default bg-muted/50 text-default hover:bg-default'"
          @click="selectConversation(conversation.id)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <div class="truncate text-sm font-semibold">
                  {{ displayName(conversation.peerUser) }}
                </div>
                <span
                  v-if="conversation.pinned"
                  class="rounded-full px-2 py-0.5 text-[10px] font-semibold"
                  :class="selectedConversationId === conversation.id ? 'bg-white/15 text-white' : 'bg-amber-100 text-amber-800'"
                >
                  置顶
                </span>
                <span
                  v-if="conversation.muted"
                  class="rounded-full px-2 py-0.5 text-[10px] font-semibold"
                  :class="selectedConversationId === conversation.id ? 'bg-white/15 text-white' : 'bg-muted text-muted'"
                >
                  免打扰
                </span>
              </div>

              <div class="mt-2 line-clamp-2 text-sm opacity-80">
                {{ conversation.lastMessage?.contentText || arrayToLines(conversation.lastMessage?.imageUrls).slice(0, 80) || '还没有消息' }}
              </div>
            </div>

            <div class="shrink-0 text-right text-xs opacity-70">
              <div>{{ conversation.updatedAt ? formatDateTime(conversation.updatedAt) : '暂无更新' }}</div>
              <div
                v-if="conversation.unreadCount"
                class="mt-2 inline-flex rounded-full bg-rose-500 px-2 py-0.5 font-semibold text-white"
              >
                {{ conversation.unreadCount }}
              </div>
            </div>
          </div>
        </button>

        <div v-if="listLoading" class="h-20 animate-pulse rounded-[1.5rem] border border-default bg-muted/50" />

        <EmptyState
          v-if="!conversations.list.length && !listLoading"
          title="还没有会话"
          description="你和其他用户建立的一对一私信会话会出现在这里。"
          icon="i-lucide-message-square"
        />
      </div>

      <div v-if="conversations.total > listPageSize" class="mt-5 flex justify-end">
        <UPagination
          :page="listPage"
          :items-per-page="listPageSize"
          :total="conversations.total"
          @update:page="listPage = $event"
        />
      </div>
    </section>

    <section class="rounded-[2rem] border border-default bg-elevated/90 p-5 shadow-xl shadow-slate-900/5">
      <template v-if="conversationDetail">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-default pb-4">
          <div>
            <div class="text-2xl font-semibold text-default">
              {{ displayName(conversationDetail.peerUser) }}
            </div>
            <div class="mt-1 text-sm text-muted">
              当前页消息按时间升序显示，但分页是以最新页为 `page = 1`。
            </div>
            <div class="mt-1 text-xs text-muted">
              上次同步 {{ formatDateTime(lastSyncedAt) }}
            </div>
          </div>

          <div class="flex flex-wrap gap-2">
            <UButton color="neutral" variant="outline" class="rounded-full" @click="toggleSetting('pin')">
              {{ conversationDetail.pinned ? '取消置顶' : '置顶会话' }}
            </UButton>
            <UButton color="neutral" variant="outline" class="rounded-full" @click="toggleSetting('mute')">
              {{ conversationDetail.muted ? '取消免打扰' : '设为免打扰' }}
            </UButton>
            <UButton color="error" variant="outline" class="rounded-full" @click="deleteConversation">
              删除会话
            </UButton>
          </div>
        </div>

        <div class="mt-5 rounded-[2rem] border border-default bg-muted/50 p-4">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div class="text-sm text-muted">
              当前查看第 {{ detailPage }} 页消息
            </div>
            <div class="flex gap-2">
              <UButton
                color="neutral"
                variant="outline"
                class="rounded-full"
                @click="refreshMessages()"
              >
                更新
              </UButton>
              <UButton
                color="neutral"
                variant="outline"
                class="rounded-full"
                :disabled="detailPage === 1"
                @click="detailPage -= 1"
              >
                上一页
              </UButton>
              <UButton
                color="neutral"
                variant="outline"
                class="rounded-full"
                :disabled="conversationDetail.messages.length < detailPageSize"
                @click="detailPage += 1"
              >
                下一页
              </UButton>
            </div>
          </div>

          <div class="space-y-3">
            <div
              v-for="message in conversationDetail.messages"
              :key="message.id"
              class="flex"
              :class="message.senderUserId === currentUserId ? 'justify-end' : 'justify-start'"
            >
              <div
                class="max-w-[80%] rounded-[1.5rem] px-4 py-3 text-sm shadow-sm"
                :class="message.senderUserId === currentUserId ? 'bg-primary text-white' : 'border border-default bg-default text-default'"
              >
                <div class="whitespace-pre-wrap leading-7">
                  {{ message.contentText || '仅包含图片 URL' }}
                </div>

                <div
                  v-if="message.imageUrls.length"
                  class="mt-3 space-y-2"
                >
                  <a
                    v-for="imageUrl in message.imageUrls"
                    :key="imageUrl"
                    :href="imageUrl"
                    target="_blank"
                    rel="noreferrer"
                    class="block truncate text-xs underline opacity-80"
                  >
                    {{ imageUrl }}
                  </a>
                </div>

                <div class="mt-2 text-[11px] opacity-70">
                  {{ formatDateTime(message.createdAt) }}
                </div>
              </div>
            </div>

            <div v-if="detailLoading" class="h-24 animate-pulse rounded-[1.5rem] border border-default bg-default" />

            <EmptyState
              v-if="!conversationDetail.messages.length && !detailLoading"
              title="当前页没有消息"
              description="发送第一条消息后，这里会显示当前分页切片内的内容。"
              icon="i-lucide-send-horizontal"
            />
          </div>
        </div>

        <div class="mt-5 space-y-3">
          <UTextarea
            v-model="composer.contentText"
            :rows="5"
            placeholder="输入消息内容"
            :ui="{ base: 'rounded-[2rem]' }"
          />
          <UTextarea
            v-model="composer.imageUrlsText"
            :rows="2"
            placeholder="可选：每行一个图片 URL"
            :ui="{ base: 'rounded-[2rem]' }"
          />

          <div class="flex justify-end">
            <UButton class="rounded-full bg-primary text-white hover:bg-primary/90" @click="sendMessage">
              发送消息
            </UButton>
          </div>
        </div>
      </template>

      <EmptyState
        v-else
        title="请选择一个会话"
        description="从左侧会话列表中选择一个对象，即可查看历史消息并继续发送私信。"
        icon="i-lucide-panel-left-open"
      />
    </section>
  </div>
</template>
