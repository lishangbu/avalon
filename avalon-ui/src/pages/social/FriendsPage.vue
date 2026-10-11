<script setup lang="ts">
/** 离开页面停止轮询；确认框保留选中关系编号，列表刷新不能换掉删除目标。 */
import {computed, inject, ref} from 'vue'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useWorld} from '@/features/world/queries'
import {friendsOptions, useFriendActions, type FriendEntry} from '@/features/friends/queries'
import {openPrivateChatKey} from '@/features/chat/entry'

const active = usePageActive(),
  world = useWorld()
const after = ref('0'),
  history = ref<string[]>([]),
  name = ref(''),
  submitted = ref(false)
const selected = ref<FriendEntry>(),
  confirming = ref(false)
const {busy, error, command} = useFriendActions()
// 只有已接受的好友显示快捷入口；申请操作继续使用原有关系接口。
// 战斗时按钮禁用，面板入口仍再次检查门禁，防止界面状态变化时误打开消息。
const openPrivateChat = inject(openPrivateChatKey)
const canChat = computed(() => Boolean(openPrivateChat) && !world.data.value?.character?.activeBattleId)
const query = useQuery(
  computed(() => ({
    ...friendsOptions(after.value),
    enabled: active.value,
    refetchInterval: active.value ? 10_000 : false,
  })),
)
const groups = computed(() =>
  [
    {title: '好友', status: 'ACCEPTED'},
    {title: '收到的申请', status: 'INCOMING'},
    {title: '发出的申请', status: 'OUTGOING'},
  ].map((group) => ({
    ...group,
    entries: query.data.value?.entries.filter((entry) => entry.status === group.status) ?? [],
  })),
)
const profession = (id: string) => world.data.value?.content.professions.find((job) => job.id === id)?.name ?? id

async function request() {
  submitted.value = false
  if (await command('', 'POST', {name: name.value.trim()})) {
    name.value = ''
    submitted.value = true
    after.value = '0'
    history.value = []
  }
}

function confirmRemoval(entry: FriendEntry) {
  selected.value = {...entry}
  confirming.value = true
}

async function remove() {
  if (selected.value && (await command('/' + selected.value.id, 'DELETE'))) confirming.value = false
}

function next() {
  if (query.data.value?.next) {
    history.value.push(after.value)
    after.value = query.data.value.next
  }
}
</script>
<template>
  <AppPage title="好友" back="/more">
    <AppNotice v-if="error || query.error.value" tone="danger">{{ error || query.error.value?.message }}</AppNotice>
    <form @submit.prevent="request">
      <label class="field"
        >完整角色名
        <input v-model="name" maxlength="16" autocomplete="off" :disabled="busy" @input="submitted = false" />
      </label>
      <IonButton type="submit" :disabled="busy || name.trim().length < 2">添加好友</IonButton>
    </form>
    <AppNotice v-if="submitted">好友关系已更新</AppNotice>
    <p v-if="query.isPending.value" role="status">正在加载</p>
    <template v-else>
      <section v-for="group in groups" :key="group.status" :aria-label="group.title">
        <h2>{{ group.title }}</h2>
        <p v-if="!group.entries.length && !query.error.value" class="empty">本页暂无记录</p>
        <article v-for="entry in group.entries" :key="entry.id" class="row" :data-friend-id="entry.id">
          <strong>{{ entry.name }}</strong>
          <p>{{ profession(entry.profession) }}</p>
          <div class="actions">
            <template v-if="entry.status === 'INCOMING'">
              <IonButton :disabled="busy" @click="command('/' + entry.id + '/accept')">接受</IonButton>
              <IonButton :disabled="busy" fill="clear" @click="command('/' + entry.id, 'DELETE')">拒绝</IonButton>
            </template>
            <IonButton
              v-else-if="entry.status === 'OUTGOING'"
              :disabled="busy"
              fill="clear"
              @click="command('/' + entry.id, 'DELETE')"
              >撤回</IonButton
            >
            <template v-else>
              <IonButton :disabled="!canChat" fill="outline" @click="openPrivateChat?.(entry.name)">私聊</IonButton>
              <IonButton :disabled="busy" fill="clear" color="danger" @click="confirmRemoval(entry)"
                >删除好友</IonButton
              >
            </template>
          </div>
        </article>
      </section>
      <div class="actions">
        <IonButton v-if="history.length" :disabled="busy" fill="clear" @click="after = history.pop() ?? '0'"
          >上一页</IonButton
        >
        <IonButton v-if="query.data.value?.next" :disabled="busy" fill="clear" @click="next">下一页</IonButton>
        <IonButton :disabled="query.isFetching.value || busy" fill="clear" @click="query.refetch()">刷新</IonButton>
      </div>
    </template>
    <AppSheet v-model="confirming" title="删除好友" :busy="busy">
      <p>确认与 {{ selected?.name }} 解除好友关系？</p>
      <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
      <template #footer>
        <IonButton :disabled="busy" fill="clear" @click="confirming = false">取消</IonButton>
        <IonButton :disabled="busy" color="danger" @click="remove">确认删除</IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
