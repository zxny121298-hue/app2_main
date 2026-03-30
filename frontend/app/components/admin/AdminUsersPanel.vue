<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { PageResult, UserProfileView, UserRole, UserStatus } from '~/types/forum'
import {
  displayName,
  formatCount,
  formatDateTime,
  initialsOf,
  userStatusLabels
} from '~/utils/forum'

type UserStatusFilterValue = 'all' | UserStatus
type UserRoleFilterValue = 'all' | UserRole

const NuxtLink = resolveComponent('NuxtLink')
const toast = useToast()
const api = useApi()

const PAGE_SIZE = 10

const userRoleLabels: Record<UserRole, string> = {
  admin: '管理员',
  user: '普通用户'
}

const usersPending = ref(false)
const usersPage = ref(1)
const users = ref<PageResult<UserProfileView>>(createEmptyPageResult<UserProfileView>())
const userFilters = reactive<{
  keyword: string
  status: UserStatusFilterValue
  role: UserRoleFilterValue
}>({
  keyword: '',
  status: 'all',
  role: 'all'
})

const userStatusItems = [
  { label: '全部状态', value: 'all' },
  { label: '正常', value: 'active' },
  { label: '已封禁', value: 'banned' }
]

const userRoleItems = [
  { label: '全部角色', value: 'all' },
  { label: '管理员', value: 'admin' },
  { label: '普通用户', value: 'user' }
]

const userColumns = computed((): TableColumn<UserProfileView>[] => [
  {
    id: 'identity',
    header: '用户',
    cell: ({ row }) => renderIdentityCell(row.original, `@${row.original.username}`)
  },
  {
    accessorKey: 'role',
    header: '角色',
    cell: ({ row }) =>
      renderStatusPill(
        userRoleLabels[row.original.role as UserRole],
        row.original.role === 'admin' ? 'warning' : 'neutral'
      )
  },
  {
    accessorKey: 'status',
    header: '状态',
    cell: ({ row }) =>
      renderStatusPill(
        userStatusLabels[row.original.status as UserStatus],
        row.original.status === 'banned' ? 'danger' : 'success'
      )
  },
  {
    id: 'moderation',
    header: '封禁 / 禁言',
    cell: ({ row }) =>
      h('div', { class: 'space-y-1 text-sm text-muted' }, [
        h('div', userModerationText(row.original)),
        h('div', row.original.mutedUntilAt ? `禁言至 ${formatDateTime(row.original.mutedUntilAt)}` : '当前未禁言')
      ])
  },
  {
    id: 'levelExp',
    header: '等级 / 经验',
    cell: ({ row }) =>
      h('div', { class: 'space-y-1 text-sm text-muted' }, [
        h('div', { class: 'font-medium text-default' }, `Lv.${row.original.level}`),
        h('div', `总经验 ${formatCount(row.original.totalExp)}`),
        h('div', `当前等级经验 ${formatCount(row.original.currentLevelExp)}`)
      ])
  },
  {
    accessorKey: 'coinBalance',
    header: '硬币',
    cell: ({ row }) =>
      h('span', { class: 'text-sm font-medium text-default' }, formatCount(row.original.coinBalance))
  },
  {
    accessorKey: 'createdAt',
    header: '注册时间',
    cell: ({ row }) =>
      h('div', { class: 'text-sm text-muted' }, formatDateTime(row.original.createdAt))
  },
  {
    accessorKey: 'lastLoginAt',
    header: '最近登录',
    cell: ({ row }) =>
      h('div', { class: 'text-sm text-muted' }, formatDateTime(row.original.lastLoginAt))
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, '操作'),
    cell: ({ row }) =>
      h('div', { class: 'flex justify-end' }, [
        renderTableLink(`/users/${row.original.id}`, '查看详情')
      ])
  }
])

watch(usersPage, async (nextPage, previousPage) => {
  if (nextPage !== previousPage) {
    await loadUsers()
  }
})

