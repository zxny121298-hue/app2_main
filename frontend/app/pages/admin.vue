<script setup lang="ts">
import type { NavigationMenuItem } from "@nuxt/ui";
import type {
  BoardView,
  PageResult,
  PostCardView,
  UserProfileView,
} from "~/types/forum";
import { formatCount } from "~/utils/forum";
import AdminBoardsPanel from "~/components/admin/AdminBoardsPanel.vue";
import AdminUsersPanel from "~/components/admin/AdminUsersPanel.vue";
import AdminPostsPanel from "~/components/admin/AdminPostsPanel.vue";

definePageMeta({
  middleware: "admin",
});

type AdminModule = "boards" | "users" | "posts";

const route = useRoute();
const toast = useToast();
const api = useApi();

const activeModule = ref<AdminModule>("boards");
const boardCount = ref(0);
const userCount = ref(0);
const postCount = ref(0);

const moduleCards = computed(() => [
  {
    id: "boards" as AdminModule,
    icon: "i-lucide-layout-grid",
    title: "板块管理",
    description: "创建、排序、启停论坛板块，并处理批量状态变更。",
    count: `${formatCount(boardCount.value)} 个板块`,
  },
  {
    id: "users" as AdminModule,
    icon: "i-lucide-users",
    title: "用户管理",
    description: "筛选用户并进入详情页执行封禁、禁言和数值调整。",
    count: `${formatCount(userCount.value)} 位用户`,
  },
  {
    id: "posts" as AdminModule,
    icon: "i-lucide-files",
    title: "帖子管理",
    description: "筛选正常状态帖子，直接执行置顶、加精、删除和查看详情。",
    count: `${formatCount(postCount.value)} 篇帖子`,
  },
]);

const currentModuleCard = computed(
  () =>
    moduleCards.value.find((item) => item.id === activeModule.value) ??
    moduleCards.value[0]!,
);

const moduleNavigationItems = computed<NavigationMenuItem[][]>(() => [
  [
    ...moduleCards.value.map((item) => ({
      label: item.title,
      icon: item.icon,
      badge: item.count.replace(/ .*/, ""),
      active: activeModule.value === item.id,
      onSelect: (event: Event) => {
        event.preventDefault();
        void setActiveModule(item.id);
      },
    })),
  ],
]);

watch(
  () => route.query.module,
  async (value) => {
    const rawValue = Array.isArray(value) ? value[0] : value;
    const nextModule = parseAdminModule(rawValue);
    activeModule.value = nextModule;

    if (rawValue !== nextModule) {
      await replaceModuleQuery(nextModule);
    }
  },
  { immediate: true },
);

onMounted(loadSummary);

function parseAdminModule(value: unknown): AdminModule {
  return value === "users" || value === "posts" || value === "boards"
    ? value
    : "boards";
}

async function replaceModuleQuery(module: AdminModule) {
  await navigateTo(
    {
      path: "/admin",
      query: {
        module,
      },
    },
    {
      replace: true,
    },
  );
}

async function setActiveModule(module: AdminModule) {
  activeModule.value = module;

  const rawValue = Array.isArray(route.query.module)
    ? route.query.module[0]
    : route.query.module;
  if (rawValue === module) {
    return;
  }

  await replaceModuleQuery(module);
}

async function loadSummary() {
  try {
    const [boardList, userPage, postPage] = await Promise.all([
      api.request<BoardView[]>("/api/boards", {
        query: {
          includeDisabled: true,
        },
      }),
      api.request<PageResult<UserProfileView>>("/api/admin/users", {
        query: {
          page: 1,
          pageSize: 1,
        },
      }),
      api.request<PageResult<PostCardView>>("/api/admin/posts", {
        query: {
          page: 1,
          pageSize: 1,
        },
      }),
    ]);

    boardCount.value = boardList.length;
    userCount.value = userPage.total;
    postCount.value = postPage.total;
  } catch (error) {
    const apiError = error as { message?: string };

    toast.add({
      title: "后台概览加载失败",
      description: apiError.message || "请稍后重试",
      color: "error",
    });
  }
}
</script>

<template>
  <div
    class="space-y-6 grid grid-cols-[240px_1fr] lg:items-start lg:gap-6 lg:space-y-0"
  >
    <aside class="hidden md:block">
      <div
        class="sticky top-[calc(var(--app-header-height,0px)+1rem)] space-y-4 rounded-[2rem] border border-default bg-elevated/90 p-4 shadow-xl shadow-slate-900/5 backdrop-blur"
      >
        <div class="space-y-2 px-2">
          <div
            class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
          >
            后台工作台
          </div>
          <div class="text-lg font-semibold text-default">模块导航</div>
          <p class="text-sm leading-6 text-muted">
            在同一工作区管理板块、用户和帖子。
          </p>
        </div>

        <UNavigationMenu
          orientation="vertical"
          color="neutral"
          highlight
          class="data-[orientation=vertical]:w-full"
          :items="moduleNavigationItems"
        />
      </div>
    </aside>

    <div class="min-w-0 space-y-4">
      <div
        class="md:hidden rounded-[2rem] border border-default bg-elevated/90 p-3 shadow-lg shadow-slate-900/5 backdrop-blur"
      >
        <UNavigationMenu
          color="neutral"
          highlight
          class="w-full"
          :items="moduleNavigationItems"
        />
      </div>

      <section
        class="rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-xl shadow-slate-900/5 backdrop-blur"
      >
        <div
          class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between"
        >
          <div class="min-w-0">
            <div
              class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
            >
              当前模块
            </div>
            <h1 class="mt-4 text-3xl font-semibold tracking-tight text-default">
              {{ currentModuleCard.title }}
            </h1>
            <p class="mt-3 max-w-3xl text-sm leading-7 text-muted">
              {{ currentModuleCard.description }}
            </p>
          </div>

          <div
            class="rounded-3xl border border-default bg-muted/50 px-4 py-4 text-right"
          >
            <div
              class="text-xs font-semibold uppercase tracking-[0.18em] text-muted"
            >
              当前总量
            </div>
            <div class="mt-2 text-2xl font-semibold text-default">
              {{ currentModuleCard.count }}
            </div>
          </div>
        </div>
      </section>

      <AdminBoardsPanel
        v-if="activeModule === 'boards'"
        @refresh-summary="loadSummary"
      />
      <AdminUsersPanel v-else-if="activeModule === 'users'" />
      <AdminPostsPanel v-else @refresh-summary="loadSummary" />
    </div>
  </div>
</template>
