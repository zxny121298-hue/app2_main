<script setup lang="ts">
const props = withDefaults(defineProps<{
  loggedIn: boolean
  loginTo: string
  expanded: boolean
  contentText: string
  imageUrls: string[]
  replyTargetName?: string | null
  pending?: boolean
  uploadPending?: boolean
  imageAccept: string
  maxUploadSizeMb: number
}>(), {
  replyTargetName: null,
  pending: false,
  uploadPending: false
})

const emit = defineEmits<{
  'update:contentText': [value: string]
  'select-images': [files: File[]]
  'remove-image': [url: string]
  submit: []
  expand: []
  collapse: []
}>()

const composerRoot = ref<HTMLElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)

const transitionCurve = 'cubic-bezier(0.22,1,0.36,1)'

const expandedPlaceholder = computed(() => {
  if (!props.loggedIn) {
    return '登录后参与评价'
  }

  if (props.replyTargetName) {
    return `回复 ${props.replyTargetName}`
  }

  return '回复这篇帖子'
})

const collapsedPreview = computed(() => {
  const previewLine = props.contentText
    .replace(/\r/g, '')
    .split('\n')
    .map(line => line.trim())
    .find(Boolean)

  if (previewLine) {
    return previewLine.length > 48 ? `${previewLine.slice(0, 48)}...` : previewLine
  }

  if (props.imageUrls.length) {
    return '继续编辑评价'
  }

  return expandedPlaceholder.value
})

const collapsibleUi = {
  root: 'w-full',
  content: `overflow-hidden data-[state=open]:animate-[collapsible-down_320ms_${transitionCurve}] data-[state=closed]:animate-[collapsible-up_300ms_${transitionCurve}] motion-reduce:data-[state=open]:animate-none motion-reduce:data-[state=closed]:animate-none`
}

const composerShapeStyle = computed(() => ({
  borderRadius: props.expanded ? '2rem' : '9999px'
}))

function focusInput() {
  if (!props.expanded) {
    return
  }

  const textarea = composerRoot.value?.querySelector('textarea')

  if (textarea instanceof HTMLTextAreaElement) {
    textarea.focus()
  }
}

function requestExpand() {
  emit('expand')
}

function openImagePicker() {
  imageInput.value?.click()
}

function handleImageSelection(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  target.value = ''

  if (files.length) {
    emit('select-images', files)
  }
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!props.expanded || props.pending || props.uploadPending) {
    return
  }

  const target = event.target

  if (!(target instanceof Node)) {
    return
  }

  if (composerRoot.value?.contains(target)) {
    return
  }

  emit('collapse')
}

onMounted(() => {
  if (import.meta.client) {
    document.addEventListener('pointerdown', handleDocumentPointerDown)
  }
})

onBeforeUnmount(() => {
  if (import.meta.client) {
    document.removeEventListener('pointerdown', handleDocumentPointerDown)
  }
})

defineExpose({
  focusInput
})
</script>

<template>
  <div
    class="pointer-events-none fixed inset-x-0 bottom-0 z-40 px-3 sm:px-6"
    style="padding-bottom: calc(env(safe-area-inset-bottom) + 0.75rem)"
  >
    <div
      ref="composerRoot"
      class="pointer-events-auto mx-auto max-w-5xl overflow-hidden transform-gpu border transition-[border-radius,background-color,border-color,box-shadow,backdrop-filter] duration-[320ms] ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none"
      :style="composerShapeStyle"
      :class="expanded
        ? 'border-default bg-elevated/90 shadow-[0_-24px_60px_rgba(15,23,42,0.18)] backdrop-blur-xl'
        : 'border-default bg-elevated/70 shadow-[0_12px_40px_rgba(15,23,42,0.14)] backdrop-blur-xl'"
    >
      <UCollapsible
        :open="expanded"
        :unmount-on-hide="false"
        :ui="collapsibleUi"
      >
        <div
          class="overflow-hidden transition-[max-height,opacity,transform,padding] duration-[300ms] ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none"
          :class="expanded
            ? 'pointer-events-none max-h-0 -translate-y-2 px-5 py-0 opacity-0'
            : 'max-h-24 translate-y-0 px-5 py-4 opacity-100'"
        >
          <button
            type="button"
            class="block w-full rounded-[inherit] text-left"
            @click="requestExpand"
          >
            <span class="block truncate text-sm text-muted">
              {{ collapsedPreview }}
            </span>
          </button>
        </div>

        <template #content>
          <div
            class="space-y-4 px-4 pb-4 pt-4 transform-gpu transition-[opacity,transform] duration-[300ms] ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none sm:px-5"
            :class="expanded ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-2 opacity-0'"
          >
            <div v-if="replyTargetName" class="flex items-center">
              <span class="inline-flex max-w-full truncate rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted">
                回复 {{ replyTargetName }}
              </span>
            </div>

            <UTextarea
              :model-value="contentText"
              :rows="3"
              :maxrows="8"
              autoresize
              fixed
              color="neutral"
              variant="soft"
              :placeholder="expandedPlaceholder"
              :disabled="!loggedIn || pending"
              :ui="{ base: 'rounded-[1.5rem] border border-default bg-default/80 px-4 py-3 text-sm leading-6 shadow-inner shadow-slate-900/5' }"
              @update:model-value="emit('update:contentText', String($event))"
            />

            <div v-if="imageUrls.length" class="max-h-40 overflow-y-auto pr-1">
              <div class="grid gap-3 sm:grid-cols-2">
                <div
                  v-for="(imageUrl, index) in imageUrls"
                  :key="imageUrl"
                  class="overflow-hidden rounded-[1.25rem] border border-default bg-muted"
                >
                  <img
                    :src="imageUrl"
                    :alt="`评价图片预览 ${index + 1}`"
                    class="h-28 w-full object-cover"
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
                      @click="emit('remove-image', imageUrl)"
                    >
                      移除
                    </UButton>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="flex flex-wrap items-center gap-2">
                <input
                  ref="imageInput"
                  type="file"
                  class="hidden"
                  :accept="imageAccept"
                  multiple
                  @change="handleImageSelection"
                >
                <UButton
                  color="neutral"
                  variant="outline"
                  size="sm"
                  class="rounded-full"
                  :loading="uploadPending"
                  :disabled="!loggedIn || pending"
                  @click="openImagePicker"
                >
                  上传图片
                </UButton>
                <span class="text-xs text-muted">
                  单张最大 {{ maxUploadSizeMb }} MB
                </span>
              </div>

              <UButton
                v-if="loggedIn"
                :loading="pending"
                :disabled="uploadPending"
                class="rounded-full bg-primary text-white hover:bg-primary/90"
                @click="emit('submit')"
              >
                {{ replyTargetName ? '发送回复' : '发布评价' }}
              </UButton>
            </div>
          </div>
        </template>
      </UCollapsible>
    </div>
  </div>
</template>