onMounted(loadUsers)

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

async function loadUsers() {
  usersPending.value = true

  try {
    users.value = await api.request<PageResult<UserProfileView>>('/api/admin/users', {
      query: {
        page: usersPage.value,
        pageSize: PAGE_SIZE,
        keyword: normalizeNullableText(userFilters.keyword) || undefined,
        status: userFilters.status === 'all' ? undefined : userFilters.status,
        role: userFilters.role === 'all' ? undefined : userFilters.role
      }
    })
  } catch (error) {
    const apiError = error as { message?: string }

    toast.add({
      title: '用户列表加载失败',
      description: apiError.message || '请稍后重试',
      color: 'error'
    })
  } finally {
    usersPending.value = false
  }
}

async function applyUserFilters() {
  usersPage.value = 1
  await loadUsers()
}

async function resetUserFilters() {
  userFilters.keyword = ''
  userFilters.status = 'all'
  userFilters.role = 'all'
  usersPage.value = 1
  await loadUsers()
}

function userModerationText(user: UserProfileView) {
  if (user.bannedUntilAt) {
    return `封禁至 ${formatDateTime(user.bannedUntilAt)}`
  }

  if (user.status === 'banned') {
    return '已永久封禁'
  }

  return '当前未封禁'
}

function renderIdentityCell(user: Pick<UserProfileView, 'avatarUrl' | 'nickname' | 'username'>, subtitle: string) {
  return h('div', { class: 'flex min-w-0 items-center gap-3' }, [
    renderAvatar(user),
    h('div', { class: 'min-w-0' }, [
      h('div', { class: 'truncate font-medium text-default' }, displayName(user)),
      h('div', { class: 'truncate text-sm text-muted' }, subtitle)
    ])
  ])
}

function renderAvatar(user: Pick<UserProfileView, 'avatarUrl' | 'nickname' | 'username'>) {
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
        用户管理
      </h2>
      <p class="mt-2 text-sm leading-7 text-muted">
        先在后台筛选和定位用户，再进入详情页执行封禁、禁言、经验与硬币调整。
      </p>
    </div>

    <form
      class="mt-5 grid gap-3 xl:grid-cols-[minmax(0,1.5fr),12rem,12rem,auto,auto]"
      @submit.prevent="applyUserFilters"
    >
      <UInput
        v-model="userFilters.keyword"
        placeholder="搜索用户名或昵称"
        icon="i-lucide-search"
        color="neutral"
        variant="outline"
        :ui="{ base: 'rounded-2xl' }"
      />
      <USelect
        v-model="userFilters.status"
        :items="userStatusItems"
        color="neutral"
        variant="outline"
      />
      <USelect
        v-model="userFilters.role"
        :items="userRoleItems"
        color="neutral"
        variant="outline"
      />
      <UButton
        type="submit"
        class="rounded-full"
        :loading="usersPending"
      >
        查询
      </UButton>
      <UButton
        type="button"
        color="neutral"
        variant="outline"
        class="rounded-full"
        @click="resetUserFilters"
      >
        重置
      </UButton>
    </form>

    <div class="mt-5 text-sm text-muted">
      共 {{ formatCount(users.total) }} 位用户
    </div>

    <div class="mt-4 overflow-hidden rounded-[1.75rem] border border-default bg-default">
      <UTable
        :data="users.list"
        :columns="userColumns"
        :loading="usersPending"
        loading-color="primary"
        loading-animation="carousel"
        empty="当前条件下没有用户。"
        sticky="header"
        class="max-h-[42rem]"
      />
    </div>

    <div
      v-if="users.total > users.pageSize"
      class="mt-5 flex justify-end"
    >
      <UPagination
        :page="usersPage"
        :items-per-page="users.pageSize"
        :total="users.total"
        :show-controls="true"
        :show-edges="true"
        @update:page="usersPage = $event"
      />
    </div>
  </section>
</template>
