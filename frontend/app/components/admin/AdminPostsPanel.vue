<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { BoardView, PageResult, PostCardView } from '~/types/forum'
import {
  displayName,
  formatCount,
  formatDateTime,
  initialsOf,
  truncateText
} from '~/utils/forum'

type BooleanFilterValue = 'all' | 'true' | 'false'

const emit = defineEmits<{
  refreshSummary: []
}>()

const NuxtLink = resolveComponent('NuxtLink')
const toast = useToast()
const api = useApi()

const PAGE_SIZE = 10

const boards = ref<BoardView[]>([])
const boardsPending = ref(false)
const postsPending = ref(false)
const postsPage = ref(1)
const posts = ref<PageResult<PostCardView>>(createEmptyPageResult<PostCardView>())
const postFilters = reactive<{
  keyword: string
  boardId: string
  pinned: BooleanFilterValue
  featured: BooleanFilterValue
}>({
  keyword: '',
  boardId: 'all',
  pinned: 'all',
  featured: 'all'
})
const postActionPendingIds = ref<number[]>([])
const postDeleteConfirmOpen = ref(false)
const postDeletePending = ref(false)
const postDeleteTarget = ref<PostCardView | null>(null)

const postBooleanItems = [
  { label: '全部', value: 'all' },
  { label: '是', value: 'true' },
  { label: '否', value: 'false' }
]

const postBoardItems = computed(() => [
  { label: '全部板块', value: 'all' },
  ...boards.value.map(board => ({
    label: board.name,
    value: String(board.id)
  }))
])

const postColumns = computed((): TableColumn<PostCardView>[] => [
  {
    id: 'title',
    header: '标题',
    cell: ({ row }) =>
      h('div', { class: 'min-w-0 max-w-xl' }, [
        h('div', { class: 'font-medium text-default' }, row.original.title),
        h(
          'div',
          { class: 'mt-1 text-sm leading-6 text-muted' },
          truncateText(row.original.contentText, 80) || '帖子仅包含图片内容。'
        )
      ])
  },
  {
    id: 'author',
    header: '作者',
    cell: ({ row }) => renderIdentityCell(row.original.author, `Lv.${row.original.author.level}`)
  },
  {
    accessorKey: 'boardName',
    header: '板块',
    cell: ({ row }) =>
      h('div', { class: 'text-sm font-medium text-default' }, row.original.boardName)
  },
  {
    id: 'metrics',
    header: '互动数据',
    cell: ({ row }) =>
      h('div', { class: 'space-y-1 text-sm text-muted' }, [
        h('div', `评价 ${formatCount(row.original.commentCount)}`),
        h('div', `点赞 ${formatCount(row.original.likeCount)}`),
        h('div', `打赏 ${formatCount(row.original.rewardCoinCount)}`)
      ])
  },
  {
    id: 'flags',
    header: '标记',
    cell: ({ row }) =>
      h('div', { class: 'flex flex-wrap gap-2' }, [
        row.original.pinned
          ? renderStatusPill('置顶', 'warning')
          : renderStatusPill('未置顶', 'neutral'),
        row.original.featured
          ? renderStatusPill('加精', 'success')
          : renderStatusPill('未加精', 'neutral')
      ])
  },
  {
    accessorKey: 'createdAt',
    header: '发布时间',
    cell: ({ row }) =>
      h('div', { class: 'text-sm text-muted' }, formatDateTime(row.original.createdAt))
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, '操作'),
    cell: ({ row }) => {
      const pending = isPostActionPending(row.original.id)

      return h('div', { class: 'flex flex-wrap justify-end gap-2' }, [
        renderTableButton(
          row.original.pinned ? '取消置顶' : '置顶',
          () => togglePostPin(row.original),
          'warning',
          pending
        ),
        renderTableButton(
          row.original.featured ? '取消加精' : '加精',
          () => togglePostFeatured(row.original),
          'success',
          pending
        ),
        renderTableButton(
          '删除',
          () => requestDeletePost(row.original),
          'danger',
          pending
        ),
        renderTableLink(`/posts/${row.original.id}`, '查看详情')
      ])
    }
  }
])

watch(postsPage, async (nextPage, previousPage) => {
  if (nextPage !== previousPage) {
    await loadPosts()
  }
})

onMounted(async () => {
  await Promise.all([loadBoards(), loadPosts()])
})

function createEmptyPageResult<T>(): PageResult<T> {
  return {
    total: 0,
    page: 1,
    pageSize: PAGE_SIZE,
    list: []
  }
}

function normalizeNullableText(value?: string | null) {
  const normalized = value?.trim() || ''
  return normalized || null
}

function booleanFilterValue(value: BooleanFilterValue) {
  if (value === 'all') {
    return undefined
  }

  return value === 'true'
}

