<script setup lang="ts">
import type {
  AiChatMessageRole,
  AiChatReply,
  AiChatRequestPayload,
} from "~/types/forum";
import { ApiError } from "~/composables/useApi";

const route = useRoute();
const toast = useToast();

/** 帖子详情页 /posts/:id 时把帖子 ID 传给后端，便于 AI 结合当前帖与评论回答 */
const contextPostId = computed(() => {
  const raw = route.params.id;
  if (raw != null && raw !== "") {
    const n = typeof raw === "string" ? Number(raw) : Number(raw);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  const m = /^\/posts\/(\d+)\/?$/.exec(route.path);
  if (m) {
    const n = Number(m[1]);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }
  return undefined;
});
const auth = useAuth();
const api = useApi();

const open = ref(false);
const input = ref("");
const pending = ref(false);
const scrollRef = ref<HTMLElement | null>(null);
const panelRef = ref<HTMLElement | null>(null);

/** null = 贴在右下角；拖动后改为绝对 left/top */
const panelPosition = ref<{ left: number; top: number } | null>(null);

type PanelDragState = {
  pointerId: number;
  startX: number;
  startY: number;
  originLeft: number;
  originTop: number;
};

let panelDrag: PanelDragState | null = null;

/** 像素尺寸：左上角拖动缩放（右下角为锚点，随鼠标移动连续变化） */
const PANEL_MIN_W = 260;
const PANEL_MIN_H = 280;

const panelWidth = ref(448);
const panelHeight = ref(640);

function panelMaxDimensions() {
  const fs = parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
  /** 与 Tailwind bottom-[calc(1.5rem+3.5rem+0.75rem)] 一致，贴底时顶边需留出空隙 */
  const dockedBottomGap = (1.5 + 3.5 + 0.75) * fs;
  const maxW = Math.max(PANEL_MIN_W, window.innerWidth - 16);
  let maxH = Math.max(PANEL_MIN_H, window.innerHeight - 16);
  if (panelPosition.value == null) {
    maxH = Math.min(
      maxH,
      Math.max(PANEL_MIN_H, window.innerHeight - dockedBottomGap - 8),
    );
  }
  return { w: maxW, h: maxH };
}

function defaultPanelSize() {
  const fs = parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
  const { w: maxW, h: maxH } = panelMaxDimensions();
  const w = Math.min(Math.round(28 * fs), maxW);
  const h = Math.min(Math.round(window.innerHeight * 0.88), 760, maxH);
  return {
    w: Math.max(PANEL_MIN_W, w),
    h: Math.max(PANEL_MIN_H, h),
  };
}

function clampPanelDimensions() {
  const { w: maxW, h: maxH } = panelMaxDimensions();
  panelWidth.value = Math.min(
    Math.max(PANEL_MIN_W, panelWidth.value),
    maxW,
  );
  panelHeight.value = Math.min(
    Math.max(PANEL_MIN_H, panelHeight.value),
    maxH,
  );
}

const panelInlineStyle = computed(() => {
  const style: Record<string, string> = {
    width: `${Math.round(panelWidth.value)}px`,
    height: `${Math.round(panelHeight.value)}px`,
  };
  if (panelPosition.value != null) {
    style.left = `${panelPosition.value.left}px`;
    style.top = `${panelPosition.value.top}px`;
    style.right = "auto";
    style.bottom = "auto";
  }
  return style;
});

function clampPanelToViewport() {
  const panel = panelRef.value;
  if (!panel || panelPosition.value == null) {
    return;
  }
  const r = panel.getBoundingClientRect();
  const inset = 8;
  let nl = panelPosition.value.left;
  let nt = panelPosition.value.top;
  const maxL = Math.max(inset, window.innerWidth - r.width - inset);
  const maxT = Math.max(inset, window.innerHeight - r.height - inset);
  nl = Math.min(Math.max(inset, nl), maxL);
  nt = Math.min(Math.max(inset, nt), maxT);
  panelPosition.value = { left: nl, top: nt };
}

type ResizeDragState = {
  pointerId: number;
  startX: number;
  startY: number;
  startW: number;
  startH: number;
  /** 缩放时固定右下角在视口中的位置 */
  anchorRight: number;
  anchorBottom: number;
  /** 按下时是否仍为贴底定位（未拖标题栏） */
  dockedAtStart: boolean;
};

let resizeDrag: ResizeDragState | null = null;

/** 仅改宽度：拖面板右缘 */
type WidthResizeDragState = {
  pointerId: number;
  startX: number;
  startW: number;
  anchorLeft: number;
  dockedAtStart: boolean;
};

let widthResizeDrag: WidthResizeDragState | null = null;

const PANEL_STATE_STORAGE_KEY = "forum-ai-chat-panel-state";

type PersistedPanelStateV1 = {
  v: 1;
  w: number;
  h: number;
  free: boolean;
  left?: number;
  top?: number;
};

let persistDebounceTimer: ReturnType<typeof setTimeout> | null = null;

function persistPanelState() {
  if (!import.meta.client) {
    return;
  }
  const state: PersistedPanelStateV1 = {
    v: 1,
    w: Math.round(panelWidth.value),
    h: Math.round(panelHeight.value),
    free: panelPosition.value != null,
    left: panelPosition.value?.left,
    top: panelPosition.value?.top,
  };
  try {
    localStorage.setItem(PANEL_STATE_STORAGE_KEY, JSON.stringify(state));
  } catch {
    /* ignore quota */
  }
}

function schedulePersistPanelState() {
  if (!import.meta.client || !open.value) {
    return;
  }
  if (persistDebounceTimer != null) {
    clearTimeout(persistDebounceTimer);
  }
  persistDebounceTimer = setTimeout(() => {
    persistDebounceTimer = null;
    persistPanelState();
  }, 350);
}

function loadPersistedPanel(): boolean {
  if (!import.meta.client) {
    return false;
  }
  try {
    const raw = localStorage.getItem(PANEL_STATE_STORAGE_KEY);
    if (!raw) {
      return false;
    }
    const o = JSON.parse(raw) as PersistedPanelStateV1;
    if (o.v !== 1 || !Number.isFinite(o.w) || !Number.isFinite(o.h)) {
      return false;
    }
    panelWidth.value = o.w;
    panelHeight.value = o.h;
    if (
      o.free &&
      typeof o.left === "number" &&
      typeof o.top === "number" &&
      Number.isFinite(o.left) &&
      Number.isFinite(o.top)
    ) {
      panelPosition.value = { left: o.left, top: o.top };
    } else {
      panelPosition.value = null;
    }
    void nextTick(() => {
      clampPanelDimensions();
      clampPanelToViewport();
    });
    return true;
  } catch {
    return false;
  }
}

function onResizePointerDown(e: PointerEvent) {
  e.stopPropagation();
  e.preventDefault();
  if (e.button !== 0) {
    return;
  }
  const panel = panelRef.value;
  if (!panel) {
    return;
  }
  clampPanelDimensions();
  const r = panel.getBoundingClientRect();
  resizeDrag = {
    pointerId: e.pointerId,
    startX: e.clientX,
    startY: e.clientY,
    startW: r.width,
    startH: r.height,
    anchorRight: r.right,
    anchorBottom: r.bottom,
    dockedAtStart: panelPosition.value == null,
  };
  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
}

function onResizePointerMove(e: PointerEvent) {
  if (!resizeDrag || e.pointerId !== resizeDrag.pointerId) {
    return;
  }
  const { w: maxW, h: maxH } = panelMaxDimensions();
  const dx = e.clientX - resizeDrag.startX;
  const dy = e.clientY - resizeDrag.startY;
  let w = resizeDrag.startW - dx;
  let h = resizeDrag.startH - dy;
  w = Math.min(Math.max(PANEL_MIN_W, w), maxW);
  h = Math.min(Math.max(PANEL_MIN_H, h), maxH);
  panelWidth.value = w;
  panelHeight.value = h;
  if (!resizeDrag.dockedAtStart) {
    panelPosition.value = {
      left: resizeDrag.anchorRight - w,
      top: resizeDrag.anchorBottom - h,
    };
    clampPanelToViewport();
  }
}

function onResizePointerUp(e: PointerEvent) {
  if (!resizeDrag || e.pointerId !== resizeDrag.pointerId) {
    return;
  }
  try {
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
  } catch {
    /* ignore */
  }
  resizeDrag = null;
  clampPanelDimensions();
  clampPanelToViewport();
  schedulePersistPanelState();
}

function onWidthResizePointerDown(e: PointerEvent) {
  e.stopPropagation();
  e.preventDefault();
  if (e.button !== 0) {
    return;
  }
  const panel = panelRef.value;
  if (!panel) {
    return;
  }
  clampPanelDimensions();
  const r = panel.getBoundingClientRect();
  widthResizeDrag = {
    pointerId: e.pointerId,
    startX: e.clientX,
    startW: r.width,
    anchorLeft: r.left,
    dockedAtStart: panelPosition.value == null,
  };
  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
}

function onWidthResizePointerMove(e: PointerEvent) {
  if (!widthResizeDrag || e.pointerId !== widthResizeDrag.pointerId) {
    return;
  }
  const { w: maxW } = panelMaxDimensions();
  const dx = e.clientX - widthResizeDrag.startX;
  let w = widthResizeDrag.startW + dx;
  w = Math.min(Math.max(PANEL_MIN_W, w), maxW);
  panelWidth.value = w;
  if (!widthResizeDrag.dockedAtStart && panelPosition.value != null) {
    panelPosition.value = {
      left: widthResizeDrag.anchorLeft,
      top: panelPosition.value.top,
    };
    clampPanelToViewport();
  }
}

function onWidthResizePointerUp(e: PointerEvent) {
  if (!widthResizeDrag || e.pointerId !== widthResizeDrag.pointerId) {
    return;
  }
  try {
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
  } catch {
    /* ignore */
  }
  widthResizeDrag = null;
  clampPanelDimensions();
  clampPanelToViewport();
  schedulePersistPanelState();
}

function onWindowResize() {
  if (!open.value) {
    return;
  }
  clampPanelDimensions();
  clampPanelToViewport();
}

watch(open, (v) => {
  if (v) {
    if (!loadPersistedPanel()) {
      const d = defaultPanelSize();
      panelWidth.value = d.w;
      panelHeight.value = d.h;
      panelPosition.value = null;
    }
  } else {
    persistPanelState();
    panelDrag = null;
    resizeDrag = null;
    widthResizeDrag = null;
    if (persistDebounceTimer != null) {
      clearTimeout(persistDebounceTimer);
      persistDebounceTimer = null;
    }
  }
});

watch(
  [panelWidth, panelHeight, panelPosition],
  () => {
    if (open.value) {
      schedulePersistPanelState();
    }
  },
  { deep: true },
);

/** 仅 user / assistant，服务端会自动加 system */
const messages = ref<{ role: AiChatMessageRole; content: string }[]>([]);

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  });
}

