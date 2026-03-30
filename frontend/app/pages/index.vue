<script setup lang="ts">
import type { PageResult, PostCardView } from "~/types/forum";
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
const { boards, loadBoards } = useForumMeta();
const isLoggedIn = computed(() => auth.isLoggedIn.value);

const composeOpen = ref(false);
const pending = ref(false);
const errorMessage = ref("");
const pageSize = 10;
const page = ref(1);
const posts = ref<PageResult<PostCardView>>({
  total: 0,
  page: 1,
  pageSize,
  list: [],
});

const selectedBoardId = computed(() => {
  const rawValue = Array.isArray(route.query.boardId)
    ? route.query.boardId[0]
    : route.query.boardId;
  const parsed = Number(rawValue || 0);
  return Number.isFinite(parsed) ? parsed : 0;
});

const selectedBoard = computed(
  () => boards.value.find((item) => item.id === selectedBoardId.value) || null,
);
const highlightedCount = computed(
  () => posts.value.list.filter((item) => item.pinned || item.featured).length,
);

async function loadPosts() {
  pending.value = true;
  errorMessage.value = "";

  try {
    posts.value = await api.request<PageResult<PostCardView>>("/api/posts", {
      query: {
        boardId: selectedBoardId.value || undefined,
        page: page.value,
        pageSize,
      },
    });
  } catch (error) {
    const apiError = error as { message?: string };
    errorMessage.value = apiError.message || "帖子列表加载失败";
  } finally {
    pending.value = false;
  }
}

async function setBoard(nextBoardId = 0) {
  await navigateTo({
    path: "/",
    query: {
      ...(nextBoardId ? { boardId: nextBoardId } : {}),
    },
  });
}

async function handleToggleLike(post: PostCardView) {
  if (!auth.isLoggedIn.value) {
    await navigateTo("/login?redirect=/");
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
      title: "帖子点赞失败",
      description: apiError.message || "请稍后再试",
      color: "error",
    });
  }
}

async function handleToggleFavorite(post: PostCardView) {
  if (!auth.isLoggedIn.value) {
    await navigateTo("/login?redirect=/");
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
      title: "帖子收藏失败",
      description: apiError.message || "请稍后再试",
      color: "error",
    });
  }
}

async function handlePageChange(nextPage: number) {
  await navigateTo({
    path: "/",
    query: {
      ...(selectedBoardId.value ? { boardId: selectedBoardId.value } : {}),
      ...(nextPage > 1 ? { page: nextPage } : {}),
    },
  });
}

watch(
  () => [route.query.boardId, route.query.page],
  async () => {
    const rawPage = Array.isArray(route.query.page)
      ? route.query.page[0]
      : route.query.page;
    const parsedPage = Number(rawPage || 1);
    page.value = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1;

    await loadPosts();
  },
  { immediate: true },
);

watch(
  () => route.query.compose,
  async (value) => {
    const compose = Array.isArray(value) ? value[0] : value;

    if (compose === "1") {
      if (!auth.isLoggedIn.value) {
        await navigateTo("/login?redirect=/%3Fcompose%3D1");
        return;
      }

      composeOpen.value = true;
    }
  },
  { immediate: true },
);

watch(composeOpen, async (value) => {
  if (!value && route.query.compose === "1") {
    await navigateTo(
      {
        path: "/",
        query: {
          ...(selectedBoardId.value ? { boardId: selectedBoardId.value } : {}),
          ...(page.value > 1 ? { page: page.value } : {}),
        },
      },
      {
        replace: true,
      },
    );
  }
});

onMounted(async () => {
  await loadBoards();
});
</script>

