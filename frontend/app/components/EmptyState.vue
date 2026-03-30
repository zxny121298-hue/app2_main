<script setup lang="ts">
defineProps<{
  title: string
  description: string
  icon?: string
}>()
</script>

<template>
  <div class="rounded-3xl border border-dashed border-default bg-elevated/70 px-6 py-12 text-center">
    <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted">
      <UIcon :name="icon || 'i-lucide-inbox'" class="size-7" />
    </div>
    <h3 class="text-lg font-semibold text-default">
      {{ title }}
    </h3>
    <p class="mx-auto mt-2 max-w-xl text-sm leading-6 text-muted">
      {{ description }}
    </p>
  </div>
</template>