watch(
  () => open.value,
  (v) => {
    if (v) {
      scrollToBottom();
    }
  },
);

watch(
  messages,
  () => scrollToBottom(),
  { deep: true },
);

function handleFabClick() {
  if (!auth.isLoggedIn.value) {
    toast.add({
      title: "请先登录",
      description: "登录后即可使用论坛 AI 助手",
      color: "warning",
    });
    void navigateTo(`/login?redirect=${encodeURIComponent(route.fullPath)}`);
    return;
  }
  open.value = !open.value;
}

function clearChat() {
  messages.value = [];
  input.value = "";
}

async function sendMessage() {
  const text = input.value.trim();
  if (!text || pending.value) {
    return;
  }
  if (!auth.isLoggedIn.value) {
    handleFabClick();
    return;
  }

  input.value = "";
  messages.value.push({ role: "user", content: text });
  pending.value = true;

  try {
    const payload: AiChatRequestPayload = {
      messages: messages.value.map((m) => ({
        role: m.role,
        content: m.content,
      })),
      includeRecentPostSummaries: true,
    };
    const pid = contextPostId.value;
    if (pid != null) {
      payload.contextPostId = pid;
    }
    const data = await api.request<AiChatReply>("/api/ai/chat", {
      method: "POST",
      body: payload,
    });
    const reply = data?.reply?.trim();
    if (!reply) {
      throw new Error("AI 返回内容为空");
    }
    messages.value.push({ role: "assistant", content: reply });
  } catch (error) {
    messages.value.pop();
    input.value = text;
    let description = "请稍后再试";
    if (error instanceof ApiError) {
      description = error.message || description;
    } else if (error instanceof Error && error.message) {
      description = error.message;
    }
    toast.add({
      title: "发送失败",
      description,
      color: "error",
      duration: 6000,
    });
  } finally {
    pending.value = false;
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    void sendMessage();
  }
}

