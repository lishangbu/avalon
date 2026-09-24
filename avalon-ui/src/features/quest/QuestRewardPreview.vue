<script setup lang="ts">
import {computed, ref} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import ContentLink from '@/features/content/ContentLink.vue'
const props = defineProps<{step: string}>()
const open = ref(false)
const rewards = useQuery({
  queryKey: computed(() => ['quests', 'rewards', props.step]),
  queryFn: () =>
    api<{code: string; name: string; quantity: number}[]>(`/api/quests/${encodeURIComponent(props.step)}/rewards`),
  enabled: open,
})
</script>
<template>
  <details @toggle="open = ($event.target as HTMLDetailsElement).open">
    <summary>查看物品奖励</summary>
    <p v-if="rewards.error.value" role="alert">{{ rewards.error.value.message }}</p>
    <p v-else-if="rewards.isPending.value">正在读取奖励…</p>
    <template v-else>
      <ContentLink
        v-for="item in rewards.data.value"
        :key="item.code"
        kind="item"
        :code="item.code"
        :name="`${item.name} ×${item.quantity}`"
      />
      <p v-if="!rewards.data.value?.length">此步骤没有物品奖励。</p>
    </template>
  </details>
</template>
