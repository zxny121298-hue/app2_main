<script setup lang="ts">
import { h } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { BoardStatus, BoardView, UpsertBoardRequest } from '~/types/forum'
import { boardStatusLabels, formatCount, formatDateTime } from '~/utils/forum'

type BoardFilterValue = 'all' | BoardStatus

const emit = defineEmits<{
  refreshSummary: []
}>()

const toast = useToast()
const api = useApi()

const MAX_BOARD_NAME_LENGTH = 100
const MAX_BOARD_DESCRIPTION_LENGTH = 255
const MAX_BOARD_SORT_ORDER = 999999

const boards = ref<BoardView[]>([])
const boardsPending = ref(false)
const boardFilter = ref<BoardFilterValue>('all')
const boardEditorOpen = ref(false)
const boardEditorPending = ref(false)
const boardEditingId = ref<number | null>(null)
const boardStatusConfirmOpen = ref(false)
const boardStatusPending = ref(false)
const boardStatusTarget = ref<BoardView | null>(null)
const batchConfirmOpen = ref(false)
const batchPending = ref(false)
const batchTargetStatus = ref<BoardStatus | null>(null)
const selectedBoardIds = ref<number[]>([])
const boardForm = reactive<{
  name: string
  description: string
  sortOrder: number
  status: BoardStatus
}>({
  name: '',
  description: '',
  sortOrder: 0,
  status: 'enabled'
})

const enabledBoards = computed(() =>
  boards.value.filter(board => board.status === 'enabled')
)
const disabledBoards = computed(() =>
  boards.value.filter(board => board.status === 'disabled')
)
const filteredBoards = computed(() => {
  if (boardFilter.value === 'all') {
    return boards.value
  }

  return boards.value.filter(board => board.status === boardFilter.value)
})
const visibleBoardIds = computed(() => filteredBoards.value.map(board => board.id))
const allVisibleBoardsSelected = computed(() =>
  visibleBoardIds.value.length > 0 &&
  visibleBoardIds.value.every(id => selectedBoardIds.value.includes(id))
)
const canBatchEnable = computed(() =>
  selectedBoardIds.value.some(id =>
    boards.value.find(board => board.id === id)?.status === 'disabled'
  )
)
const canBatchDisable = computed(() =>
  selectedBoardIds.value.some(id =>
    boards.value.find(board => board.id === id)?.status === 'enabled'
  )
)

const boardFilterItems = [
  { label: '全部状态', value: 'all' },
  { label: '启用中', value: 'enabled' },
  { label: '已停用', value: 'disabled' }
]

const boardStatusItems = [
  { label: '启用中', value: 'enabled' },
  { label: '已停用', value: 'disabled' }
]

const boardColumns = computed((): TableColumn<BoardView>[] => [
  {
    id: 'select',
    header: () =>
      h('div', { class: 'flex justify-center' }, [
        h('input', {
          type: 'checkbox',
          checked: allVisibleBoardsSelected.value,
          disabled: !visibleBoardIds.value.length,
          class: 'h-4 w-4 rounded border-default text-primary focus:ring-primary/40',
          onClick: (event: Event) => event.stopPropagation(),
          onChange: (event: Event) => {
            toggleAllVisibleBoards((event.target as HTMLInputElement).checked)
          }
        })
      ]),
    cell: ({ row }) =>
      h('div', { class: 'flex justify-center' }, [
        h('input', {
          type: 'checkbox',
          checked: selectedBoardIds.value.includes(row.original.id),
          class: 'h-4 w-4 rounded border-default text-primary focus:ring-primary/40',
          onClick: (event: Event) => event.stopPropagation(),
          onChange: (event: Event) => {
            toggleBoardSelection(row.original.id, (event.target as HTMLInputElement).checked)
          }
        })
      ]),
    meta: {
      class: {
        th: 'w-12',
        td: 'w-12'
      }
    }
  },
  {
    accessorKey: 'name',
    header: '板块名',
    cell: ({ row }) =>
      h('div', { class: 'min-w-0' }, [
        h('div', { class: 'font-medium text-default' }, row.original.name),
        h('div', { class: 'mt-1 text-xs text-muted' }, `ID ${row.original.id}`)
      ])
  },
  {
    accessorKey: 'description',
    header: '描述',
    cell: ({ row }) =>
      h(
        'div',
        { class: 'max-w-lg text-sm leading-6 text-muted' },
        row.original.description || '暂无描述'
      )
  },
  {
    accessorKey: 'sortOrder',
    header: '排序',
    cell: ({ row }) =>
      h('span', { class: 'text-sm font-medium text-default' }, String(row.original.sortOrder))
  },
  {
    accessorKey: 'status',
    header: '状态',
    cell: ({ row }) =>
      renderStatusPill(
        boardStatusLabels[row.original.status],
        row.original.status === 'enabled' ? 'success' : 'neutral'
      )
  },
  {
    accessorKey: 'updatedAt',
    header: '更新时间',
    cell: ({ row }) =>
      h('div', { class: 'text-sm text-muted' }, formatDateTime(row.original.updatedAt))
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, '操作'),
    cell: ({ row }) =>
      h('div', { class: 'flex flex-wrap justify-end gap-2' }, [
        renderTableButton('编辑', () => openEditBoardEditor(row.original)),
        renderTableButton(
          row.original.status === 'enabled' ? '停用' : '启用',
          () => requestToggleBoardStatus(row.original),
          row.original.status === 'enabled' ? 'danger' : 'success',
          boardStatusPending.value && boardStatusTarget.value?.id === row.original.id
        )
      ])
  }
])

