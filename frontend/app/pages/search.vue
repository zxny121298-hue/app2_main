<script setup lang="ts">
import type {
  BoardView,
  PageResult,
  PostCardView,
  SearchHistoryView,
  SearchType,
  UnifiedSearchResponse,
  UserProfileView,
} from "~/types/forum";
import type { LocationQueryValue } from "vue-router";
import {
  displayName,
  formatCount,
  formatDateTime,
  initialsOf,
  truncateText,
} from "~/utils/forum";

const route = useRoute();
const toast = useToast();
const auth = useAuth();
const api = useApi();

const pageSize = 10;
const searchType = ref<SearchType>("content");
const queryInput = ref("");
const page = ref(1);
const pending = ref(false);
const errorMessage = ref("");

const posts = ref<PageResult<PostCardView>>({
  total: 0,
  page: 1,
  pageSize,
  list: [],
});
const users = ref<PageResult<UserProfileView>>({
  total: 0,
  page: 1,
  pageSize,
  list: [],
});
const topics = ref<PageResult<BoardView>>({
  total: 0,
  page: 1,
  pageSize,
  list: [],
});

const historyOpen = ref(false);
const historyLoading = ref(false);
const historyItems = ref<SearchHistoryView[]>([]);

const isLoggedIn = computed(() => auth.isLoggedIn.value);

const typeTabs = [
  { label: "帖子", value: "content" as const },
  { label: "用户", value: "user" as const },
  { label: "话题", value: "topic" as const },
];

function readQueryString(
  value: LocationQueryValue | LocationQueryValue[] | undefined,
): string | undefined {
  if (value == null) {
    return undefined;
  }
  if (Array.isArray(value)) {
    const first = value[0];
    return first ?? undefined;
  }
  return value;
}

function syncFromRoute() {
  const rawType = readQueryString(route.query.type);
  if (rawType === "content" || rawType === "user" || rawType === "topic") {
    searchType.value = rawType;
  }
  const rawQ =
    readQueryString(route.query.q) ?? readQueryString(route.query.keyword);
  queryInput.value = rawQ?.trim() ?? "";

  const rawPage = Number(readQueryString(route.query.page) ?? 1);
  page.value = Number.isFinite(rawPage) && rawPage > 0 ? rawPage : 1;
}

async function fetchResults() {
  pending.value = true;
  errorMessage.value = "";
  try {
    const res = await api.request<UnifiedSearchResponse>("/api/search", {
      query: {
        type: searchType.value,
        ...(queryInput.value.trim()
          ? { q: queryInput.value.trim() }
          : {}),
        page: page.value,
        pageSize,
      },
    });
    if (res.posts) {
      posts.value = res.posts;
    }
    if (res.users) {
      users.value = res.users;
    }
    if (res.topics) {
      topics.value = res.topics;
    }
  } catch (error) {
    const apiError = error as { message?: string };
    errorMessage.value = apiError.message || "搜索失败";
  } finally {
    pending.value = false;
  }
}

async function pushSearchQuery(extra: Record<string, string | number> = {}) {
  const q = queryInput.value.trim();
  await navigateTo({
    path: "/search",
    query: {
      type: searchType.value,
      ...(q ? { q } : {}),
      ...(page.value > 1 ? { page: page.value } : {}),
      ...extra,
    },
  });
}

async function handleSubmit() {
  page.value = 1;
  historyOpen.value = false;
  await pushSearchQuery();
}

async function handleTypeChange(next: SearchType) {
  if (next === searchType.value) {
    return;
  }
  searchType.value = next;
  page.value = 1;
  await pushSearchQuery();
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  await pushSearchQuery();
}

async function loadHistory() {
  if (!isLoggedIn.value) {
    historyItems.value = [];
    return;
  }
  historyLoading.value = true;
  try {
    historyItems.value = await api.request<SearchHistoryView[]>(
      "/api/search/history",
      { query: { limit: 10 } },
    );
  } catch {
    historyItems.value = [];
  } finally {
    historyLoading.value = false;
  }
}

