<script setup lang="ts">
/** 采用页内通知，接受或拒绝均不导航，避免离开战斗或覆盖尚未提交的指令。 */
import {computed, ref} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageClock} from '@/shared/usePageClock'
import {invitationOptions, useInvitationActions} from './invitations'

const props = defineProps<{active: boolean}>()
const active = computed(() => props.active),
  now = usePageClock(active),
  after = ref('0'),
  history = ref<string[]>([])
const query = useQuery(
  computed(() => ({
    ...invitationOptions(after.value),
    enabled: active.value,
    refetchInterval: active.value ? 3000 : false,
  })),
)
const {busy, error, command} = useInvitationActions()
const seconds = (expires: string) => Math.max(0, Math.ceil((Date.parse(expires) - now.value) / 1000))
function next() {
  if (query.data.value?.next) {
    history.value.push(after.value)
    after.value = query.data.value.next
  }
}
</script>
<template>
  <section
    v-if="
      query.data.value?.invitations.length || query.data.value?.next || history.length || error || query.error.value
    "
    aria-label="副本邀请"
  >
    <h2>副本邀请</h2>
    <AppNotice v-if="error || query.error.value" tone="danger">{{ error || query.error.value?.message }}</AppNotice>
    <article v-for="invitation in query.data.value?.invitations" :key="invitation.id" class="row">
      <strong>{{ invitation.sender }}邀请你加入{{ invitation.dungeon }}</strong>
      <p>
        {{ invitation.members }}/{{ invitation.capacity }} 人 ·
        {{ seconds(invitation.expiresAt) ? `剩余 ${seconds(invitation.expiresAt)} 秒` : '邀请已过期' }}
      </p>
      <div class="actions">
        <IonButton
          :disabled="busy || seconds(invitation.expiresAt) === 0"
          @click="command('/' + invitation.id + '/accept')"
          >接受邀请</IonButton
        >
        <IonButton :disabled="busy" fill="clear" @click="command('/' + invitation.id + '/decline')">拒绝邀请</IonButton>
      </div>
    </article>
    <div class="actions">
      <IonButton v-if="history.length" fill="clear" @click="after = history.pop() ?? '0'">上一页邀请</IonButton>
      <IonButton v-if="query.data.value?.next" fill="clear" @click="next">下一页邀请</IonButton>
      <IonButton v-if="query.error.value" fill="clear" @click="query.refetch()">重试邀请列表</IonButton>
    </div>
  </section>
</template>