<template>
  <div class="grid grid-cols-[240px_1fr] gap-6">
    <aside>
      <div
        class="sticky top-(--app-header-height) rounded-4xl border border-default bg-elevated/90 p-4 shadow-xl shadow-slate-900/5 backdrop-blur"
      >
        <div class="hidden lg:block space-y-4">
          <div class="space-y-2 px-2">
            <div
              class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
            >
              板块导航
            </div>
            <!-- <div class="text-lg font-semibold text-default">
              {{ selectedBoard ? selectedBoard.name : "全部板块" }}
            </div>
            <p class="text-sm leading-6 text-muted">
              {{
                selectedBoard?.description ||
                "按板块筛选首页帖子，快速切换当前浏览范围。"
              }}
            </p> -->
          </div>

          <div class="flex flex-col gap-2">
            <button
              type="button"
              class="flex items-center justify-between rounded-2xl border px-4 py-3 text-left text-sm font-medium transition"
              :class="
                selectedBoardId === 0
                  ? 'border-primary bg-primary text-white shadow-lg shadow-primary/20'
                  : 'border-default bg-default text-default hover:bg-accented'
              "
              @click="setBoard()"
            >
              <span>全部板块</span>
              <UIcon name="i-lucide-chevron-right" class="size-4 opacity-70" />
            </button>

            <button
              v-for="board in boards"
              :key="board.id"
              type="button"
              class="flex items-center justify-between rounded-2xl border px-4 py-3 text-left text-sm font-medium transition"
              :class="
                selectedBoardId === board.id
                  ? 'border-primary bg-primary text-white shadow-lg shadow-primary/20'
                  : 'border-default bg-default text-default hover:bg-accented'
              "
              @click="setBoard(board.id)"
            >
              <span class="truncate">{{ board.name }}</span>
              <UIcon
                name="i-lucide-chevron-right"
                class="size-4 shrink-0 opacity-70"
              />
            </button>
          </div>
        </div>

        <div class="lg:hidden overflow-x-auto">
          <div class="flex gap-2 pb-1">
            <button
              type="button"
              class="whitespace-nowrap rounded-full border px-4 py-2 text-sm font-medium transition"
              :class="
                selectedBoardId === 0
                  ? 'border-primary bg-primary text-white'
                  : 'border-default bg-default text-muted hover:bg-accented'
              "
              @click="setBoard()"
            >
              全部板块
            </button>

            <button
              v-for="board in boards"
              :key="board.id"
              type="button"
              class="whitespace-nowrap rounded-full border px-4 py-2 text-sm font-medium transition"
              :class="
                selectedBoardId === board.id
                  ? 'border-primary bg-primary text-white'
                  : 'border-default bg-default text-muted hover:bg-accented'
              "
              @click="setBoard(board.id)"
            >
              {{ board.name }}
            </button>
          </div>
        </div>
      </div>
    </aside>

    <div class="min-w-0 space-y-6">
      <section
        class="overflow-hidden rounded-[2rem] border border-default bg-elevated/85 p-6 shadow-xl shadow-slate-900/5 backdrop-blur"
      >
        <div class="grid gap-6 lg:grid-cols-[1.3fr,0.7fr] lg:items-end">
          <div>
            <div
              class="inline-flex rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
            >
              帖子列表按 pinned / featured / createdAt 倒序展示
            </div>
            <h1 class="mt-4 text-4xl font-semibold tracking-tight text-default">
              {{ selectedBoard ? `${selectedBoard.name} 板块` : "全部帖子" }}
            </h1>
            <p class="mt-3 max-w-3xl text-sm leading-7 text-muted">
              当前首页直接对接 `/api/posts` 与
              `/api/boards`。公共读取接口在登录态下会附带
              token，从而拿到准确的点赞和收藏状态。
            </p>

            <div class="mt-6 flex flex-wrap gap-3">
              <UButton
                v-if="isLoggedIn"
                class="rounded-full bg-primary text-white hover:bg-primary/90"
                @click="composeOpen = true"
              >
                发布新帖子
              </UButton>
              <UButton
                v-else
                to="/login?redirect=/"
                color="neutral"
                variant="outline"
                class="rounded-full"
              >
                登录后发帖
              </UButton>
            </div>
          </div>

          <div class="grid gap-3 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
            <div
              class="rounded-3xl border border-default bg-muted/50 px-5 py-4"
            >
              <div class="text-sm text-muted">当前总量</div>
              <div class="mt-2 text-3xl font-semibold text-default">
                {{ formatCount(posts.total) }}
              </div>
            </div>

            <div
              class="rounded-3xl border border-default bg-muted/50 px-5 py-4"
            >
              <div class="text-sm text-muted">本页高亮</div>
              <div class="mt-2 text-3xl font-semibold text-default">
                {{ formatCount(highlightedCount) }}
              </div>
            </div>

            <div
              class="rounded-3xl border border-default bg-muted/50 px-5 py-4"
            >
              <div class="text-sm text-muted">当前分页</div>
              <div class="mt-2 text-3xl font-semibold text-default">
                {{ page }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <UAlert
        v-if="errorMessage"
        color="error"
        variant="soft"
        icon="i-lucide-circle-alert"
        :title="errorMessage"
      />

      <section class="space-y-4">
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

            <div
              class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between"
            >
              <div class="min-w-0 flex-1">
                <NuxtLink
                  :to="`/posts/${post.id}`"
                  class="text-2xl font-semibold leading-9 text-default transition hover:text-primary"
                >
                  {{ post.title }}
                </NuxtLink>

                <p class="mt-3 text-sm leading-7 text-muted">
                  {{
                    truncateText(post.contentText, 220) ||
                    "该帖子仅包含图片内容。"
                  }}
                </p>
              </div>

              <div
                class="rounded-3xl border border-default bg-muted/50 px-4 py-3 text-sm text-muted lg:w-72"
              >
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full border border-default bg-primary text-sm font-semibold text-white"
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
                    <div class="mt-1 text-xs text-muted">
                      等级 Lv.{{ post.author.level }} ·
                      {{ formatDateTime(post.createdAt) }}
                    </div>
                  </div>
                </div>
                <div class="mt-3 grid grid-cols-3 gap-2 text-xs">
                  <div
                    class="rounded-2xl border border-default bg-default px-3 py-2 text-center"
                  >
                    <div class="text-muted">评论</div>
                    <div class="mt-1 font-semibold text-default">
                      {{ formatCount(post.commentCount) }}
                    </div>
                  </div>
                  <div
                    class="rounded-2xl border border-default bg-default px-3 py-2 text-center"
                  >
                    <div class="text-muted">点赞</div>
                    <div class="mt-1 font-semibold text-default">
                      {{ formatCount(post.likeCount) }}
                    </div>
                  </div>
                  <div
                    class="rounded-2xl border border-default bg-default px-3 py-2 text-center"
                  >
                    <div class="text-muted">打赏</div>
                    <div class="mt-1 font-semibold text-default">
                      {{ formatCount(post.rewardCoinCount) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <UButton
                color="neutral"
                variant="outline"
                class="rounded-full"
                @click="handleToggleLike(post)"
              >
                {{ post.liked ? "取消点赞" : "点赞" }} ·
                {{ formatCount(post.likeCount) }}
              </UButton>
              <UButton
                color="neutral"
                variant="outline"
                class="rounded-full"
                @click="handleToggleFavorite(post)"
              >
                {{ post.favorited ? "取消收藏" : "收藏" }}
              </UButton>
              <UButton
                :to="`/posts/${post.id}`"
                class="rounded-full bg-primary text-white hover:bg-primary/90"
              >
                查看详情
              </UButton>
            </div>
          </div>
        </article>

        <div v-if="pending" class="grid gap-4">
          <div
            v-for="item in 3"
            :key="item"
            class="h-48 animate-pulse rounded-[2rem] border border-default bg-muted/50"
          />
        </div>

        <EmptyState
          v-else-if="!posts.list.length"
          title="当前还没有帖子"
          description="可以切换到其他板块看看，或者直接发布第一篇帖子。"
          icon="i-lucide-files"
        />

        <div v-else class="flex justify-end pt-2">
          <UPagination
            :page="page"
            :items-per-page="pageSize"
            :total="posts.total"
            :show-controls="true"
            :show-edges="true"
            @update:page="handlePageChange"
          />
        </div>
      </section>
    </div>

    <PostComposerModal
      v-model:open="composeOpen"
      :boards="boards"
      @created="navigateTo(`/posts/${$event.id}`)"
    />
  </div>
</template>
