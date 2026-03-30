<script setup lang="ts">
import type { SearchHistoryView, SearchType } from "~/types/forum";
import { ApiError } from "~/composables/useApi";
import { truncateText } from "~/utils/forum";

const route = useRoute();
const toast = useToast();
const auth = useAuth();
const api = useApi();
const { headerSearchExpanded, openHeaderSearch, closeHeaderSearch } =
  useHeaderSearchExpand();

const isLoggedIn = computed(() => auth.isLoggedIn.value);

const q = ref("");
const inputRef = ref<HTMLInputElement | null>(null);
const historyItems = ref<SearchHistoryView[]>([]);
const historyLoading = ref(false);
const historyShowAll = ref(false);
const clearPending = ref(false);

/** 演示热搜（无后端接口时可替换为真实数据） */
const HOT_LIST = [
  { rank: 1, text: "校园活动报名", badge: "hot" as const },
  { rank: 2, text: "二手闲置转让", badge: "new" as const },
  { rank: 3, text: "期末复习资料汇总" },
  { rank: 4, text: "食堂新品测评", badge: "hot" as const },
  { rank: 5, text: "失物招领", badge: "book" as const },
  { rank: 6, text: "社团招新" },
  { rank: 7, text: "选课经验分享", badge: "new" as const },
  { rank: 8, text: "租房信息" },
  { rank: 9, text: "校园网问题" },
  { rank: 10, text: "讲座预告" },
];

const hotLeft = computed(() => HOT_LIST.filter((x) => x.rank <= 5));
const hotRight = computed(() => HOT_LIST.filter((x) => x.rank > 5));

const historyVisible = computed(() => historyItems.value.length > 0);
const historyTagsSlice = computed(() => {
  const list = historyItems.value;
  if (historyShowAll.value) {
    return list;
  }
  return list.slice(0, 8);
});
const historyHasMore = computed(
  () => !historyShowAll.value && historyItems.value.length > 8,
);

async function loadHistory() {
  if (!isLoggedIn.value) {
    historyItems.value = [];
    return;
  }
  historyLoading.value = true;
  try {
    historyItems.value = await api.request<SearchHistoryView[]>(
      "/api/search/history",
      { query: { limit: 20 } },
    );
  } catch {
    historyItems.value = [];
  } finally {
    historyLoading.value = false;
  }
}

async function clearHistory() {
  if (!isLoggedIn.value || clearPending.value) {
    return;
  }
  clearPending.value = true;
  try {
    await api.request("/api/search/history", { method: "DELETE" });
    historyItems.value = [];
    historyShowAll.value = false;
  } catch (error) {
    const msg =
      error instanceof ApiError ? error.message : "清空失败，请稍后重试";
    toast.add({ title: "清空失败", description: msg, color: "error" });
  } finally {
    clearPending.value = false;
  }
}

function applyHistory(item: SearchHistoryView) {
  const type = item.searchType as SearchType;
  const keyword = item.keyword.trim();
  closeHeaderSearch();
  void navigateTo({
    path: "/search",
    query: { type, q: keyword },
  });
}

async function submitSearch() {
  const keyword = q.value.trim();
  closeHeaderSearch();
  await navigateTo({
    path: "/search",
    query: {
      type: "content",
      ...(keyword ? { q: keyword } : {}),
    },
  });
}

function applyHot(text: string) {
  q.value = text;
  void submitSearch();
}

function expandAndFocus() {
  openHeaderSearch();
  historyShowAll.value = false;
  void loadHistory();
  void nextTick(() => {
    inputRef.value?.focus();
  });
}

function onBackdropClick() {
  closeHeaderSearch();
}

function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === "Escape" && headerSearchExpanded.value) {
    closeHeaderSearch();
  }
}

watch(headerSearchExpanded, (v) => {
  if (v) {
    document.body.style.overflow = "hidden";
    void loadHistory();
    void nextTick(() => inputRef.value?.focus());
    window.addEventListener("keydown", onGlobalKeydown);
  } else {
    document.body.style.overflow = "";
    window.removeEventListener("keydown", onGlobalKeydown);
    q.value = "";
    historyShowAll.value = false;
  }
});

