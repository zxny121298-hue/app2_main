<script setup lang="ts">
import type {
  NotificationGroupView,
  NotificationItemView,
  PageResult
} from '~/types/forum'
import {
  displayName,
  formatDateTime,
  getNotificationLink,
  notificationEventLabels
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
const api = useApi()
const { refreshUnreadCount } = useForumMeta()
const autoRefreshMs = 20000

const groupPageSize = 12
const itemPageSize = 12

const groupPage = ref(1)
const itemPage = ref(1)
const selectedGroupId = ref<number | null>(null)
const groups = ref(createEmptyPage<NotificationGroupView>(groupPageSize))
const items = ref(createEmptyPage<NotificationItemView>(itemPageSize))
const groupsLoading = ref(false)
const itemsLoading = ref(false)
const lastSyncedAt = ref<string | null>(null)
let pollingTimer: number | null = null

function getSelectedIdFromRoute() {
  const rawValue = Array.isArray(route.query.id) ? route.query.id[0] : route.query.id
  const parsed = Number(rawValue || 0)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

async function selectGroup(id: number) {
  await navigateTo({
    path: '/notifications',
    query: {
      id
    }
  })
}

async function loadGroups(showToast = true) {
  groupsLoading.value = true

  try {
    groups.value = await api.request<PageResult<NotificationGroupView>>('/api/notifications/groups', {
      query: {
        page: groupPage.value,
        pageSize: groupPageSize
      }
    })

    if (!selectedGroupId.value && groups.value.list[0]) {
      await selectGroup(groups.value.list[0].id)
    }
  } catch (error) {
    if (showToast) {
      const apiError = error as { message?: string }
      toast.add({
        title: '通知列表加载失败',
        description: apiError.message || '请稍后重试',
        color: 'error'
      })
    }
  } finally {
    groupsLoading.value = false
  }
}

async function loadItems(showToast = true) {
  if (!selectedGroupId.value) {
    items.value = createEmptyPage<NotificationItemView>(itemPageSize)
    return
  }

  itemsLoading.value = true

  try {
    items.value = await api.request<PageResult<NotificationItemView>>(`/api/notifications/groups/${selectedGroupId.value}/items`, {
      query: {
        page: itemPage.value,
        pageSize: itemPageSize
      }
    })
  } catch (error) {
    if (showToast) {
      const apiError = error as { message?: string }
      toast.add({
        title: '通知明细加载失败',
        description: apiError.message || '请稍后重试',
        color: 'error'
      })
    }
  } finally {
    itemsLoading.value = false
  }
}

async function markItemRead(item: NotificationItemView) {
  try {
    await api.request<void>(`/api/notifications/items/${item.id}/read`, {
      method: 'POST'
    })

    await refreshNotifications()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '标记已读失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function markGroupRead() {
  if (!selectedGroupId.value) {
    return
  }

  try {
    await api.request<void>(`/api/notifications/groups/${selectedGroupId.value}/read`, {
      method: 'POST'
    })

    await refreshNotifications()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '整组标记已读失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function markAllRead() {
  try {
    await api.request<void>('/api/notifications/read-all', {
      method: 'POST'
    })

    await refreshNotifications()
  } catch (error) {
    const apiError = error as { message?: string }
    toast.add({
      title: '全部已读失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function refreshNotifications(showToast = true) {
  await Promise.all([
    loadGroups(showToast),
    loadItems(showToast),
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
    if (document.hidden || groupsLoading.value || itemsLoading.value) {
      return
    }

    await refreshNotifications(false)
  }, autoRefreshMs)
}

watch(
  () => route.query.id,
  async () => {
    selectedGroupId.value = getSelectedIdFromRoute()
    itemPage.value = 1
    await loadItems()
    lastSyncedAt.value = new Date().toISOString()
  },
  { immediate: true }
)

watch(groupPage, () => loadGroups())
watch(itemPage, () => loadItems())

onMounted(async () => {
  await refreshNotifications()
  startPolling()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="grid gap-6 xl:grid-cols-[22rem,1fr]">
    <section class="rounded-[2rem] border border-default bg-elevated/90 p-5 shadow-xl shadow-slate-900/5">
      <div class="mb-4 flex items-start justify-between gap-3">
        <div>
          <div class="text-xl font-semibold text-default">
            通知分组
          </div>
          <div class="mt-1 text-sm text-muted">
            相同目标和事件类型会被聚合到同一组。页面每 20 秒自动轮询一次。
          </div>
        </div>

        <div class="flex flex-wrap gap-2">
          <UButton color="neutral" variant="outline" class="rounded-full" @click="refreshNotifications()">
            刷新
          </UButton>
          <UButton color="neutral" variant="outline" class="rounded-full" @click="markAllRead">
            全部已读
          </UButton>
        </div>
      </div>

      <div class="space-y-3">
        <button
          v-for="group in groups.list"
          :key="group.id"
          type="button"
          class="w-full rounded-[1.5rem] border px-4 py-4 text-left transition"
          :class="selectedGroupId === group.id ? 'border-primary bg-primary text-white shadow-lg shadow-primary/20' : 'border-default bg-muted/50 text-default hover:bg-default'"
          @click="selectGroup(group.id)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div class="text-sm font-semibold">
                {{ notificationEventLabels[group.eventType] }}
              </div>
              <div class="mt-2 text-sm opacity-80">
                最近触发者：{{ displayName(group.latestActor) }}
              </div>
              <div class="mt-2 text-xs opacity-70">
                最新时间 {{ formatDateTime(group.latestAt) }}
              </div>
            </div>
            <div class="shrink-0 text-right">
              <div class="rounded-full px-2 py-0.5 text-xs font-semibold"
                :class="selectedGroupId === group.id ? 'bg-white/15 text-white' : 'bg-rose-100 text-rose-700'">
                未读 {{ group.unreadCount }}
              </div>
              <div class="mt-2 text-xs opacity-70">
                共 {{ group.totalCount }} 条
              </div>
            </div>
          </div>
        </button>

        <div v-if="groupsLoading" class="h-20 animate-pulse rounded-[1.5rem] border border-default bg-muted/50" />

        <EmptyState
          v-if="!groups.list.length && !groupsLoading"
          title="还没有通知"
          description="评论、点赞、打赏和私信都会产生通知。"
          icon="i-lucide-bell"
        />
      </div>

      <div v-if="groups.total > groupPageSize" class="mt-5 flex justify-end">
        <UPagination
          :page="groupPage"
          :items-per-page="groupPageSize"
          :total="groups.total"
          @update:page="groupPage = $event"
        />
      </div>
    </section>

    <section class="rounded-[2rem] border border-default bg-elevated/90 p-5 shadow-xl shadow-slate-900/5">
      <template v-if="selectedGroupId">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-default pb-4">
          <div>
            <div class="text-2xl font-semibold text-default">
              通知明细
            </div>
            <div class="mt-1 text-sm text-muted">
              按 `createdAt desc` 排序展示当前组的通知项。
            </div>
            <div class="mt-1 text-xs text-muted">
              上次同步 {{ formatDateTime(lastSyncedAt) }}
            </div>
          </div>

          <UButton color="neutral" variant="outline" class="rounded-full" @click="markGroupRead">
            整组标记已读
          </UButton>
        </div>

        <div class="mt-5 space-y-3">
          <article
            v-for="item in items.list"
            :key="item.id"
            class="rounded-[1.5rem] border px-4 py-4"
            :class="item.read ? 'border-default bg-muted/50' : 'border-primary/25 bg-primary/10'"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-default">
                  {{ notificationEventLabels[item.eventType] }}
                </div>
                <div class="mt-2 text-sm text-muted">
                  触发者：{{ displayName(item.actor) }} · {{ formatDateTime(item.createdAt) }}
                </div>
              </div>

              <div class="flex flex-wrap gap-2">
                <UButton
                  :to="getNotificationLink(item)"
                  color="neutral"
                  variant="outline"
                  class="rounded-full"
                >
                  打开目标
                </UButton>
                <UButton
                  v-if="!item.read"
                  color="neutral"
                  variant="outline"
                  class="rounded-full"
                  @click="markItemRead(item)"
                >
                  标记已读
                </UButton>
              </div>
            </div>
          </article>

          <div v-if="itemsLoading" class="h-20 animate-pulse rounded-[1.5rem] border border-default bg-muted/50" />

          <EmptyState
            v-if="!items.list.length && !itemsLoading"
            title="当前组没有通知项"
            description="切换左侧其他分组，或等待新的通知产生。"
            icon="i-lucide-bell-off"
          />
        </div>

        <div v-if="items.total > itemPageSize" class="mt-5 flex justify-end">
          <UPagination
            :page="itemPage"
            :items-per-page="itemPageSize"
            :total="items.total"
            @update:page="itemPage = $event"
          />
        </div>
      </template>

      <EmptyState
        v-else
        title="请选择一个通知分组"
        description="左侧会展示按目标聚合后的通知组，点击后可查看具体明细。"
        icon="i-lucide-panel-left-open"
      />
    </section>
  </div>
</template>
