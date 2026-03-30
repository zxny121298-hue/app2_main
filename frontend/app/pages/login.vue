<script setup lang="ts">
import { z } from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { LoginRequest, LoginResponse } from '~/types/forum'

definePageMeta({
  layout: 'auth'
})

const route = useRoute()
const auth = useAuth()
const api = useApi()

const redirectTarget = computed(() => {
  const value = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  return typeof value === 'string' && value.startsWith('/') ? value : '/profile'
})

const schema = z.object({
  username: z
    .string()
    .trim()
    .min(3, '用户名至少 3 个字符')
    .max(50, '用户名最多 50 个字符'),
  password: z
    .string()
    .min(6, '密码至少 6 个字符')
    .max(64, '密码最多 64 个字符')
})

type Schema = z.output<typeof schema>

const state = reactive<Schema>({
  username: '',
  password: ''
})

const pending = ref(false)
const errorMessage = ref('')

async function onSubmit(event: FormSubmitEvent<Schema>) {
  if (pending.value) {
    return
  }

  pending.value = true
  errorMessage.value = ''

  try {
    const payload: LoginRequest = {
      username: event.data.username.trim(),
      password: event.data.password
    }

    const response = await api.request<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: payload
    })

    auth.applyLogin(response)
    await navigateTo(redirectTarget.value)
  } catch (error) {
    const apiError = error as { message?: string }
    errorMessage.value = apiError.message || '登录失败，请稍后再试'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-3xl">
    <section class="rounded-[2rem] border border-default bg-elevated/90 p-8 shadow-2xl shadow-slate-900/10 backdrop-blur">
      <div class="grid gap-8 lg:grid-cols-[0.95fr,1.05fr] lg:items-center">
        <div class="space-y-5">
          <div class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
            登录后可发帖、评论、私信、签到
          </div>

          <div>
            <h1 class="text-4xl font-semibold tracking-tight text-default">
              登录论坛账户
            </h1>
            <p class="mt-3 text-sm leading-7 text-muted">
              后端采用 Bearer Token 方案，注册和登录都会直接返回 access token。前端会将其持久化并自动附带到后续请求中。
            </p>
          </div>

          <div class="grid gap-3">
            <div class="rounded-3xl border border-default bg-muted/50 px-4 py-4">
              <div class="text-sm font-semibold text-default">
                权限规则
              </div>
              <div class="mt-2 text-sm leading-6 text-muted">
                登录态可访问资料、收藏、消息、通知等页面；发言类接口还会受封禁和禁言状态约束。
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-4 py-4">
              <div class="text-sm font-semibold text-default">
                响应规则
              </div>
              <div class="mt-2 text-sm leading-6 text-muted">
                前端统一读取响应体里的 `code` 与 `message`，而不是只依赖 HTTP 状态码。
              </div>
            </div>
          </div>
        </div>

        <div class="rounded-[2rem] border border-default bg-default p-6 shadow-sm shadow-slate-900/5">
          <UForm
            :schema="schema"
            :state="state"
            class="space-y-5"
            @submit="onSubmit"
          >
            <UFormField label="用户名" name="username" required>
              <UInput
                v-model="state.username"
                size="xl"
                placeholder="请输入用户名"
                autocomplete="username"
                :ui="{ base: 'rounded-2xl' }"
              />
            </UFormField>

            <UFormField label="密码" name="password" required>
              <UInput
                v-model="state.password"
                type="password"
                size="xl"
                placeholder="请输入密码"
                autocomplete="current-password"
                :ui="{ base: 'rounded-2xl' }"
              />
            </UFormField>

            <UAlert
              v-if="errorMessage"
              color="error"
              variant="soft"
              icon="i-lucide-circle-alert"
              :title="errorMessage"
            />

            <div class="flex flex-col gap-3 pt-2 sm:flex-row">
              <UButton
                type="submit"
                :loading="pending"
                size="xl"
                class="justify-center rounded-full bg-primary text-white hover:bg-primary/90"
              >
                立即登录
              </UButton>

              <UButton
                to="/register"
                color="neutral"
                variant="outline"
                size="xl"
                class="justify-center rounded-full"
              >
                创建新账号
              </UButton>
            </div>
          </UForm>
        </div>
      </div>
    </section>
  </div>
</template>
