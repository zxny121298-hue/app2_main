<script setup lang="ts">
const file = ref<File | null>(null);
const uploadedUrl = ref("");
const errorMsg = ref("");

function onSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  file.value = target.files?.[0] ?? null;
}

async function uploadImage() {
  if (!file.value) {
    errorMsg.value = "请先选择图片";
    return;
  }

  errorMsg.value = "";

  const formData = new FormData();
  formData.append("image", file.value);

  try {
    const res = await $fetch<{ ok: boolean; url?: string }>("/api/upload", {
      method: "POST",
      body: formData,
    });

    uploadedUrl.value = res.url || "";
  } catch {
    errorMsg.value = "上传失败";
  }
}
</script>

<template>
  <div>
    <input type="file" accept="image/*" @change="onSelect" />
    <button @click="uploadImage">上传</button>

    <p v-if="errorMsg">{{ errorMsg }}</p>
    <img
      v-if="uploadedUrl"
      :src="uploadedUrl"
      alt="uploaded"
      style="max-width: 300px"
    />
  </div>
</template>
