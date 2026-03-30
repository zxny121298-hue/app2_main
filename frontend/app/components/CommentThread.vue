<script setup lang="ts">
import type { CommentView } from '~/types/forum'
import { arrayToLines, displayName, formatDateTime } from '~/utils/forum'

type ReplyPayload = {
  commentId: number
  userId: number
  authorName: string
}

defineOptions({
  name: 'CommentThread'
})

const props = withDefaults(defineProps<{
  comment: CommentView
  depth?: number
}>(), {
  depth: 0
})

const emit = defineEmits<{
  refresh: []
  reply: [payload: ReplyPayload]
}>()

const auth = useAuth()
const api = useApi()
const toast = useToast()

const rewardOpen = ref(false)
const rewardAmount = ref(1)
const actionPending = ref(false)
const loginRedirect = computed(() => `/login?redirect=/posts/${props.comment.postId}`)

function triggerReply() {
  emit('reply', {
    commentId: props.comment.id,
    userId: props.comment.author.id,
    authorName: displayName(props.comment.author)
  })
}

async function toggleLike() {
  if (!auth.isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  actionPending.value = true

  try {
    await api.request<void>(`/api/comments/${props.comment.id}/like`, {
      method: props.comment.liked ? 'DELETE' : 'POST'
    })

    emit('refresh')
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '评价点赞失败',
      description: apiError.message || '请稍后再试',
      color: 'error'
    })
  } finally {
    actionPending.value = false
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn.value) {
    await navigateTo(loginRedirect.value)
    return
  }

  actionPending.value = true

  try {
    await api.request<void>(`/api/comments/${props.comment.id}/favorite`, {
      method: props.comment.favorited ? 'DELETE' : 'POST'
    })

    emit('refresh')
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '评价收藏失败',
      description: apiError.message || '请稍后再试',
      color: 'error'
    })
  } finally {
    actionPending.value = false
  }
}

async function submitReward() {
  if (!auth.isLoggedIn.value) {
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

  actionPending.value = true

  try {
    await api.request<void>(`/api/comments/${props.comment.id}/reward`, {
      method: 'POST',
      body: {
        coinAmount: Number(rewardAmount.value)
      }
    })

    rewardOpen.value = false
    emit('refresh')
    toast.add({
      title: '评价打赏成功',
      color: 'success'
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '评价打赏失败',
      description: apiError.message || '请稍后再试',
      color: 'error'
    })
  } finally {
    actionPending.value = false
  }
}
</script>

<template>
  <div :class="depth ? 'border-l border-default pl-4' : ''">
    <article class="rounded-3xl border border-default bg-elevated/90 p-5 shadow-sm">
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2 text-sm text-muted">
            <NuxtLink
              :to="`/users/${comment.author.id}`"
              class="font-semibold text-default transition hover:text-primary"
            >
              {{ displayName(comment.author) }}
            </NuxtLink>

            <template v-if="comment.replyToUser">
              <span>回复</span>
              <NuxtLink
                :to="`/users/${comment.replyToUser.id}`"
                class="font-semibold text-default transition hover:text-primary"
              >
                {{ displayName(comment.replyToUser) }}
              </NuxtLink>
            </template>

            <span>{{ formatDateTime(comment.createdAt) }}</span>
          </div>

          <div class="mt-3 whitespace-pre-wrap text-sm leading-7 text-default">
            {{ comment.contentText || '这条评价仅包含图片。' }}
          </div>

          <div
            v-if="comment.imageUrls.length"
            class="mt-4 grid gap-3 sm:grid-cols-2"
          >
            <a
              v-for="(imageUrl, index) in comment.imageUrls"
              :key="imageUrl"
              :href="imageUrl"
              target="_blank"
              rel="noreferrer"
              class="overflow-hidden rounded-2xl border border-default bg-muted"
            >
              <img
                :src="imageUrl"
                :alt="`${displayName(comment.author)} 的评价图片 ${index + 1}`"
                class="h-44 w-full object-cover transition hover:scale-[1.02]"
              >
            </a>
          </div>
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-center gap-2">
        <UButton
          size="sm"
          color="neutral"
          variant="outline"
          :disabled="actionPending"
          @click="toggleLike"
        >
          {{ comment.liked ? '取消点赞' : '点赞' }} · {{ comment.likeCount }}
        </UButton>
        <UButton
          size="sm"
          color="neutral"
          variant="outline"
          :disabled="actionPending"
          @click="toggleFavorite"
        >
          {{ comment.favorited ? '取消收藏' : '收藏' }}
        </UButton>
        <UButton
          size="sm"
          color="neutral"
          variant="outline"
          :disabled="actionPending"
          @click="triggerReply"
        >
          回复
        </UButton>
        <UButton
          size="sm"
          color="neutral"
          variant="outline"
          :disabled="actionPending"
          @click="rewardOpen = true"
        >
          打赏 · {{ comment.rewardCoinCount }}
        </UButton>
      </div>
    </article>

    <div v-if="comment.children.length" class="mt-4 space-y-4">
      <CommentThread
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :depth="depth + 1"
        @refresh="emit('refresh')"
        @reply="emit('reply', $event)"
      />
    </div>

    <UModal
      v-model:open="rewardOpen"
      title="打赏评价"
      description="请输入要赠送的硬币数量，最少 1 个。"
      :ui="{ content: 'max-w-lg rounded-[2rem]' }"
    >
      <template #body>
        <div class="space-y-3">
          <div class="text-sm text-muted">
            当前评价对象：{{ displayName(comment.author) }}
          </div>
          <UInput
            v-model="rewardAmount"
            type="number"
            min="1"
            :ui="{ base: 'rounded-2xl' }"
          />
          <div class="rounded-2xl border border-default bg-muted px-4 py-3 text-sm text-muted">
            评价摘要：{{ comment.contentText || arrayToLines(comment.imageUrls).slice(0, 80) }}
          </div>
        </div>
      </template>

      <template #footer="{ close }">
        <div class="flex w-full justify-end gap-2">
          <UButton color="neutral" variant="outline" class="rounded-full" @click="close">
            取消
          </UButton>
          <UButton
            :loading="actionPending"
            class="rounded-full bg-primary text-white hover:bg-primary/90"
            @click="submitReward"
          >
            确认打赏
          </UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
