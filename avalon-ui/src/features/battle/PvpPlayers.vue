<script setup lang="ts">
import {computed, nextTick, ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppNotice from '@/shared/ui/AppNotice.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import {battleKey} from './queries'
import type {PvpView} from './types'

const props = defineProps<{active: boolean; location: string; allowed: boolean}>()
const client = useQueryClient(),
  router = useRouter(),
  action = useAction(true)
const selected = ref<{id: string; name: string}>()
const confirm = ref(false)
const players = useQuery({
  queryKey: computed(() => ['pvp-players', props.location]),
  enabled: computed(() => props.active && props.allowed),
  queryFn: () => api<{id: string; name: string}[]>('/api/battle/pvp/players'),
  refetchInterval: 5000,
})
const protection = useQuery({
  queryKey: ['pvp-protection'],
  enabled: computed(() => props.active),
  queryFn: async () => (await api<{expiresAt: string | null}>('/api/battle/pvp/protection')).expiresAt,
  refetchInterval: 5000,
})
let startRequest: {opponentCharacter: string; requestId: string} | undefined
let protectionRequest: {requestId: string} | undefined
function select(player: {id: string; name: string}) {
  selected.value = player
  confirm.value = true
}
async function attack() {
  if (!selected.value) return
  let started: PvpView | undefined
  await action.run(async () => {
    if (!startRequest || startRequest.opponentCharacter !== selected.value!.id)
      startRequest = {opponentCharacter: selected.value!.id, requestId: crypto.randomUUID()}
    started = await api<PvpView>('/api/battle/pvp', 'POST', startRequest)
    startRequest = undefined
  })
  if (!started) return
  // 忙碌时弹窗禁止关闭，先解除提交保护再关闭，避免路由已跳转而弹窗仍遮住战场。
  await nextTick()
  confirm.value = false
  await nextTick()
  client.setQueryData(battleKey, started.battle)
  await client.invalidateQueries()
  await router.push('/battle')
}
async function protect() {
  await action.run(async () => {
    protectionRequest ??= {requestId: crypto.randomUUID()}
    await api('/api/battle/pvp/protection', 'POST', protectionRequest)
    protectionRequest = undefined
    await client.invalidateQueries()
  })
}
</script>
<template>
  <section aria-label="免战与附近训练师">
    <AppNotice v-if="action.error.value || players.error.value || protection.error.value" tone="danger">
      {{ action.error.value || players.error.value?.message || protection.error.value?.message }}
    </AppNotice>
    <p v-if="protection.data.value">免战保护至 {{ new Date(protection.data.value).toLocaleTimeString() }}</p>
    <IonButton :disabled="action.busy.value || !!protection.data.value" fill="outline" @click="protect">
      使用免战牌（30分钟）
    </IonButton>
    <template v-if="allowed">
      <p>此区域允许玩家对战。免战牌可保护你与正在采集的伙伴。</p>
      <div v-for="player in players.data.value" :key="player.id" class="row">
        <span>{{ player.name }}</span>
        <IonButton :disabled="action.busy.value" fill="outline" @click="select(player)">攻击</IonButton>
      </div>
    </template>
    <AppSheet v-model="confirm" title="确认攻击" :busy="action.busy.value">
      <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
      <p>攻击 {{ selected?.name }} 将进入训练师对战，并解除自己的免战保护。</p>
      <template #footer><IonButton :disabled="action.busy.value" @click="attack">确认攻击</IonButton></template>
    </AppSheet>
  </section>
</template>
