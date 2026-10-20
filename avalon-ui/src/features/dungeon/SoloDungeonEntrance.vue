<script setup lang="ts">
import {watch, ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import {useRouter} from 'vue-router'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppNotice from '@/shared/ui/AppNotice.vue'

const props = defineProps<{active: boolean; location: string}>()
const emit = defineEmits<{busy: [value: boolean]}>()
const router = useRouter()
const client = useQueryClient()
const action = useAction(true)
type Entrance = {id: string; name: string; level: number; unlocked: boolean; remainingStarts: number}
const entrances = useQuery({
  queryKey: ['dungeon', 'solo', () => props.location],
  queryFn: () => api<Entrance[]>('/api/dungeon/runs/solo'),
  enabled: () => props.active,
})
const pending = ref<{dungeon: string; requestId: string}>()
watch(action.busy, (value) => emit('busy', value))

async function enter(dungeon: string) {
  if (!pending.value || pending.value.dungeon !== dungeon) pending.value = {dungeon, requestId: crypto.randomUUID()}
  let id: string | undefined
  const success = await action.run(async () => {
    const result = await api<{id: string}>(`/api/dungeon/runs/solo/${dungeon}`, 'POST', {
      requestId: pending.value!.requestId,
    })
    id = result.id
  })
  await client.invalidateQueries({queryKey: ['dungeon']})
  await client.invalidateQueries({queryKey: ['world']})
  if (success && id) {
    pending.value = undefined
    await router.push(`/dungeon/${id}`)
  }
}
</script>

<template>
  <AppNotice v-if="props.active && entrances.isError.value" tone="danger">
    无法读取单人副本入口。
    <IonButton size="small" :disabled="entrances.isFetching.value" @click="entrances.refetch()">重试</IonButton>
  </AppNotice>
  <section v-if="entrances.data.value?.length" aria-label="单人副本入口">
    <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
    <div v-for="entrance in entrances.data.value" :key="entrance.id">
      <h2>{{ entrance.name }}</h2>
      <p>单人 · {{ entrance.level }} 级 · 今日剩余发起 {{ entrance.remainingStarts }} 次</p>
      <p v-if="!entrance.unlocked">达到所需等级并完成入口前置任务后开放。</p>
      <IonButton
        :disabled="action.busy.value || !entrance.unlocked || entrance.remainingStarts === 0"
        @click="enter(entrance.id)"
        >进入副本</IonButton
      >
    </div>
  </section>
</template>
