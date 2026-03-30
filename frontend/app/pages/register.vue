<script setup lang="ts">
import { z } from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { LoginResponse, RegisterRequest } from '~/types/forum'

definePageMeta({
  layout: 'auth'
})

const auth = useAuth()
const api = useApi()

const schema = z.object({
  username: z
    .string()
    .trim()
    .min(3, '用户名至少 3 个字符')
    .max(50, '用户名最多 50 个字符'),
  nickname: z
    .string()
    .trim()
    .max(50, '昵称最多 50 个字符')
    .optional()
    .or(z.literal('')),
  password: z
    .string()
    .min(6, '密码至少 6 个字符')
    .max(64, '密码最多 64 个字符'),
  confirmPassword: z
    .string()
    .min(6, '确认密码至少 6 个字符')
    .max(64, '确认密码最多 64 个字符')
}).superRefine((value, ctx) => {
  if (value.password !== value.confirmPassword) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: '两次输入的密码不一致',
      path: ['confirmPassword']
    })
  }
})

type Schema = z.output<typeof schema>

const state = reactive<Schema>({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
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
    const payload: RegisterRequest = {
      username: event.data.username.trim(),
      password: event.data.password,
      nickname: event.data.nickname?.trim() || null
    }

    const response = await api.request<LoginResponse>('/api/auth/register', {
      method: 'POST',
      body: payload
    })

    auth.applyLogin(response)
    await navigateTo('/profile')
  } catch (error) {
    const apiError = error as { message?: string }
    errorMessage.value = apiError.message || '注册失败，请稍后再试'
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
            注册成功后会直接返回登录态
          </div>

          <div>
            <h1 class="text-4xl font-semibold tracking-tight text-default">
              创建论坛账户
            </h1>
            <p class="mt-3 text-sm leading-7 text-muted">
              用户名 3 到 50 个字符，密码 6 到 64 个字符。昵称可选；如果留空，后端会自动把昵称设置为用户名。
            </p>
          </div>

          <div class="grid gap-3">
            <div class="rounded-3xl border border-default bg-muted/50 px-4 py-4">
              <div class="text-sm font-semibold text-default">
                新用户默认状态
              </div>
              <div class="mt-2 text-sm leading-6 text-muted">
                `role = user`、`status = active`、`coinBalance = 0`、`level = 1`。
              </div>
            </div>
            <div class="rounded-3xl border border-default bg-muted/50 px-4 py-4">
              <div class="text-sm font-semibold text-default">
                前端落地方式
              </div>
              <div class="mt-2 text-sm leading-6 text-muted">
                注册完成后立刻持久化 access token，并跳转到个人资料页继续完善信息。
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

            <UFormField label="昵称" name="nickname">
              <UInput
                v-model="state.nickname"
                size="xl"
                placeholder="可选，不填则与用户名一致"
                :ui="{ base: 'rounded-2xl' }"
              />
            </UFormField>

            <UFormField label="密码" name="password" required>
              <UInput
                v-model="state.password"
                type="password"
                size="xl"
                placeholder="请输入密码"
                autocomplete="new-password"
                :ui="{ base: 'rounded-2xl' }"
              />
            </UFormField>

            <UFormField label="确认密码" name="confirmPassword" required>
              <UInput
                v-model="state.confirmPassword"
                type="password"
                size="xl"
                placeholder="请再次输入密码"
                autocomplete="new-password"
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
                创建账号
              </UButton>

              <UButton
                to="/login"
                color="neutral"
                variant="outline"
                size="xl"
                class="justify-center rounded-full"
              >
                已有账号，去登录
              </UButton>
            </div>
          </UForm>
        </div>
      </div>
    </section>
  </div>
</template>
