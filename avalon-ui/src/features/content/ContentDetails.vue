<script setup lang="ts">
/** 详情只读并使用相同查询键；列表点击不执行购买、学习或领取命令。 */
import {computed} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import AppNotice from '@/shared/ui/AppNotice.vue'

const props = defineProps<{kind: 'item' | 'skill' | 'ability' | 'creature'; code: string}>()
const detail = useQuery({
  queryKey: computed(() => ['content-detail', props.kind, props.code]),
  queryFn: () =>
    api<{name: string; description: string; rules: string[]}>(
      `/api/content/details/${props.kind}/${encodeURIComponent(props.code)}`,
    ),
  staleTime: computed(() => (props.kind === 'creature' ? 0 : Infinity)),
})
</script>
<template>
  <AppNotice v-if="detail.error.value" tone="danger">{{ detail.error.value.message }}</AppNotice>
  <p v-else-if="detail.isPending.value">正在读取说明…</p>
  <section v-else-if="detail.data.value" aria-label="详细说明">
    <p>{{ detail.data.value.description }}</p>
    <ul v-if="detail.data.value.rules.length">
      <li v-for="rule in detail.data.value.rules" :key="rule">{{ rule }}</li>
    </ul>
  </section>
</template>
<style scoped>
p,
li {
  line-height: 1.7;
  overflow-wrap: anywhere;
}
ul {
  padding-left: 1.2em;
}
</style>
