<script setup lang="ts">
import type { BoardView, CreatePostRequest, PostDetailView } from '~/types/forum'
import { findFirstTooLongEntry, normalizeNullableText } from '~/utils/forum'

const props = defineProps<{
  open: boolean
  boards: BoardView[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  created: [value: PostDetailView]
}>()

const api = useApi()
const toast = useToast()
const { uploadImage, accept: imageAccept, maxBytes: maxUploadBytes } = useImageUpload()

const pending = ref(false)
const imageUploadPending = ref(false)
const postImageInput = ref<HTMLInputElement | null>(null)
const state = reactive({
  boardId: 0,
  title: '',
  contentText: '',
  imageUrls: [] as string[]
})

const selectedBoard = computed(() => props.boards.find(board => board.id === Number(state.boardId)) || null)
const imageUrls = computed(() => state.imageUrls)
const maxUploadSizeMb = Math.floor(maxUploadBytes / (1024 * 1024))

const modelValue = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value)
})

function resetState() {
  state.boardId = props.boards[0]?.id || 0
  state.title = ''
  state.contentText = ''
  state.imageUrls = []
  imageUploadPending.value = false
}

function setImageUrls(urls: string[]) {
  state.imageUrls = [...urls]
}

function appendImageUrls(urls: string[]) {
  const merged = [...imageUrls.value]

  for (const url of urls) {
    if (!merged.includes(url)) {
      merged.push(url)
    }
  }

  setImageUrls(merged)
}

function removeImageUrl(targetUrl: string) {
  setImageUrls(imageUrls.value.filter(url => url !== targetUrl))
}

function openImagePicker() {
  postImageInput.value?.click()
}

watch(
  () => props.open,
  (value) => {
    if (value) {
      resetState()
    }
  },
  { immediate: true }
)

async function handleImageSelection(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  target.value = ''

  if (!files.length) {
    return
  }

  imageUploadPending.value = true
  const uploadedUrls: string[] = []
  let failedCount = 0
  let lastErrorMessage = ''

  try {
    for (const file of files) {
      try {
        const uploaded = await uploadImage(file, 'post')
        uploadedUrls.push(uploaded.url)
      } catch (error) {
        failedCount += 1
        lastErrorMessage = error instanceof Error ? error.message : '请稍后重试'
      }
    }

    if (uploadedUrls.length) {
      appendImageUrls(uploadedUrls)

      toast.add({
        title: `已上传 ${uploadedUrls.length} 张图片`,
        color: 'success'
      })
    }

    if (failedCount) {
      toast.add({
        title: `${failedCount} 张图片上传失败`,
        description: lastErrorMessage || '请稍后重试',
        color: 'warning'
      })
    }
  } finally {
    imageUploadPending.value = false
  }
}