watch(boardFilter, () => {
  selectedBoardIds.value = []
})

onMounted(loadBoards)

function normalizeNullableText(value?: string | null) {
  const normalized = value?.trim() || ''
  return normalized || null
}

function resetBoardForm() {
  boardEditingId.value = null
  boardForm.name = ''
  boardForm.description = ''
  boardForm.sortOrder = 0
  boardForm.status = 'enabled'
}

function openCreateBoardEditor() {
  resetBoardForm()
  boardEditorOpen.value = true
}

function openEditBoardEditor(board: BoardView) {
  boardEditingId.value = board.id
  boardForm.name = board.name
  boardForm.description = board.description || ''
  boardForm.sortOrder = board.sortOrder
  boardForm.status = board.status
  boardEditorOpen.value = true
}

function closeBoardEditor() {
  boardEditorOpen.value = false
  resetBoardForm()
}

function toggleBoardSelection(boardId: number, checked: boolean) {
  if (checked) {
    if (!selectedBoardIds.value.includes(boardId)) {
      selectedBoardIds.value.push(boardId)
    }
    return
  }

  selectedBoardIds.value = selectedBoardIds.value.filter(id => id !== boardId)
}

function toggleAllVisibleBoards(checked: boolean) {
  if (checked) {
    selectedBoardIds.value = Array.from(new Set([
      ...selectedBoardIds.value,
      ...visibleBoardIds.value
    ]))
    return
  }

  selectedBoardIds.value = selectedBoardIds.value.filter(id => !visibleBoardIds.value.includes(id))
}