function onQueryFocus() {
  if (!queryInput.value.trim()) {
    historyOpen.value = true;
    void loadHistory();
  }
}

function onQueryInput() {
  if (queryInput.value.trim()) {
    historyOpen.value = false;
  }
}

function applyHistoryItem(item: SearchHistoryView) {
  searchType.value = item.searchType;
  queryInput.value = item.keyword;
  historyOpen.value = false;
  page.value = 1;
  void navigateTo({
    path: "/search",
    query: {
      type: item.searchType,
      q: item.keyword,
    },
  });
}

async function handleToggleLike(post: PostCardView) {
  if (!auth.isLoggedIn.value) {
    await navigateTo("/login?redirect=/search");
    return;
  }
  try {
    await api.request<void>(`/api/posts/${post.id}/like`, {
      method: post.liked ? "DELETE" : "POST",
    });
    post.liked = !post.liked;
    post.likeCount += post.liked ? 1 : -1;
  } catch (error) {
    const apiError = error as { message?: string };
    toast.add({
      title: "操作失败",
      description: apiError.message || "请稍后再试",
      color: "error",
    });
  }
}

async function handleToggleFavorite(post: PostCardView) {
  if (!auth.isLoggedIn.value) {
    await navigateTo("/login?redirect=/search");
    return;
  }
  try {
    await api.request<void>(`/api/posts/${post.id}/favorite`, {
      method: post.favorited ? "DELETE" : "POST",
    });
    post.favorited = !post.favorited;
  } catch (error) {
    const apiError = error as { message?: string };
    toast.add({
      title: "操作失败",
      description: apiError.message || "请稍后再试",
      color: "error",
    });
  }
}