function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === "Escape" && open.value) {
    open.value = false;
  }
}

onMounted(() => {
  window.addEventListener("keydown", onGlobalKeydown);
  window.addEventListener("resize", onWindowResize);
});
onUnmounted(() => {
  window.removeEventListener("keydown", onGlobalKeydown);
  window.removeEventListener("resize", onWindowResize);
});

function onPanelHeaderPointerDown(e: PointerEvent) {
  if (e.button !== 0) {
    return;
  }
  const panel = panelRef.value;
  if (!panel) {
    return;
  }
  const rect = panel.getBoundingClientRect();
  if (panelPosition.value == null) {
    panelPosition.value = { left: rect.left, top: rect.top };
  }
  panelDrag = {
    pointerId: e.pointerId,
    startX: e.clientX,
    startY: e.clientY,
    originLeft: panelPosition.value.left,
    originTop: panelPosition.value.top,
  };
  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
}

function onPanelHeaderPointerMove(e: PointerEvent) {
  if (!panelDrag || e.pointerId !== panelDrag.pointerId) {
    return;
  }
  const panel = panelRef.value;
  if (!panel) {
    return;
  }
  const dx = e.clientX - panelDrag.startX;
  const dy = e.clientY - panelDrag.startY;
  const r = panel.getBoundingClientRect();
  const inset = 8;
  let nl = panelDrag.originLeft + dx;
  let nt = panelDrag.originTop + dy;
  const maxL = Math.max(inset, window.innerWidth - r.width - inset);
  const maxT = Math.max(inset, window.innerHeight - r.height - inset);
  nl = Math.min(Math.max(inset, nl), maxL);
  nt = Math.min(Math.max(inset, nt), maxT);
  panelPosition.value = { left: nl, top: nt };
}

