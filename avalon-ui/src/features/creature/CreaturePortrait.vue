<script lang="ts" setup>
/** 只展示后端图鉴提供的图片；尚未配图的物种不占用空白位置。 */
defineProps<{source?: string | null; name: string; large?: boolean}>()
</script>

<template>
  <img v-if="source" :src="source" :alt="name" :class="{large}" class="portrait" width="50" height="50" />
</template>

<style scoped>
.portrait {
  flex: none;
  object-fit: contain;
  image-rendering: pixelated;
}

.large {
  display: block;
  width: 100px;
  height: 100px;
  margin: 16px auto;
}
</style>