async function loadBoards() {
  boardsPending.value = true

  try {
    boards.value = await api.request<BoardView[]>('/api/boards', {
      query: {
        includeDisabled: true
      }
    })

    selectedBoardIds.value = selectedBoardIds.value.filter(id =>
      boards.value.some(board => board.id === id)
    )

    emit('refreshSummary')
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

function validateBoardForm() {
  const name = boardForm.name.trim()
  const description = boardForm.description.trim()
  const sortOrder = Number(boardForm.sortOrder)

  if (!name) {
    toast.add({
      title: '请输入板块名称',
      color: 'warning'
    })
    return null
  }

  if (name.length > MAX_BOARD_NAME_LENGTH) {
    toast.add({
      title: `板块名称不能超过 ${MAX_BOARD_NAME_LENGTH} 个字符`,
      color: 'warning'
    })
    return null
  }

  if (description.length > MAX_BOARD_DESCRIPTION_LENGTH) {
    toast.add({
      title: `板块描述不能超过 ${MAX_BOARD_DESCRIPTION_LENGTH} 个字符`,
      color: 'warning'
    })
    return null
  }

  if (!Number.isInteger(sortOrder) || sortOrder < 0 || sortOrder > MAX_BOARD_SORT_ORDER) {
    toast.add({
      title: `排序值必须是 0 到 ${MAX_BOARD_SORT_ORDER} 之间的整数`,
      color: 'warning'
    })
    return null
  }

  const payload: UpsertBoardRequest = {
    name,
    description: normalizeNullableText(description),
    sortOrder,
    status: boardForm.status
  }

  return payload
}

async function submitBoardEditor() {
  const payload = validateBoardForm()
  if (!payload) {
    return
  }

  boardEditorPending.value = true

  try {
    if (boardEditingId.value) {
      await api.request<BoardView>(`/api/admin/boards/${boardEditingId.value}`, {
        method: 'PUT',
        body: payload
      })
    } else {
      await api.request<BoardView>('/api/admin/boards', {
        method: 'POST',
        body: payload
      })
    }

    toast.add({
      title: boardEditingId.value ? '板块已更新' : '板块已创建',
      color: 'success'
    })

    closeBoardEditor()
    await loadBoards()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '板块保存失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    boardEditorPending.value = false
  }
}

function requestToggleBoardStatus(board: BoardView) {
  boardStatusTarget.value = board

  if (board.status === 'disabled') {
    void confirmToggleBoardStatus()
    return
  }

  boardStatusConfirmOpen.value = true
}

async function confirmToggleBoardStatus() {
  if (!boardStatusTarget.value) {
    return
  }

  const targetBoard = boardStatusTarget.value
  const nextStatus: BoardStatus = targetBoard.status === 'enabled' ? 'disabled' : 'enabled'
  boardStatusPending.value = true

  try {
    await api.request<void>(`/api/admin/boards/${targetBoard.id}/status`, {
      method: 'PUT',
      body: {
        status: nextStatus
      }
    })

    toast.add({
      title: nextStatus === 'enabled' ? '板块已启用' : '板块已停用',
      color: 'success'
    })

    boardStatusConfirmOpen.value = false
    boardStatusTarget.value = null
    selectedBoardIds.value = selectedBoardIds.value.filter(id => id !== targetBoard.id)
    await loadBoards()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '板块状态更新失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    boardStatusPending.value = false
  }
}

function requestBatchStatus(status: BoardStatus) {
  if (!selectedBoardIds.value.length) {
    toast.add({
      title: '请先选择板块',
      color: 'warning'
    })
    return
  }

  batchTargetStatus.value = status
  batchConfirmOpen.value = true
}

async function confirmBatchStatus() {
  if (!batchTargetStatus.value || !selectedBoardIds.value.length) {
    return
  }

  batchPending.value = true

  try {
    await Promise.all(
      selectedBoardIds.value.map(id =>
        api.request<void>(`/api/admin/boards/${id}/status`, {
          method: 'PUT',
          body: {
            status: batchTargetStatus.value
          }
        })
      )
    )

    toast.add({
      title: batchTargetStatus.value === 'enabled' ? '批量启用成功' : '批量停用成功',
      color: 'success'
    })

    batchConfirmOpen.value = false
    batchTargetStatus.value = null
    selectedBoardIds.value = []
    await loadBoards()
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '批量操作失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    batchPending.value = false
  }
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
</script>

<template>
  <section class="rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-xl shadow-slate-900/5">
    <div class="flex flex-col gap-4 border-b border-default pb-5 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <h2 class="text-2xl font-semibold text-default">
          板块管理
        </h2>
        <p class="mt-2 text-sm leading-7 text-muted">
          统一维护板块的创建、排序与启停。批量处理改成显式多选，不再依赖长按手势。
        </p>
      </div>

      <div class="flex flex-wrap gap-3">
        <USelect
          v-model="boardFilter"
          :items="boardFilterItems"
          class="min-w-44"
          color="neutral"
          variant="outline"
        />
        <UButton
          class="rounded-full"
          @click="openCreateBoardEditor"
        >
          新建板块
        </UButton>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-3 rounded-[1.75rem] border border-default bg-muted/50 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="space-y-1">
        <div class="text-sm font-medium text-default">
          已选 {{ selectedBoardIds.length }} 个板块
        </div>
        <div class="text-xs text-muted">
          启用中 {{ formatCount(enabledBoards.length) }} 个，已停用 {{ formatCount(disabledBoards.length) }} 个
        </div>
      </div>

      <div class="flex flex-wrap gap-2">
        <UButton
          color="success"
          variant="soft"
          class="rounded-full"
          :loading="batchPending && batchTargetStatus === 'enabled'"
          :disabled="!canBatchEnable || batchPending"
          @click="requestBatchStatus('enabled')"
        >
          批量启用
        </UButton>
        <UButton
          color="error"
          variant="soft"
          class="rounded-full"
          :loading="batchPending && batchTargetStatus === 'disabled'"
          :disabled="!canBatchDisable || batchPending"
          @click="requestBatchStatus('disabled')"
        >
          批量停用
        </UButton>
      </div>
    </div>

    <div class="mt-5 overflow-hidden rounded-[1.75rem] border border-default bg-default">
      <UTable
        :data="filteredBoards"
        :columns="boardColumns"
        :loading="boardsPending"
        loading-color="primary"
        loading-animation="carousel"
        empty="当前条件下没有板块。"
        sticky="header"
        class="max-h-[42rem]"
      />
    </div>

    <UModal
      v-model:open="boardEditorOpen"
      :title="boardEditingId ? '编辑板块' : '新建板块'"
      :description="boardEditingId ? '更新板块名称、描述、排序和状态。' : '创建一个新的论坛板块。'"
      :ui="{ content: 'max-w-2xl rounded-[2rem]', body: 'space-y-4' }"
    >
      <template #body>
        <div class="grid gap-2">
          <label class="text-sm font-medium text-default">板块名称</label>
          <UInput
            v-model="boardForm.name"
            color="neutral"
            variant="outline"
            :ui="{ base: 'rounded-2xl bg-default' }"
          />
        </div>

        <div class="grid gap-2">
          <label class="text-sm font-medium text-default">板块描述</label>
          <UTextarea
            v-model="boardForm.description"
            :rows="4"
            color="neutral"
            variant="outline"
            :ui="{ base: 'rounded-[1.75rem] bg-default' }"
          />
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="grid gap-2">
            <label class="text-sm font-medium text-default">排序值</label>
            <UInput
              v-model="boardForm.sortOrder"
              type="number"
              color="neutral"
              variant="outline"
              :ui="{ base: 'rounded-2xl bg-default' }"
            />
            <p class="text-xs text-muted">
              排序值需为 0 到 {{ MAX_BOARD_SORT_ORDER }} 之间的整数。
            </p>
          </div>

          <div class="grid gap-2">
            <label class="text-sm font-medium text-default">状态</label>
            <USelect
              v-model="boardForm.status"
              :items="boardStatusItems"
              color="neutral"
              variant="outline"
            />
          </div>
        </div>
      </template>

      <template #footer>
        <div class="flex w-full justify-end gap-2">
          <UButton
            color="neutral"
            variant="outline"
            class="rounded-full"
            @click="closeBoardEditor"
          >
            取消
          </UButton>
          <UButton
            class="rounded-full"
            :loading="boardEditorPending"
            @click="submitBoardEditor"
          >
            {{ boardEditingId ? '保存更新' : '创建板块' }}
          </UButton>
        </div>
      </template>
    </UModal>

    <UModal
      v-model:open="boardStatusConfirmOpen"
      title="确认停用板块"
      :description="boardStatusTarget ? `停用后，用户将无法在“${boardStatusTarget.name}”内发帖。` : '停用后，用户将无法在该板块内发帖。'"
      :ui="{ content: 'max-w-lg rounded-[2rem]' }"
    >
      <template #footer>
        <div class="flex w-full justify-end gap-2">
          <UButton
            color="neutral"
            variant="outline"
            class="rounded-full"
            @click="boardStatusConfirmOpen = false"
          >
            取消
          </UButton>
          <UButton
            color="error"
            class="rounded-full"
            :loading="boardStatusPending"
            @click="confirmToggleBoardStatus"
          >
            确认停用
          </UButton>
        </div>
      </template>
    </UModal>

    <UModal
      v-model:open="batchConfirmOpen"
      title="确认批量操作"
      :description="batchTargetStatus === 'enabled' ? `确定要批量启用这 ${selectedBoardIds.length} 个板块吗？` : `确定要批量停用这 ${selectedBoardIds.length} 个板块吗？`"
      :ui="{ content: 'max-w-lg rounded-[2rem]' }"
    >
      <template #footer>
        <div class="flex w-full justify-end gap-2">
          <UButton
            color="neutral"
            variant="outline"
            class="rounded-full"
            @click="batchConfirmOpen = false"
          >
            取消
          </UButton>
          <UButton
            :color="batchTargetStatus === 'enabled' ? 'success' : 'error'"
            class="rounded-full"
            :loading="batchPending"
            @click="confirmBatchStatus"
          >
            确认
          </UButton>
        </div>
      </template>
    </UModal>
  </section>
</template>
