<script setup lang="ts">
import type { CommentView, PageResult, PostDetailView } from '~/types/forum'
import {
  displayName,
  findFirstTooLongEntry,
  formatCount,
  formatDateTime,
  initialsOf,
  normalizeNullableText
} from '~/utils/forum'

type ReplyTarget = {
  commentId: number
  userId: number
  authorName: string
}

const route = useRoute()
const toast = useToast()
const auth = useAuth()
const api = useApi()
const { uploadImage, accept: imageAccept, maxBytes: maxUploadBytes } = useImageUpload()
const isLoggedIn = computed(() => auth.isLoggedIn.value)
const isAdmin = computed(() => auth.isAdmin.value)
const maxUploadSizeMb = Math.floor(maxUploadBytes / (1024 * 1024))

const post = ref<PostDetailView | null>(null)
const errorMessage = ref('')
const postLoading = ref(false)
const commentsLoading = ref(false)
const rewardOpen = ref(false)
const rewardAmount = ref(1)
const composerPending = ref(false)
const composerUploadPending = ref(false)
const composerExpanded = ref(false)
const replyTarget = ref<ReplyTarget | null>(null)
const composerRef = ref<{ focusInput: () => void } | null>(null)
const draft = reactive({
  contentText: '',
  imageUrls: [] as string[]
})

const commentsPage = ref(1)
const commentsPageSize = 10
const comments = ref<PageResult<CommentView>>({
  total: 0,
  page: 1,
  pageSize: commentsPageSize,
  list: []
})

const postId = computed(() => Number(route.params.id))
const loginRedirect = computed(() => `/login?redirect=/posts/${postId.value}`)
const composerPaddingStyle = computed(() => ({
  '--composer-padding-mobile': composerExpanded.value ? '24rem' : '7rem',
  '--composer-padding-desktop': composerExpanded.value ? '28rem' : '8rem'
}))

function resetComposer() {
  draft.contentText = ''
  draft.imageUrls = []
  composerExpanded.value = false
  replyTarget.value = null
  composerUploadPending.value = false
}

function setComposerImageUrls(urls: string[]) {
  draft.imageUrls = [...urls]
}

function appendComposerImageUrls(urls: string[]) {
  const merged = [...draft.imageUrls]

  for (const url of urls) {
    if (!merged.includes(url)) {
      merged.push(url)
    }
  }

  setComposerImageUrls(merged)
}

function removeComposerImageUrl(targetUrl: string) {
  setComposerImageUrls(draft.imageUrls.filter(url => url !== targetUrl))
}

async function focusComposer() {
  await nextTick()

  if (import.meta.client) {
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        composerRef.value?.focusInput()
      })
    })
  } else {
    composerRef.value?.focusInput()
  }
}

async function openComposer() {
  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  composerExpanded.value = true
  await focusComposer()
}

async function handleReply(payload: ReplyTarget) {
  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  replyTarget.value = payload
  composerExpanded.value = true
  await focusComposer()
}

function collapseComposer() {
  if (composerPending.value || composerUploadPending.value) {
    return
  }

  composerExpanded.value = false
  replyTarget.value = null
}

async function handleComposerImageSelection(files: File[]) {
  if (!files.length) {
    return
  }

  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  composerUploadPending.value = true
  const uploadedUrls: string[] = []
  let failedCount = 0
  let lastErrorMessage = ''

  try {
    for (const file of files) {
      try {
        const uploaded = await uploadImage(file, 'comment')
        uploadedUrls.push(uploaded.url)
      } catch (error) {
        failedCount += 1
        lastErrorMessage = error instanceof Error ? error.message : '请稍后重试'
      }
    }

    if (uploadedUrls.length) {
      appendComposerImageUrls(uploadedUrls)

      toast.add({
        title: `已上传 ${uploadedUrls.length} 张评价图片`,
        color: 'success'
      })
    }

    if (failedCount) {
      toast.add({
        title: `${failedCount} 张评价图片上传失败`,
        description: lastErrorMessage || '请稍后重试',
        color: 'warning'
      })
    }
  } finally {
    composerUploadPending.value = false
  }
}

