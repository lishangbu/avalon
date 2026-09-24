<script setup lang="ts">
/** 每位好友保留自己的请求号；超时重试沿用编号，发送成功才允许产生下一次邀请。 */
import {computed, ref} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {friendsOptions} from '@/features/friends/queries'
import {useInvitationActions} from './invitations'

const props = defineProps<{lobby: string; active: boolean}>()
const after = ref('0'),
  history = ref<string[]>([]),
  sent = ref<string[]>([])
const query = useQuery(
  computed(() => ({
    ...friendsOptions(after.value),
    enabled: props.active,
    refetchInterval: props.active ? 10000 : false,
  })),
)
const friends = computed(() => query.data.value?.entries.filter((entry) => entry.status === 'ACCEPTED') ?? [])
const {busy, error, command} = useInvitationActions()
const requests = new Map<string, string>()
async function invite(character: string) {
  const requestId = requests.get(character) ?? crypto.randomUUID()
  requests.set(character, requestId)
  if (await command('', {lobby: props.lobby, recipient: character, requestId})) {
    requests.delete(character)
    sent.value.push(character)
  }
}
function next() {
  if (query.data.value?.next) {
    history.value.push(after.value)
    after.value = query.data.value.next
  }
}
</script>
<template>
  <section aria-label="邀请好友">
    <h2>邀请好友</h2>
    <AppNotice v-if="error || query.error.value" tone="danger">{{ error || query.error.value?.message }}</AppNotice>
    <p v-if="query.isPending.value">正在加载好友</p>
    <p v-else-if="!friends.length && !query.error.value">当前页没有可邀请的好友</p>
    <article v-for="friend in friends" :key="friend.id" class="row">
      <strong>{{ friend.name }}</strong>
      <p v-if="sent.includes(friend.character)" role="status">已发送邀请</p>
      <IonButton :disabled="busy" fill="outline" :aria-label="`邀请${friend.name}`" @click="invite(friend.character)"
        >邀请入队</IonButton
      >
    </article>
    <div class="actions">
      <IonButton v-if="history.length" fill="clear" @click="after = history.pop() ?? '0'">上一页好友</IonButton>
      <IonButton v-if="query.data.value?.next" fill="clear" @click="next">下一页好友</IonButton>
      <IonButton v-if="query.error.value" fill="clear" @click="query.refetch()">重试好友列表</IonButton>
    </div>
  </section>
</template>