watch(
  () => route.fullPath,
  async () => {
    syncFromRoute();
    await fetchResults();
  },
  { immediate: true },
);
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-8">
    <section
      class="rounded-[2rem] border border-default bg-elevated/85 p-6 shadow-xl shadow-slate-900/5 backdrop-blur"
    >
      <div
        class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
      >
        统一搜索 · GET /api/search
      </div>
      <h1 class="mt-4 text-3xl font-semibold tracking-tight text-default">
        搜索
      </h1>
      <p class="mt-2 text-sm leading-6 text-muted">
        按类型搜索帖子、用户或板块（话题）；登录后自动记录搜索历史，聚焦输入框可查看最近记录。
      </p>

      <div class="mt-6 flex flex-wrap gap-2">
        <UButton
          v-for="tab in typeTabs"
          :key="tab.value"
          size="sm"
          class="rounded-full"
          :color="searchType === tab.value ? 'primary' : 'neutral'"
          :variant="searchType === tab.value ? 'solid' : 'outline'"
          @click="handleTypeChange(tab.value)"
        >
          {{ tab.label }}
        </UButton>
      </div>

      <form class="relative mt-6 flex flex-col gap-3 sm:flex-row" @submit.prevent="handleSubmit">
        <UInput
          v-model="queryInput"
          size="lg"
          class="w-full flex-1"
          placeholder="输入关键词（留空则暂无结果）"
          icon="i-lucide-search"
          autocomplete="off"
          @focus="onQueryFocus"
          @input="onQueryInput"
        />
        <UButton
          type="submit"
          class="shrink-0 rounded-full bg-primary text-white hover:bg-primary/90"
        >
          搜索
        </UButton>

        <div
          v-if="historyOpen && isLoggedIn"
          class="absolute left-0 right-0 top-full z-30 mt-2 overflow-hidden rounded-2xl border border-default bg-elevated shadow-xl"
        >
          <div
            class="border-b border-default px-4 py-2 text-xs font-medium text-muted"
          >
            最近搜索
          </div>
          <div v-if="historyLoading" class="px-4 py-6 text-sm text-muted">
            加载中…
          </div>
          <ul v-else-if="historyItems.length" class="max-h-64 divide-y divide-default overflow-y-auto">
            <li
              v-for="item in historyItems"
              :key="item.id"
            >
              <button
                type="button"
                class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition hover:bg-accented/60"
                @mousedown.prevent="applyHistoryItem(item)"
              >
                <span class="truncate font-medium text-default">{{
                  item.keyword
                }}</span>
                <span class="shrink-0 text-xs text-muted">{{
                  item.searchType === "content"
                    ? "帖子"
                    : item.searchType === "user"
                      ? "用户"
                      : "话题"
                }}</span>
              </button>
            </li>
          </ul>
          <div v-else class="px-4 py-6 text-sm text-muted">
            暂无历史，先搜索几次吧。
          </div>
        </div>
      </form>
    </section>

    <UAlert
      v-if="errorMessage"
      color="error"
      variant="soft"
      icon="i-lucide-circle-alert"
      :title="errorMessage"
    />

    <section v-if="searchType === 'content'" class="space-y-4">
      <div
        v-if="pending"
        class="grid gap-4"
      >
        <div
          v-for="item in 3"
          :key="item"
          class="h-40 animate-pulse rounded-[2rem] border border-default bg-muted/50"
        />
      </div>
      <template v-else>
        <article
          v-for="post in posts.list"
          :key="post.id"
          class="rounded-[2rem] border border-default bg-elevated/90 p-6 shadow-lg shadow-slate-900/5 backdrop-blur transition hover:-translate-y-0.5 hover:shadow-xl"
        >
          <div class="flex flex-col gap-4">
            <div class="flex flex-wrap items-center gap-2">
              <span
                v-if="post.pinned"
                class="rounded-full bg-rose-50 px-3 py-1 text-xs font-semibold text-rose-700"
              >
                置顶
              </span>
              <span
                v-if="post.featured"
                class="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700"
              >
                加精
              </span>
              <span
                class="rounded-full border border-default bg-muted px-3 py-1 text-xs font-semibold text-muted"
              >
                {{ post.boardName }}
              </span>
            </div>
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="min-w-0 flex-1">
                <NuxtLink
                  :to="`/posts/${post.id}`"
                  class="text-xl font-semibold leading-8 text-default transition hover:text-primary"
                >
                  {{ post.title }}
                </NuxtLink>
                <p class="mt-2 text-sm leading-7 text-muted">
                  {{
                    truncateText(post.contentText, 200) ||
                      "该帖子仅包含图片内容。"
                  }}
                </p>
              </div>
              <div
                class="flex items-center gap-3 rounded-3xl border border-default bg-muted/50 px-4 py-3 text-sm text-muted lg:w-64"
              >
                <div
                  class="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-full border border-default bg-primary text-xs font-semibold text-white"
                >
                  <img
                    v-if="post.author.avatarUrl"
                    :src="post.author.avatarUrl"
                    :alt="displayName(post.author)"
                    class="h-full w-full object-cover"
                  />
                  <span v-else>{{ initialsOf(post.author) }}</span>
                </div>
                <div class="min-w-0">
                  <NuxtLink
                    :to="`/users/${post.author.id}`"
                    class="block truncate font-semibold text-default transition hover:text-primary"
                  >
                    {{ displayName(post.author) }}
                  </NuxtLink>
                  <div class="mt-0.5 text-xs text-muted">
                    {{ formatDateTime(post.createdAt) }}
                  </div>
                </div>
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <UButton
                color="neutral"
                variant="outline"
                size="sm"
                class="rounded-full"
                @click="handleToggleLike(post)"
              >
                {{ post.liked ? "取消点赞" : "点赞" }} ·
                {{ formatCount(post.likeCount) }}
              </UButton>
              <UButton
                color="neutral"
                variant="outline"
                size="sm"
                class="rounded-full"
                @click="handleToggleFavorite(post)"
              >
                {{ post.favorited ? "取消收藏" : "收藏" }}
              </UButton>
              <UButton
                size="sm"
                :to="`/posts/${post.id}`"
                class="rounded-full bg-primary text-white hover:bg-primary/90"
              >
                详情
              </UButton>
            </div>
          </div>
        </article>

        <EmptyState
          v-if="!posts.list.length"
          title="没有找到帖子"
          description="换个关键词，或切换到其他搜索类型试试。"
          icon="i-lucide-file-search"
        />

        <div v-if="posts.list.length" class="flex justify-end pt-2">
          <UPagination
            :page="page"
            :items-per-page="pageSize"
            :total="posts.total"
            :show-controls="true"
            :show-edges="true"
            @update:page="handlePageChange"
          />
        </div>
      </template>
    </section>

    <section v-else-if="searchType === 'user'" class="space-y-4">
      <div v-if="pending" class="grid gap-3">
        <div
          v-for="item in 5"
          :key="item"
          class="h-20 animate-pulse rounded-2xl border border-default bg-muted/50"
        />
      </div>
      <template v-else>
        <NuxtLink
          v-for="u in users.list"
          :key="u.id"
          :to="`/users/${u.id}`"
          class="flex items-center gap-4 rounded-2xl border border-default bg-elevated/90 p-4 transition hover:border-primary/40 hover:bg-accented/40"
        >
          <div
            class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary text-sm font-semibold text-white"
          >
            <img
              v-if="u.avatarUrl"
              :src="u.avatarUrl"
              :alt="displayName(u)"
              class="h-full w-full object-cover"
            />
            <span v-else>{{ initialsOf(u) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="truncate font-semibold text-default">
              {{ displayName(u) }}
            </div>
            <div class="truncate text-xs text-muted">
              @{{ u.username }} · Lv.{{ u.level }}
            </div>
          </div>
          <UIcon name="i-lucide-chevron-right" class="size-5 shrink-0 text-muted" />
        </NuxtLink>

        <EmptyState
          v-if="!users.list.length"
          title="没有找到用户"
          description="试试昵称、用户名或数字 ID。"
          icon="i-lucide-users"
        />

        <div v-if="users.list.length" class="flex justify-end pt-2">
          <UPagination
            :page="page"
            :items-per-page="pageSize"
            :total="users.total"
            :show-controls="true"
            :show-edges="true"
            @update:page="handlePageChange"
          />
        </div>
      </template>
    </section>

    <section v-else class="space-y-4">
      <div v-if="pending" class="grid gap-3">
        <div
          v-for="item in 4"
          :key="item"
          class="h-24 animate-pulse rounded-2xl border border-default bg-muted/50"
        />
      </div>
      <template v-else>
        <NuxtLink
          v-for="b in topics.list"
          :key="b.id"
          :to="{ path: '/', query: { boardId: b.id } }"
          class="block rounded-2xl border border-default bg-elevated/90 p-5 transition hover:border-primary/40 hover:bg-accented/40"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="text-lg font-semibold text-default">
                {{ b.name }}
              </div>
              <p class="mt-1 text-sm leading-6 text-muted">
                {{ b.description || "暂无简介" }}
              </p>
            </div>
            <UIcon name="i-lucide-layout-grid" class="size-6 shrink-0 text-primary" />
          </div>
        </NuxtLink>

        <EmptyState
          v-if="!topics.list.length"
          title="没有找到板块"
          description="话题与板块共用数据，仅搜索已启用板块。"
          icon="i-lucide-hash"
        />

        <div v-if="topics.list.length" class="flex justify-end pt-2">
          <UPagination
            :page="page"
            :items-per-page="pageSize"
            :total="topics.total"
            :show-controls="true"
            :show-edges="true"
            @update:page="handlePageChange"
          />
        </div>
      </template>
    </section>
  </div>
</template>
