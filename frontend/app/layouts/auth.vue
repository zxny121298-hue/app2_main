<template>
  <div class="min-h-screen text-default">
    <div class="mx-auto flex min-h-screen max-w-7xl flex-col px-4 py-6 lg:px-6">
      <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <NuxtLink
          to="/"
          class="flex items-center gap-3 rounded-3xl px-1 py-1 transition hover:bg-accented/60"
        >
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-lg font-semibold text-white shadow-lg shadow-primary/20">
            论
          </div>
          <div>
            <div class="text-base font-semibold tracking-[0.18em] text-muted">
              CHAT FORUM
            </div>
            <div class="text-2xl font-semibold text-default">
              账户中心
            </div>
          </div>
        </NuxtLink>

        <div class="flex items-center gap-2">
          <UButton to="/" color="neutral" variant="outline" class="rounded-full">
            返回首页
          </UButton>
          <UColorModeButton
            color="neutral"
            variant="outline"
            class="rounded-full"
          />
        </div>
      </header>

      <main class="flex flex-1 items-center justify-center py-8">
        <slot />
      </main>
    </div>

    <ClientOnly>
      <AiChatFab />
    </ClientOnly>
  </div>
</template>