async function loadBoards() {
  boardsPending.value = true

  try {
    boards.value = await api.request<BoardView[]>('/api/boards', {
      query: {
        includeDisabled: true
      }
    })

    if (postFilters.boardId !== 'all' && !boards.value.some(board => String(board.id) === postFilters.boardId)) {
      postFilters.boardId = 'all'
    }
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '板块列表加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    boardsPending.value = false
  }
}

async function loadPosts() {
  postsPending.value = true

  try {
    posts.value = await api.request<PageResult<PostCardView>>('/api/admin/posts', {
      query: {
        page: postsPage.value,
        pageSize: PAGE_SIZE,
        boardId: postFilters.boardId === 'all' ? undefined : Number(postFilters.boardId),
        keyword: normalizeNullableText(postFilters.keyword) || undefined,
        pinned: booleanFilterValue(postFilters.pinned),
        featured: booleanFilterValue(postFilters.featured)
      }
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '帖子列表加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    postsPending.value = false
  }
}

async function applyPostFilters() {
  postsPage.value = 1
  await loadPosts()
}

async function resetPostFilters() {
  postFilters.keyword = ''
  postFilters.boardId = 'all'
  postFilters.pinned = 'all'
  postFilters.featured = 'all'
  postsPage.value = 1
  await loadPosts()
}

function setPostPending(postId: number, pending: boolean) {
  if (pending) {
    if (!postActionPendingIds.value.includes(postId)) {
      postActionPendingIds.value.push(postId)
    }
    return
  }

  postActionPendingIds.value = postActionPendingIds.value.filter(id => id !== postId)
}

function isPostActionPending(postId: number) {
  return postActionPendingIds.value.includes(postId)
}

async function togglePostPin(post: PostCardView) {
  if (isPostActionPending(post.id)) {
    return
  }

  setPostPending(post.id, true)

  try {
    await api.request<void>(`/api/admin/posts/${post.id}/pin`, {
      method: 'PUT',
      body: {
        pinned: !post.pinned
      }
    })

    toast.add({
      title: post.pinned ? '已取消置顶' : '已设为置顶',
      color: 'success'
    })

    await loadPosts()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '置顶操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    setPostPending(post.id, false)
  }
}

async function togglePostFeatured(post: PostCardView) {
  if (isPostActionPending(post.id)) {
    return
  }

  setPostPending(post.id, true)

  try {
    await api.request<void>(`/api/admin/posts/${post.id}/feature`, {
      method: 'PUT',
      body: {
        featured: !post.featured
      }
    })

    toast.add({
      title: post.featured ? '已取消加精' : '已设为加精',
      color: 'success'
    })

    await loadPosts()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '加精操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    setPostPending(post.id, false)
  }
}

function requestDeletePost(post: PostCardView) {
  postDeleteTarget.value = post
  postDeleteConfirmOpen.value = true
}

async function confirmDeletePost() {
  if (!postDeleteTarget.value) {
    return
  }

  const targetPost = postDeleteTarget.value
  postDeletePending.value = true
  setPostPending(targetPost.id, true)

  try {
    await api.request<void>(`/api/admin/posts/${targetPost.id}`, {
      method: 'DELETE'
    })

    toast.add({
      title: '帖子已删除',
      color: 'success'
    })

    postDeleteConfirmOpen.value = false
    postDeleteTarget.value = null
    emit('refreshSummary')

    if (posts.value.list.length === 1 && postsPage.value > 1) {
      postsPage.value -= 1
    } else {
      await loadPosts()
    }
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '删除帖子失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    postDeletePending.value = false
    setPostPending(targetPost.id, false)
  }
}

function renderIdentityCell(user: PostCardView['author'], subtitle: string) {
  return h('div', { class: 'flex min-w-0 items-center gap-3' }, [
    renderAvatar(user),
    h('div', { class: 'min-w-0' }, [
      h('div', { class: 'truncate font-medium text-default' }, displayName(user)),
      h('div', { class: 'truncate text-sm text-muted' }, subtitle)
    ])
  ])
}

function renderAvatar(user: PostCardView['author']) {
  if (user.avatarUrl) {
    return h('img', {
      src: user.avatarUrl,
      alt: displayName(user),
      class: 'size-10 rounded-full object-cover ring-1 ring-default'
    })
  }

  return h(
    'div',
    {
      class: 'flex size-10 items-center justify-center rounded-full bg-primary text-sm font-semibold text-white'
    },
    initialsOf(user)
  )
}

function renderStatusPill(label: string, tone: 'success' | 'warning' | 'danger' | 'neutral') {
  const toneClass =
    tone === 'success'
      ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
      : tone === 'warning'
        ? 'border-amber-200 bg-amber-50 text-amber-700'
        : tone === 'danger'
          ? 'border-rose-200 bg-rose-50 text-rose-700'
          : 'border-default bg-muted text-muted'

  return h(
    'span',
    {
      class: `inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold ${toneClass}`
    },
    label
  )
}

function renderTableButton(label: string, onClick: () => void, tone: 'neutral' | 'success' | 'warning' | 'danger' = 'neutral', pending = false) {
  const toneClass =
    tone === 'success'
      ? 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
      : tone === 'warning'
        ? 'border-amber-200 text-amber-700 hover:bg-amber-50'
        : tone === 'danger'
          ? 'border-rose-200 text-rose-700 hover:bg-rose-50'
          : 'border-default text-default hover:bg-muted/50'

  return h(
    'button',
    {
      type: 'button',
      disabled: pending,
      class: `inline-flex h-9 items-center justify-center rounded-full border px-3 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 ${toneClass}`,
      onClick: (event: Event) => {
        event.preventDefault()
        event.stopPropagation()
        onClick()
      }
    },
    pending ? '处理中...' : label
  )
}

function renderTableLink(to: string, label: string) {
  return h(
    NuxtLink as any,
    {
      to,
      class: 'inline-flex h-9 items-center justify-center rounded-full border border-default px-3 text-sm font-medium text-default transition hover:bg-muted/50'
    },
    {
      default: () => label
    }
  )
}
</script>

<template>
  <section class="rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-xl shadow-slate-900/5">
    <div class="border-b border-default pb-5">
      <h2 class="text-2xl font-semibold text-default">
        帖子管理
      </h2>
      <p class="mt-2 text-sm leading-7 text-muted">
        后台列表只覆盖正常状态帖子，支持直接筛选、置顶、加精、删除和跳转详情。
      </p>
    </div>

    <form
      class="mt-5 grid gap-3 xl:grid-cols-[minmax(0,1.4fr),12rem,10rem,10rem,auto,auto]"
      @submit.prevent="applyPostFilters"
    >
      <UInput
        v-model="postFilters.keyword"
        placeholder="搜索标题、正文或作者"
        icon="i-lucide-search"
        color="neutral"
        variant="outline"
        :ui="{ base: 'rounded-2xl' }"
      />
      <USelect
        v-model="postFilters.boardId"
        :items="postBoardItems"
        color="neutral"
        variant="outline"
        :loading="boardsPending"
      />
      <USelect
        v-model="postFilters.pinned"
        :items="postBooleanItems"
        color="neutral"
        variant="outline"
      />
      <USelect
        v-model="postFilters.featured"
        :items="postBooleanItems"
        color="neutral"
        variant="outline"
      />
      <UButton
        type="submit"
        class="rounded-full"
        :loading="postsPending"
      >
        查询
      </UButton>
      <UButton
        type="button"
        color="neutral"
        variant="outline"
        class="rounded-full"
        @click="resetPostFilters"
      >
        重置
      </UButton>
    </form>

    <div class="mt-5 text-sm text-muted">
      共 {{ formatCount(posts.total) }} 篇正常状态帖子
    </div>

    <div class="mt-4 overflow-hidden rounded-[1.75rem] border border-default bg-default">
      <UTable
        :data="posts.list"
        :columns="postColumns"
        :loading="postsPending"
        loading-color="primary"
        loading-animation="carousel"
        empty="当前条件下没有帖子。"
        sticky="header"
        class="max-h-[42rem]"
      />
    </div>

    <div
      v-if="posts.total > posts.pageSize"
      class="mt-5 flex justify-end"
    >
      <UPagination
        :page="postsPage"
        :items-per-page="posts.pageSize"
        :total="posts.total"
        :show-controls="true"
        :show-edges="true"
        @update:page="postsPage = $event"
      />
    </div>

    <UModal
      v-model:open="postDeleteConfirmOpen"
      title="确认删除帖子"
      :description="postDeleteTarget ? `删除后，这篇帖子将不再出现在站内列表中。目标帖子：${postDeleteTarget.title}` : '删除后，这篇帖子将不再出现在站内列表中。'"
      :ui="{ content: 'max-w-lg rounded-[2rem]' }"
    >
      <template #footer>
        <div class="flex w-full justify-end gap-2">
          <UButton
            color="neutral"
            variant="outline"
            class="rounded-full"
            @click="postDeleteConfirmOpen = false"
          >
            取消
          </UButton>
          <UButton
            color="error"
            class="rounded-full"
            :loading="postDeletePending"
            @click="confirmDeletePost"
          >
            确认删除
          </UButton>
        </div>
      </template>
    </UModal>
  </section>
</template>