watch(
  () => route.fullPath,
  () => {
    closeHeaderSearch();
  },
);

onUnmounted(() => {
  document.body.style.overflow = "";
  window.removeEventListener("keydown", onGlobalKeydown);
});

function badgeClass(badge: string | undefined) {
  if (badge === "hot") {
    return "bg-rose-500 text-white";
  }
  if (badge === "new") {
    return "bg-amber-400 text-neutral-900";
  }
  if (badge === "book") {
    return "bg-amber-800/80 text-amber-50";
  }
  return "";
}
</script>

<template>
  <div class="shrink-0">
    <!-- 收起：浅灰圆角条 + 右侧放大镜（图一） -->
    <button
      v-if="!headerSearchExpanded"
      type="button"
      class="flex h-10 w-full min-w-[10rem] max-w-[16rem] items-center gap-2 rounded-[10px] bg-[#f1f2f4] py-0 pl-3.5 pr-2.5 text-left transition hover:bg-[#e8eaed] sm:min-w-[12.5rem] sm:max-w-[17rem] dark:bg-neutral-800/90 dark:hover:bg-neutral-800"
      aria-label="打开搜索"
      @click="expandAndFocus"
    >
      <span
        class="min-w-0 flex-1 truncate text-[13px] text-[#8590a6] dark:text-neutral-400"
      >
        搜索你感兴趣的帖子
      </span>
      <UIcon
        name="i-lucide-search"
        class="size-[18px] shrink-0 text-neutral-900 dark:text-neutral-100"
      />
    </button>

    <Teleport to="body">
      <Transition name="hdr-search-fade">
        <div
          v-if="headerSearchExpanded"
          class="fixed inset-0 z-[200]"
          role="dialog"
          aria-label="搜索"
        >
          <div
            class="absolute inset-0 bg-black/30 backdrop-blur-[2px] dark:bg-black/50"
            aria-hidden="true"
            @click="onBackdropClick"
          />

          <div
            class="relative mx-auto flex max-h-[min(90vh,720px)] w-full max-w-xl flex-col px-3 pt-[max(0.75rem,env(safe-area-inset-top))] sm:px-4 sm:pt-4"
          >
            <Transition name="hdr-search-pop" appear>
              <div class="flex flex-col gap-2">
                <!-- 图二：药丸输入 + 粉边 -->
                <form
                  class="flex items-center gap-2 rounded-full border-2 border-[#fb7299] bg-white py-2 pl-4 pr-3 shadow-sm dark:border-rose-400 dark:bg-neutral-950"
                  @submit.prevent="submitSearch"
                >
                  <input
                    ref="inputRef"
                    v-model="q"
                    type="search"
                    enterkeyhint="search"
                    autocomplete="off"
                    placeholder="搜索你感兴趣的帖子"
                    class="min-w-0 flex-1 border-0 bg-transparent text-sm text-default placeholder:text-neutral-400 focus:outline-none focus:ring-0 dark:placeholder:text-neutral-500"
                  />
                  <button
                    type="submit"
                    class="flex shrink-0 rounded-full p-1 text-default transition hover:bg-neutral-100 dark:hover:bg-neutral-800"
                    aria-label="搜索"
                  >
                    <UIcon name="i-lucide-search" class="size-5" />
                  </button>
                </form>

                <!-- 下拉：历史 + 热搜 -->
                <div
                  class="overflow-hidden rounded-xl border border-neutral-200/90 bg-white shadow-xl dark:border-neutral-700 dark:bg-neutral-900"
                >
                  <!-- 搜索历史 -->
                  <div
                    v-if="isLoggedIn"
                    class="border-b border-neutral-100 px-3 py-2.5 dark:border-neutral-800 sm:px-4"
                  >
                    <div class="mb-2 flex items-center justify-between gap-2">
                      <span class="text-sm font-medium text-default"
                        >搜索历史</span
                      >
                      <UButton
                        variant="link"
                        size="xs"
                        color="neutral"
                        class="p-0 text-xs text-neutral-500"
                        :disabled="!historyVisible || clearPending"
                        :loading="clearPending"
                        @click="clearHistory"
                      >
                        清空
                      </UButton>
                    </div>
                    <div
                      v-if="historyLoading"
                      class="py-4 text-center text-xs text-muted"
                    >
                      加载中…
                    </div>
                    <div
                      v-else-if="!historyVisible"
                      class="py-3 text-center text-xs text-muted"
                    >
                      暂无搜索历史
                    </div>
                    <div v-else class="flex flex-wrap gap-2">
                      <button
                        v-for="item in historyTagsSlice"
                        :key="item.id"
                        type="button"
                        class="max-w-full rounded-md bg-[#f4f4f5] px-2.5 py-1 text-left text-xs text-default transition hover:bg-[#e4e4e7] dark:bg-neutral-800 dark:hover:bg-neutral-700"
                        @click="applyHistory(item)"
                      >
                        {{ truncateText(item.keyword, 18) }}
                      </button>
                    </div>
                    <button
                      v-if="historyHasMore"
                      type="button"
                      class="mt-2 flex w-full items-center justify-center gap-0.5 text-xs text-neutral-500 transition hover:text-primary"
                      @click="historyShowAll = true"
                    >
                      展开更多
                      <UIcon name="i-lucide-chevron-down" class="size-3.5" />
                    </button>
                  </div>
                  <div
                    v-else
                    class="border-b border-neutral-100 px-3 py-2 text-xs text-muted dark:border-neutral-800 sm:px-4"
                  >
                    登录后可同步搜索历史
                  </div>

                  <!-- 热搜 -->
                  <div class="px-3 py-3 sm:px-4">
                    <div class="mb-2 text-sm font-semibold text-default">
                      热搜
                    </div>
                    <div class="grid grid-cols-2 gap-x-6 gap-y-1.5 text-sm">
                      <div class="space-y-1.5">
                        <button
                          v-for="row in hotLeft"
                          :key="row.rank"
                          type="button"
                          class="flex w-full min-w-0 items-center gap-2 text-left transition hover:text-primary"
                          @click="applyHot(row.text)"
                        >
                          <span class="w-4 shrink-0 text-xs text-neutral-400">{{
                            row.rank
                          }}</span>
                          <span class="min-w-0 flex-1 truncate text-default">{{
                            row.text
                          }}</span>
                          <span
                            v-if="row.badge"
                            class="shrink-0 rounded px-0.5 text-[10px] font-medium leading-tight"
                            :class="badgeClass(row.badge)"
                          >
                            {{
                              row.badge === "hot"
                                ? "热"
                                : row.badge === "new"
                                  ? "新"
                                  : "荐"
                            }}
                          </span>
                        </button>
                      </div>
                      <div class="space-y-1.5">
                        <button
                          v-for="row in hotRight"
                          :key="row.rank"
                          type="button"
                          class="flex w-full min-w-0 items-center gap-2 text-left transition hover:text-primary"
                          @click="applyHot(row.text)"
                        >
                          <span class="w-4 shrink-0 text-xs text-neutral-400">{{
                            row.rank
                          }}</span>
                          <span class="min-w-0 flex-1 truncate text-default">{{
                            row.text
                          }}</span>
                          <span
                            v-if="row.badge"
                            class="shrink-0 rounded px-0.5 text-[10px] font-medium leading-tight"
                            :class="badgeClass(row.badge)"
                          >
                            {{
                              row.badge === "hot"
                                ? "热"
                                : row.badge === "new"
                                  ? "新"
                                  : "荐"
                            }}
                          </span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="flex justify-center pb-2">
                  <UButton
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    class="rounded-full text-neutral-600 dark:text-neutral-300"
                    @click="closeHeaderSearch"
                  >
                    取消
                  </UButton>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.hdr-search-fade-enter-active,
.hdr-search-fade-leave-active {
  transition: opacity 0.25s ease;
}
.hdr-search-fade-enter-from,
.hdr-search-fade-leave-to {
  opacity: 0;
}

.hdr-search-pop-enter-active {
  transition:
    opacity 0.28s ease,
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}
.hdr-search-pop-enter-from {
  opacity: 0;
  transform: scale(0.94) translateY(-10px);
}
</style>
