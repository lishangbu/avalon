<script setup lang="ts">
/** 行内展开共享详情，返回上层不会叠加弹窗或丢失原有滚动位置。 */
import {ref} from 'vue'
import ContentDetails from './ContentDetails.vue'
defineProps<{kind: 'item' | 'skill' | 'ability'; code: string; name?: string}>()
const open = ref(false)
</script>
<template>
  <details @toggle="open = ($event.target as HTMLDetailsElement).open">
    <summary>
      <strong>{{ name ?? code }}</strong>
    </summary>
    <ContentDetails v-if="open" :kind="kind" :code="code" />
  </details>
</template>
<style scoped>
summary {
  cursor: pointer;
  padding-block: 10px;
  min-height: 44px;
  font-weight: 600;
}
</style>