async function submit() {
  const nextImageUrls = imageUrls.value
  const payload: CreatePostRequest = {
    boardId: Number(state.boardId),
    title: state.title.trim(),
    contentText: normalizeNullableText(state.contentText),
    imageUrls: nextImageUrls.length ? nextImageUrls : null
  }

  if (!payload.boardId) {
    toast.add({
      title: '请选择板块',
      color: 'warning'
    })
    return
  }

  if (!payload.title) {
    toast.add({
      title: '请输入帖子标题',
      color: 'warning'
    })
    return
  }

  if (payload.title.length > 200) {
    toast.add({
      title: '帖子标题不能超过 200 个字符',
      color: 'warning'
    })
    return
  }

  if (!payload.contentText && !payload.imageUrls?.length) {
    toast.add({
      title: '正文和图片不能同时为空',
      color: 'warning'
    })
    return
  }

  const tooLongImageUrl = findFirstTooLongEntry(payload.imageUrls, 255)

  if (tooLongImageUrl) {
    toast.add({
      title: '图片地址不能超过 255 个字符',
      description: tooLongImageUrl,
      color: 'warning'
    })
    return
  }

  pending.value = true

  try {
    const post = await api.request<PostDetailView>('/api/posts', {
      method: 'POST',
      body: payload
    })

    toast.add({
      title: '帖子发布成功',
      description: '已跳转到帖子详情页',
      color: 'success'
    })

    emit('created', post)
    modelValue.value = false
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '帖子发布失败',
      description: apiError.message || '请稍后再试',
      color: 'error'
    })
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <UModal
    v-model:open="modelValue"
    title="发布新帖子"
    description="正文和图片至少填写一项，图片通过上传添加。"
    :ui="{
      content: 'max-w-3xl rounded-[2rem]',
      body: 'space-y-5'
    }"
  >
    <template #body>
      <div class="grid gap-5 md:grid-cols-2">
        <div class="space-y-2">
          <label class="text-sm font-medium text-default">所属板块</label>
          <select
            v-model="state.boardId"
            class="h-12 w-full rounded-2xl border border-default bg-default px-4 text-sm text-default outline-none transition focus:border-primary/50"
          >
            <option :value="0" disabled>
              请选择板块
            </option>
            <option
              v-for="board in boards"
              :key="board.id"
              :value="board.id"
            >
              {{ board.name }}
            </option>
          </select>
          <p v-if="selectedBoard?.description" class="text-xs leading-6 text-muted">
            {{ selectedBoard.description }}
          </p>
          <p v-else-if="!boards.length" class="text-xs leading-6 text-rose-600">
            当前没有可用板块，请先让管理员创建并启用板块。
          </p>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-default">帖子标题</label>
          <UInput
            v-model="state.title"
            placeholder="最多 200 个字符"
            :ui="{ base: 'rounded-2xl' }"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-sm font-medium text-default">正文内容</label>
        <UTextarea
          v-model="state.contentText"
          :rows="8"
          placeholder="输入帖子正文，当前仅支持纯文本。"
          :ui="{ base: 'rounded-3xl' }"
        />
      </div>

      <div class="space-y-3">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <label class="text-sm font-medium text-default">帖子图片</label>
            <p class="mt-1 text-xs leading-6 text-muted">
              支持 JPG、PNG、WEBP、GIF、AVIF，单张最大 {{ maxUploadSizeMb }} MB。上传成功后会加入当前帖子图片列表。
            </p>
          </div>

          <div class="flex flex-wrap gap-2">
            <input
              ref="postImageInput"
              type="file"
              class="hidden"
              :accept="imageAccept"
              multiple
              @change="handleImageSelection"
            >
            <UButton
              color="neutral"
              variant="outline"
              class="rounded-full"
              :loading="imageUploadPending"
              @click="openImagePicker"
            >
              上传图片
            </UButton>
          </div>
        </div>

        <div
          v-if="imageUrls.length"
          class="grid gap-3 sm:grid-cols-2"
        >
          <div
            v-for="imageUrl in imageUrls"
            :key="imageUrl"
            class="overflow-hidden rounded-[1.5rem] border border-default bg-muted/50"
          >
            <img
              :src="imageUrl"
              :alt="imageUrl"
              class="h-40 w-full object-cover"
            >
            <div class="flex items-center gap-3 border-t border-default bg-default px-3 py-3">
              <div class="min-w-0 flex-1 truncate text-xs text-muted">
                {{ imageUrl }}
              </div>
              <UButton
                color="error"
                variant="ghost"
                size="sm"
                class="rounded-full"
                @click="removeImageUrl(imageUrl)"
              >
                移除
              </UButton>
            </div>
          </div>
        </div>

      </div>
    </template>

    <template #footer="{ close }">
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" class="rounded-full" @click="close">
          取消
        </UButton>
        <UButton
          :loading="pending"
          class="rounded-full bg-primary text-white hover:bg-primary/90"
          @click="submit"
        >
          发布帖子
        </UButton>
      </div>
    </template>
  </UModal>
</template>
