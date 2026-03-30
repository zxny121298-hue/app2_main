<script setup lang="ts">
import type { UserProfileView } from "~/types/forum";
import { displayName, formatCount, initialsOf } from "~/utils/forum";

const route = useRoute();
const toast = useToast();
const auth = useAuth();
const api = useApi();
const { headerSearchExpanded } = useHeaderSearchExpand();
const { loadBoards, unreadCount, refreshUnreadCount } = useForumMeta();
const isLoggedIn = computed(() => auth.isLoggedIn.value);
const isAdmin = computed(() => auth.isAdmin.value);
const currentProfile = computed(() => auth.profile.value);
const unreadPollInterval = 30000;
let unreadPollingTimer: number | null = null;

const navigationItems = computed(() => {
  const items = [
    { label: "首页", to: "/" },
    { label: "我的资料", to: "/profile", auth: true },
    { label: "消息", to: "/messages", auth: true },
    {
      label: "通知",
      to: "/notifications",
      auth: true,
      badge: unreadCount.value,
    },
    { label: "管理", to: "/admin", auth: true, admin: true },
  ];

  return items.filter((item) => {
    if (item.admin && !auth.isAdmin.value) {
      return false;
    }

    if (item.auth && !auth.isLoggedIn.value) {
      return false;
    }

    return true;
  });
});

async function refreshProfile() {
  if (!auth.isLoggedIn.value) {
    return;
  }

  try {
    const profile = await api.request<UserProfileView>("/api/users/me");
    auth.setProfile(profile);
  } catch (error) {
    const apiError = error as { message?: string };

    toast.add({
      title: "用户状态刷新失败",
      description: apiError.message || "请稍后重试",
      color: "warning",
    });
  }
}

async function handleLogout() {
  await auth.logout();
}

async function syncUnreadCount() {
  try {
    await refreshUnreadCount();
  } catch {
    unreadCount.value = 0;
  }
}

function stopUnreadPolling() {
  if (!unreadPollingTimer) {
    return;
  }

  clearInterval(unreadPollingTimer);
  unreadPollingTimer = null;
}

function startUnreadPolling() {
  if (!import.meta.client || unreadPollingTimer) {
    return;
  }

  unreadPollingTimer = window.setInterval(async () => {
    if (!auth.isLoggedIn.value || document.hidden) {
      return;
    }

    await syncUnreadCount();
  }, unreadPollInterval);
}

onMounted(async () => {
  try {
    await loadBoards();
  } catch (error) {
    const apiError = error as { message?: string };

    toast.add({
      title: "板块加载失败",
      description: apiError.message || "请检查后端服务是否已启动",
      color: "error",
    });
  }

  if (auth.isLoggedIn.value) {
    await Promise.allSettled([refreshProfile(), refreshUnreadCount()]);
    startUnreadPolling();
  }
});

watch(
  () => [route.fullPath, auth.isLoggedIn.value],
  async ([, loggedIn]) => {
    if (!loggedIn) {
      stopUnreadPolling();
      unreadCount.value = 0;
      return;
    }

    startUnreadPolling();
    await syncUnreadCount();
  },
);

onBeforeUnmount(() => {
  stopUnreadPolling();
});
</script>

<template>
  <div class="app-shell min-h-screen text-default">
    <header
      class="sticky top-0 z-40 border-b border-default bg-elevated/80 backdrop-blur-xl"
    >
      <div class="mx-auto flex max-w-7xl px-4 py-4 lg:px-6">
        <div
          class="flex w-full flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
        >
          <div class="flex items-center gap-3">
            <NuxtLink
              to="/"
              class="flex items-center gap-3 rounded-3xl px-1 py-1 transition hover:bg-accented/70"
            >
              <div
                class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-lg font-semibold text-white shadow-lg shadow-primary/20"
              >
                论
              </div>
              <div>
                <div
                  class="text-base font-semibold tracking-[0.18em] text-muted"
                >
                  CHAT FORUM
                </div>
                <div class="text-2xl font-semibold text-default">校园论坛</div>
              </div>
            </NuxtLink>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <Transition
              enter-active-class="transition duration-200 ease-out"
              leave-active-class="transition duration-200 ease-out"
              enter-from-class="opacity-0 -translate-y-1"
              leave-to-class="opacity-0 -translate-y-1"
            >
              <div
                v-show="!headerSearchExpanded"
                key="main-nav"
                class="flex flex-wrap items-center gap-2"
              >
                <NuxtLink
                  v-for="item in navigationItems"
                  :key="item.to"
                  :to="item.to"
                  class="inline-flex items-center gap-2 rounded-full border border-transparent px-4 py-2 text-sm font-medium text-muted transition hover:border-default hover:bg-accented/60"
                  active-class="border-primary bg-primary text-white shadow-lg shadow-primary/20"
                >
                  <span>{{ item.label }}</span>
                  <span
                    v-if="item.badge"
                    class="rounded-full bg-rose-500 px-2 py-0.5 text-xs font-semibold text-white"
                  >
                    {{ formatCount(item.badge) }}
                  </span>
                </NuxtLink>
              </div>
            </Transition>

            <template v-if="isLoggedIn">
              <NuxtLink
                to="/?compose=1"
                class="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-primary/20 transition hover:bg-primary/90"
              >
                <UIcon name="i-lucide-square-pen" class="size-4" />
                发布帖子
              </NuxtLink>

              <AppHeaderSearch />

              <div
                class="flex items-center gap-3 rounded-full border border-default bg-default px-3 py-2"
              >
                <div
                  class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full bg-primary text-sm font-semibold text-white"
                >
                  <img
                    v-if="currentProfile?.avatarUrl"
                    :src="currentProfile.avatarUrl"
                    :alt="displayName(currentProfile)"
                    class="h-full w-full object-cover"
                  />
                  <span v-else>{{ initialsOf(currentProfile) }}</span>
                </div>

                <div class="hidden min-w-0 sm:block">
                  <div class="truncate text-sm font-semibold text-default">
                    {{ displayName(currentProfile) }}
                  </div>
                  <div class="text-xs text-muted">
                    {{ isAdmin ? "管理员" : "普通用户" }}
                  </div>
                </div>

                <UButton
                  color="neutral"
                  variant="ghost"
                  size="sm"
                  icon="i-lucide-log-out"
                  @click="handleLogout"
                />
              </div>
            </template>

            <template v-else>
              <UButton
                to="/login"
                color="neutral"
                variant="outline"
                class="rounded-full"
              >
                登录
              </UButton>
              <UButton
                to="/register"
                class="rounded-full bg-primary text-white hover:bg-primary/90"
              >
                注册
              </UButton>
              <AppHeaderSearch />
            </template>

            <UColorModeButton
              color="neutral"
              variant="outline"
              class="rounded-full"
            />
          </div>
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-7xl px-4 py-6 lg:px-6">
      <slot />
    </main>

    <ClientOnly>
      <AiChatFab />
    </ClientOnly>
  </div>
</template>