async function loadPost() {
  postLoading.value = true
  errorMessage.value = ''

  try {
    post.value = await api.request<PostDetailView>(`/api/posts/${postId.value}`)
  } catch (error) {
    const apiError = error as { message?: string }
    errorMessage.value = apiError.message || '帖子详情加载失败'
  } finally {
    postLoading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true

  try {
    comments.value = await api.request<PageResult<CommentView>>(`/api/posts/${postId.value}/comments`, {
      query: {
        page: commentsPage.value,
        pageSize: commentsPageSize
      }
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '评价加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    commentsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadPost(), loadComments()])
}

async function handleToggleLike() {
  if (!post.value) {
    return
  }

  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  try {
    await api.request<void>(`/api/posts/${post.value.id}/like`, {
      method: post.value.liked ? 'DELETE' : 'POST'
    })

    post.value.liked = !post.value.liked
    post.value.likeCount += post.value.liked ? 1 : -1
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '帖子点赞失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function handleToggleFavorite() {
  if (!post.value) {
    return
  }

  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  try {
    await api.request<void>(`/api/posts/${post.value.id}/favorite`, {
      method: post.value.favorited ? 'DELETE' : 'POST'
    })

    post.value.favorited = !post.value.favorited
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '帖子收藏失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function handleReward() {
  if (!post.value) {
    return
  }

  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  if (!Number.isFinite(Number(rewardAmount.value)) || Number(rewardAmount.value) < 1) {
    toast.add({
      title: '打赏数量至少为 1',
      color: 'warning'
    })
    return
  }

  try {
    await api.request<void>(`/api/posts/${post.value.id}/reward`, {
      method: 'POST',
      body: {
        coinAmount: Number(rewardAmount.value)
      }
    })

    rewardOpen.value = false
    toast.add({
      title: '帖子打赏成功',
      color: 'success'
    })

    await loadPost()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '帖子打赏失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function submitComment() {
  if (!post.value) {
    return
  }

  if (!isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  const imageUrls = [...draft.imageUrls]
  const contentText = normalizeNullableText(draft.contentText)
  const tooLongImageUrl = findFirstTooLongEntry(imageUrls, 255)
  const activeReplyTarget = replyTarget.value

  if (!contentText && !imageUrls.length) {
    toast.add({
      title: activeReplyTarget ? '回复内容和图片不能同时为空' : '评价内容和图片不能同时为空',
      color: 'warning'
    })
    return
  }

  if (tooLongImageUrl) {
    toast.add({
      title: '图片地址不能超过 255 个字符',
      description: tooLongImageUrl,
      color: 'warning'
    })
    return
  }

  composerPending.value = true

  try {
    await api.request<CommentView>(`/api/posts/${post.value.id}/comments`, {
      method: 'POST',
      body: {
        parentCommentId: activeReplyTarget?.commentId || null,
        replyToUserId: activeReplyTarget?.userId || null,
        contentText,
        imageUrls: imageUrls.length ? imageUrls : null
      }
    })

    resetComposer()

    toast.add({
      title: activeReplyTarget ? '回复已发布' : '评价已发布',
      color: 'success'
    })

    if (!activeReplyTarget) {
      commentsPage.value = 1
    }

    await refreshAll()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: activeReplyTarget ? '回复失败' : '发表评价失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    composerPending.value = false
  }
}

async function togglePin() {
  if (!post.value) {
    return
  }

  try {
    await api.request<void>(`/api/admin/posts/${post.value.id}/pin`, {
      method: 'PUT',
      body: {
        pinned: !post.value.pinned
      }
    })

    await loadPost()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '置顶操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function toggleFeatured() {
  if (!post.value) {
    return
  }

  try {
    await api.request<void>(`/api/admin/posts/${post.value.id}/feature`, {
      method: 'PUT',
      body: {
        featured: !post.value.featured
      }
    })

    await loadPost()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '加精操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function deletePost() {
  if (!post.value) {
    return
  }

  try {
    await api.request<void>(`/api/admin/posts/${post.value.id}`, {
      method: 'DELETE'
    })

    toast.add({
      title: '帖子已删除',
      color: 'success'
    })

    await navigateTo('/')
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '删除帖子失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  }
}

async function handleCommentPageChange(nextPage: number) {
  commentsPage.value = nextPage
  await loadComments()
}

watch(
  () => route.params.id,
  async () => {
    commentsPage.value = 1
    resetComposer()
    await refreshAll()
  },
  { immediate: true }
)
</script>

<template>
  <div
    class="space-y-6 pb-[var(--composer-padding-mobile)] transition-[padding-bottom] duration-[320ms] ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none sm:pb-[var(--composer-padding-desktop)]"
    :style="composerPaddingStyle"
  >
    <section class="flex flex-wrap items-center justify-between gap-3 rounded-[2rem] border border-default bg-elevated/85 px-6 py-4 shadow-lg shadow-slate-900/5 backdrop-blur">
      <div class="text-sm text-muted">
        <NuxtLink to="/" class="transition hover:text-default">
          首页
        </NuxtLink>
        <span class="mx-2">/</span>
        <span>{{ post?.boardName || '帖子详情' }}</span>
      </div>

      <UButton to="/" color="neutral" variant="outline" class="rounded-full">
        返回列表
      </UButton>
    </section>

    <UAlert
      v-if="errorMessage"
      color="error"
      variant="soft"
      icon="i-lucide-circle-alert"
      :title="errorMessage"
    />

    <template v-if="post">
      <article class="overflow-hidden rounded-[2rem] border border-default bg-elevated/90 shadow-xl shadow-slate-900/5 backdrop-blur">
        <div class="border-b border-default px-6 py-6">
          <div class="flex flex-wrap items-center gap-2">
            <span
              v-if="post.pinned"
              class="rounded-full bg-rose-50 px-3 py-1 text-xs font-semibold text-rose-700"
            >
              置顶
            </span>
            <span
              v-if="post.featured"
              class="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700"
            >
              加精
            </span>
            <span class="rounded-full border border-default bg-muted px-3 py-1 text-xs font-semibold text-muted">
              {{ post.boardName }}
            </span>
          </div>

          <h1 class="mt-4 text-3xl font-semibold leading-tight text-default sm:text-4xl">
            {{ post.title }}
          </h1>

          <div class="mt-5 flex flex-col gap-4 border-t border-default pt-5 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <div class="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-full border border-default bg-primary text-lg font-semibold text-white shadow-sm shadow-slate-900/10">
                <img
                  v-if="post.author.avatarUrl"
                  :src="post.author.avatarUrl"
                  :alt="displayName(post.author)"
                  class="h-full w-full object-cover"
                >
                <span v-else>{{ initialsOf(post.author) }}</span>
              </div>

              <div class="min-w-0">
                <NuxtLink
                  :to="`/users/${post.author.id}`"
                  class="text-base font-semibold text-default transition hover:text-primary"
                >
                  {{ displayName(post.author) }}
                </NuxtLink>
                <div class="mt-1 text-sm text-muted">
                  @{{ post.author.username }} · Lv.{{ post.author.level }}
                </div>
                <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
                  <span>发布于 {{ formatDateTime(post.createdAt) }}</span>
                  <span>更新于 {{ formatDateTime(post.updatedAt) }}</span>
                </div>
              </div>
            </div>

            <UButton
              :to="`/users/${post.author.id}`"
              color="neutral"
              variant="outline"
              class="rounded-full"
            >
              访问作者主页
            </UButton>
          </div>
        </div>

        <div class="px-6 py-6">
          <div class="whitespace-pre-wrap text-base leading-8 text-default">
            {{ post.contentText || '该帖子仅包含图片内容。' }}
          </div>

          <div
            v-if="post.imageUrls.length"
            class="mt-6 grid gap-4 sm:grid-cols-2"
          >
            <a
              v-for="(imageUrl, index) in post.imageUrls"
              :key="imageUrl"
              :href="imageUrl"
              target="_blank"
              rel="noreferrer"
              class="overflow-hidden rounded-3xl border border-default bg-muted/50"
            >
              <img
                :src="imageUrl"
                :alt="`${post.title} 图片 ${index + 1}`"
                class="h-64 w-full object-cover transition hover:scale-[1.02]"
              >
            </a>
          </div>

          <div class="mt-6 flex items-center gap-2 overflow-x-auto pb-1">
            <UButton color="neutral" variant="outline" class="rounded-full whitespace-nowrap" @click="handleToggleLike">
              {{ post.liked ? '取消点赞' : '点赞' }} · {{ formatCount(post.likeCount) }}
            </UButton>
            <UButton color="neutral" variant="outline" class="rounded-full whitespace-nowrap" @click="handleToggleFavorite">
              {{ post.favorited ? '取消收藏' : '收藏' }}
            </UButton>
            <UButton color="neutral" variant="outline" class="rounded-full whitespace-nowrap" @click="rewardOpen = true">
              打赏 · {{ formatCount(post.rewardCoinCount) }}
            </UButton>
          </div>

          <div v-if="isAdmin" class="mt-6 rounded-[2rem] border border-amber-200 bg-amber-50 p-4">
            <div class="text-sm font-semibold text-amber-900">
              管理员操作
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <UButton color="warning" variant="outline" class="rounded-full" @click="togglePin">
                {{ post.pinned ? '取消置顶' : '设为置顶' }}
              </UButton>
              <UButton color="warning" variant="outline" class="rounded-full" @click="toggleFeatured">
                {{ post.featured ? '取消加精' : '设为加精' }}
              </UButton>
              <UButton color="error" variant="outline" class="rounded-full" @click="deletePost">
                删除帖子
              </UButton>
            </div>
          </div>
        </div>
      </article>

      <section class="rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-xl shadow-slate-900/5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-2xl font-semibold text-default">
            评价区
          </h2>
          <div class="text-sm text-muted">
            共 {{ formatCount(post.commentCount) }} 条评价
          </div>
        </div>

        <div class="mt-5 space-y-4">
          <CommentThread
            v-for="comment in comments.list"
            :key="comment.id"
            :comment="comment"
            @refresh="refreshAll"
            @reply="handleReply"
          />

          <div
            v-if="commentsLoading"
            class="h-32 animate-pulse rounded-3xl border border-default bg-muted/50"
          />

          <EmptyState
            v-else-if="!comments.list.length"
            title="还没有评价"
            description="这篇帖子还没有评价，欢迎发布第一条评价。"
            icon="i-lucide-message-circle"
          />
        </div>

        <div v-if="comments.total > commentsPageSize" class="mt-5 flex justify-end">
          <UPagination
            :page="commentsPage"
            :items-per-page="commentsPageSize"
            :total="comments.total"
            :show-controls="true"
            :show-edges="true"
            @update:page="handleCommentPageChange"
          />
        </div>
      </section>

      <PostCommentComposer
        ref="composerRef"
        :logged-in="isLoggedIn"
        :login-to="loginRedirect"
        :expanded="composerExpanded"
        :content-text="draft.contentText"
        :image-urls="draft.imageUrls"
        :reply-target-name="replyTarget?.authorName || null"
        :pending="composerPending"
        :upload-pending="composerUploadPending"
        :image-accept="imageAccept"
        :max-upload-size-mb="maxUploadSizeMb"
        @update:content-text="draft.contentText = $event"
        @expand="openComposer"
        @collapse="collapseComposer"
        @select-images="handleComposerImageSelection"
        @remove-image="removeComposerImageUrl"
        @submit="submitComment"
      />
    </template>

    <EmptyState
      v-else-if="!postLoading"
      title="帖子不存在"
      description="该帖子可能已被删除，或者链接参数不正确。"
      icon="i-lucide-file-x"
    />

    <UModal
      v-model:open="rewardOpen"
      title="打赏帖子"
      description="请输入要赠送的硬币数量，最低 1 个。"
      :ui="{ content: 'max-w-lg rounded-[2rem]' }"
    >
      <template #body>
        <div class="space-y-3">
          <div class="text-sm text-muted">
            当前帖子：{{ post?.title }}
          </div>
          <UInput
            v-model="rewardAmount"
            type="number"
            min="1"
            :ui="{ base: 'rounded-2xl' }"
          />
        </div>
      </template>

      <template #footer="{ close }">
        <div class="flex w-full justify-end gap-2">
          <UButton color="neutral" variant="outline" class="rounded-full" @click="close">
            取消
          </UButton>
          <UButton
            class="rounded-full bg-primary text-white hover:bg-primary/90"
            @click="handleReward"
          >
            确认打赏
          </UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