function onPanelHeaderPointerUp(e: PointerEvent) {
  if (!panelDrag || e.pointerId !== panelDrag.pointerId) {
    return;
  }
  try {
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
  } catch {
    /* ignore */
  }
  panelDrag = null;
  schedulePersistPanelState();
}
</script>

<template>
  <div>
    <!-- 右下角浮层：不盖全屏，便于同时看帖、改帖 -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="translate-y-3 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="translate-y-3 opacity-0"
    >
      <div
        v-if="open"
        ref="panelRef"
        class="fixed z-[60] flex min-h-0 flex-col overflow-hidden rounded-2xl border border-default bg-default shadow-2xl ring-1 ring-black/5 dark:ring-white/10"
        :class="
          panelPosition == null
            ? 'bottom-[calc(1.5rem+3.5rem+0.75rem)] right-3 sm:right-6'
            : ''
        "
        :style="panelInlineStyle"
        role="dialog"
        aria-label="论坛 AI 助手"
      >
        <!-- 左上角缩放：视觉同系统对话框右下角小角；外层略大便于点中 -->
        <div
          class="absolute left-0 top-0 z-40 flex size-5 cursor-nwse-resize touch-none select-none items-start justify-start sm:size-5"
          aria-label="拖动调整窗口大小"
          title="拖动调整大小"
          @pointerdown.stop="onResizePointerDown"
          @pointermove="onResizePointerMove"
          @pointerup="onResizePointerUp"
          @pointercancel="onResizePointerUp"
        >
          <div
            class="pointer-events-none relative size-[10px] overflow-hidden rounded-tl-2xl border-b border-r border-default bg-default sm:size-[11px]"
            aria-hidden="true"
          >
            <!-- 经典对话框角：三条平行斜线（与右下角视觉一致，左上镜像） -->
            <svg
              class="absolute inset-0 size-full text-muted opacity-[0.5] dark:opacity-[0.42]"
              viewBox="0 0 10 10"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M0 10 L10 0"
                stroke="currentColor"
                stroke-width="1.1"
                vector-effect="non-scaling-stroke"
              />
              <path
                d="M0 8 L8 0"
                stroke="currentColor"
                stroke-width="1.1"
                vector-effect="non-scaling-stroke"
              />
              <path
                d="M0 6 L6 0"
                stroke="currentColor"
                stroke-width="1.1"
                vector-effect="non-scaling-stroke"
              />
            </svg>
          </div>
        </div>

        <div
          class="flex shrink-0 cursor-grab touch-none select-none items-start justify-between gap-1.5 border-b border-default bg-elevated/50 py-2.5 pl-[1.35rem] pr-2 active:cursor-grabbing sm:gap-2 sm:pl-6 sm:pr-4"
          @pointerdown="onPanelHeaderPointerDown"
          @pointermove="onPanelHeaderPointerMove"
          @pointerup="onPanelHeaderPointerUp"
          @pointercancel="onPanelHeaderPointerUp"
        >
          <div class="min-w-0 flex-1 pr-1">
            <p class="text-sm font-semibold text-default">论坛 AI 助手</p>
            <p class="mt-0.5 text-[11px] text-muted leading-snug sm:text-xs">
              拖标题栏移动；左上斜拖缩放；右缘左右拖只调宽度；位置与大小会记住。
            </p>
          </div>
          <div class="flex shrink-0" @pointerdown.stop @click.stop>
            <UButton
              color="neutral"
              variant="ghost"
              size="sm"
              square
              class="cursor-pointer rounded-lg sm:rounded-xl"
              aria-label="关闭 AI 助手"
              @click.stop="open = false"
            >
              <UIcon name="i-lucide-x" class="size-4" />
            </UButton>
          </div>
        </div>

        <div class="flex min-h-0 flex-1 flex-col">
          <div
            ref="scrollRef"
            class="min-h-0 flex-1 space-y-3 overflow-y-auto px-3 py-3 sm:px-4"
          >
            <p
              v-if="!messages.length"
              class="rounded-2xl border border-dashed border-default bg-muted/30 px-3 py-5 text-center text-xs leading-relaxed text-muted sm:text-sm"
            >
              我会结合站内最近帖子摘要回答论坛问题；在帖子详情页还会参考当前帖与评论。Enter
              发送，Shift+Enter 换行。
            </p>
            <div
              v-for="(m, i) in messages"
              :key="i"
              class="flex"
              :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
            >
              <div
                class="max-w-[92%] rounded-2xl px-3 py-2 text-xs leading-relaxed sm:text-sm"
                :class="
                  m.role === 'user'
                    ? 'bg-primary text-white'
                    : 'border border-default bg-elevated text-default'
                "
              >
                {{ m.content }}
              </div>
            </div>
            <div v-if="pending" class="flex justify-start">
              <div
                class="rounded-2xl border border-default bg-muted/40 px-3 py-2 text-xs text-muted sm:text-sm"
              >
                正在思考…
              </div>
            </div>
          </div>

          <div
            class="flex shrink-0 items-end gap-2 border-t border-default bg-elevated/80 px-2 py-2.5 sm:px-3"
          >
            <UTextarea
              v-model="input"
              :rows="2"
              class="min-w-0 flex-1"
              placeholder="输入消息…"
              :ui="{ base: 'rounded-xl min-h-[2.75rem] text-sm' }"
              :disabled="pending"
              @keydown="onKeydown"
            />
            <div class="flex shrink-0 flex-col gap-1">
              <UButton
                color="primary"
                class="rounded-lg"
                size="sm"
                :loading="pending"
                :disabled="!input.trim() || pending"
                @click="sendMessage"
              >
                发送
              </UButton>
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                class="rounded-lg"
                @click="clearChat"
              >
                清空
              </UButton>
            </div>
          </div>
        </div>

        <!-- 右缘：仅调整宽度（贴底时固定视口右侧，自由拖动时固定左边） -->
        <div
          class="absolute right-0 top-14 bottom-[7.25rem] z-30 w-1.5 cursor-ew-resize touch-none select-none sm:top-16"
          title="拖动调整宽度"
          aria-label="拖动调整宽度"
          @pointerdown.stop="onWidthResizePointerDown"
          @pointermove="onWidthResizePointerMove"
          @pointerup="onWidthResizePointerUp"
          @pointercancel="onWidthResizePointerUp"
        ></div>
      </div>
    </Transition>

    <button
      type="button"
      class="fixed bottom-6 right-4 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-white shadow-xl shadow-primary/30 transition hover:scale-105 hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 sm:right-6"
      :class="open ? 'ring-2 ring-primary ring-offset-2 ring-offset-default' : ''"
      :aria-label="open ? '关闭 AI 助手' : '打开 AI 助手'"
      :aria-expanded="open"
      @click="handleFabClick"
    >
      <UIcon name="i-lucide-sparkles" class="size-7" />
    </button>
  </div>
</template>
