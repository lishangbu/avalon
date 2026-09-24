<script setup lang="ts">
/** 通知与战斗共存，不导航、不自动确认，也不改动当前回合草稿。 */
import {computed} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import {api} from '@/shared/http'
import {usePageClock} from '@/shared/usePageClock'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {useLobbyActions} from './lobby'
import DungeonInvitations from './DungeonInvitations.vue'

interface Notice {
  id: string | null
  confirmed: boolean
  deadline: string | null
  inBattle: boolean
  members: number
}
const props = defineProps<{active: boolean}>()
const active = computed(() => props.active),
  now = usePageClock(active)
const query = useQuery({
  queryKey: ['dungeon', 'notice'],
  queryFn: () => api<Notice>('/api/dungeon/lobbies/notice'),
  enabled: active,
  refetchInterval: computed(() => (active.value ? 3000 : false)),
  retry: false,
})
const notice = query.data
const remaining = computed(() =>
  notice.value?.deadline ? Math.max(0, Math.ceil((Date.parse(notice.value.deadline) - now.value) / 1000)) : 0,
)
const {busy, error, command} = useLobbyActions()
</script>
<template>
  <DungeonInvitations :active="active" />
  <section v-if="notice?.id" aria-label="组队通知" class="lobby-notice">
    <p v-if="!notice.deadline">等待队友 · {{ notice.members }}/3 人</p>
    <p v-else-if="remaining === 0">组队位置已到期</p>
    <p v-else>{{ notice.confirmed ? '已确认入队' : '队伍已就绪，请确认入队' }} · 剩余 {{ remaining }} 秒</p>
    <p v-if="notice.confirmed && notice.inBattle && remaining > 0">结束当前战斗后可前往准备。</p>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <AppNotice v-if="query.error.value" tone="danger">{{ query.error.value.message }}</AppNotice>
    <div class="actions">
      <IonButton
        v-if="notice.deadline && !notice.confirmed && remaining > 0"
        :disabled="busy"
        @click="command('/' + notice.id + '/confirm')"
        >确认入队</IonButton
      >
      <IonButton :disabled="busy" fill="clear" @click="command('/' + notice.id + '/leave')">{{
        notice.deadline && !notice.confirmed ? '拒绝入队' : '离开队伍'
      }}</IonButton>
      <IonButton v-if="!notice.inBattle" fill="outline" router-link="/dungeon-lobby">组队准备</IonButton>
    </div>
  </section>
</template>
<style scoped>
.lobby-notice {
  border-bottom: 1px solid var(--game-border);
  padding-bottom: 12px;
  margin-bottom: 16px;
}
</style>
