<script lang="ts" setup>
/** 招募轮询只读取状态，确认、准备和开本都必须由玩家主动操作。 */
import {computed, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {usePageClock} from '@/shared/usePageClock'
import {useWorld} from '@/features/world/queries'
import {lobbyCurrentOptions, recruitmentOptions, replacementOptions, useLobbyActions} from '@/features/dungeon/lobby'
import {dungeonCurrentOptions} from '@/features/dungeon/queries'
import DungeonInviteFriends from '@/features/dungeon/DungeonInviteFriends.vue'
import DungeonInvitations from '@/features/dungeon/DungeonInvitations.vue'

const active = usePageActive(),
  now = usePageClock(active),
  router = useRouter()
const world = useWorld()
const current = useQuery({
  ...lobbyCurrentOptions(),
  enabled: active,
  refetchInterval: computed(() => (active.value ? 2000 : false)),
})
const progress = useQuery({
  ...dungeonCurrentOptions(),
  enabled: active,
  refetchInterval: computed(() => (active.value ? 2000 : false)),
})
const lobby = computed(() => current.data.value?.lobby),
  self = computed(() => lobby.value?.members.find((member) => member.self))
const after = ref('0'),
  history = ref<string[]>([])
const offers = useQuery(
  computed(() => ({
    ...recruitmentOptions(after.value),
    enabled: active.value && (!lobby.value || lobby.value.closed),
    refetchInterval: active.value && (!lobby.value || lobby.value.closed) ? 3000 : false,
  })),
)
const {busy, error, command} = useLobbyActions()
const replacementAfter = ref('0'),
  replacementHistory = ref<string[]>([])
const replacements = useQuery(
  computed(() => ({
    ...replacementOptions(replacementAfter.value),
    enabled: active.value && (!lobby.value || lobby.value.closed),
    refetchInterval: active.value && (!lobby.value || lobby.value.closed) ? 3000 : false,
  })),
)
// 每个目标保留独立请求号，网络结果不明时重试不会创建第二个确认房间。
const applications = new Map<string, string>()
async function apply(run: string) {
  const requestId = applications.get(run) ?? crypto.randomUUID()
  applications.set(run, requestId)
  if (await command('/replacements/' + run, {requestId})) applications.delete(run)
}
let requestId = crypto.randomUUID()
const profession = (id: string) => world.data.value?.content.professions.find((job) => job.id === id)?.name ?? id
const seconds = (deadline: string) => Math.max(0, Math.ceil((Date.parse(deadline) - now.value) / 1000))
watch(
  () => progress.data.value?.run,
  (run) => {
    if (active.value && run) void router.replace('/dungeon/' + run.id)
  },
  {immediate: true},
)

async function create() {
  if (await command('', {requestId})) requestId = crypto.randomUUID()
}

function next() {
  if (offers.data.value?.next) {
    history.value.push(after.value)
    after.value = offers.data.value.next
  }
}

function nextReplacement() {
  if (replacements.data.value?.next) {
    replacementHistory.value.push(replacementAfter.value)
    replacementAfter.value = replacements.data.value.next
  }
}
</script>
<template>
  <AppPage title="副本招募">
    <AppNotice v-if="error || current.error.value || progress.error.value || offers.error.value" tone="danger">
      {{ error || current.error.value?.message || progress.error.value?.message || offers.error.value?.message }}
    </AppNotice>
    <AppNotice v-if="replacements.error.value" tone="danger">{{ replacements.error.value.message }}</AppNotice>
    <p>今日剩余发起 {{ progress.data.value?.remainingStarts ?? '-' }} 次</p>
    <AppNotice>入本前请全员从诺姆城外围沿地图道路前往林间兽栏入口。补员也需先抵达入口。</AppNotice>
    <DungeonInvitations :active="active" />
    <template v-if="lobby && !lobby.closed"
      ><h1>{{ lobby.name }}</h1>
      <p v-if="lobby.targetRun">补员确认</p>
      <p v-else>{{ lobby.members.length }} / 3 人</p>
      <AppNotice v-if="lobby.targetRun"
        >加入已有进度，不扣发起次数，已完成遭遇的奖励不补发。准备期间不保留空位。</AppNotice
      >
      <article v-for="member in lobby.members" :key="member.id" class="row">
        <strong
          >{{ member.name }}{{ member.self ? '（你）' : ''
          }}{{ !lobby.targetRun && member.id === lobby.leader ? ' · 队长' : '' }}</strong
        >
        <p>
          {{ profession(member.profession) }} · Lv.{{ member.level }} ·
          {{ member.inBattle ? '战斗中' : member.ready ? '已准备' : member.confirmed ? '已确认' : '待确认'
          }}<span v-if="member.deadline"> · 剩余 {{ seconds(member.deadline) }} 秒</span>
        </p>
        <details>
          <summary>编队与补给</summary>
          <p v-for="unit in member.party" :key="unit.id">
            {{ unit.name }} · Lv.{{ unit.level }} · 生命 {{ unit.health }}/{{ unit.maxHealth }} · 魔力
            {{ unit.mana }}/{{ unit.maxMana }}
          </p>
          <p>生命药 {{ member.supplies.HEALTH_POTION ?? 0 }} · 魔力药 {{ member.supplies.MANA_POTION ?? 0 }}</p>
        </details>
      </article>
      <IonButton :disabled="busy" class="leave" color="danger" fill="clear" @click="command('/' + lobby.id + '/leave')">
        离开队伍
      </IonButton>
      <DungeonInviteFriends
        v-if="!lobby.targetRun && self?.id === lobby.leader && lobby.members.length < 3"
        :key="lobby.id"
        :lobby="lobby.id"
        :active="active"
      />
    </template>
    <template v-else-if="!current.isPending.value">
      <article v-for="offer in offers.data.value?.offers" :key="offer.id" class="row">
        <strong>{{ offer.name }} · {{ offer.members }}/{{ offer.capacity }}</strong>
        <p>{{ offer.leader ?? '待任命队长' }} · {{ offer.professions.map(profession).join('、') }}</p>
        <IonButton
          :aria-label="`加入${offer.leader ?? '待任命队长'}的队伍`"
          :disabled="busy"
          fill="outline"
          @click="command('/' + offer.id + '/join')"
          >加入队伍
        </IonButton>
      </article>
      <p v-if="offers.data.value?.offers.length === 0" class="empty">当前页暂无招募</p>
      <div class="actions">
        <IonButton v-if="history.length" fill="clear" @click="after = history.pop() ?? '0'">上一页</IonButton>
        <IonButton v-if="offers.data.value?.next" fill="clear" @click="next">下一页</IonButton>
      </div>
      <h2>副本补员</h2>
      <article v-for="offer in replacements.data.value?.offers" :key="offer.id" :data-run-id="offer.id" class="row">
        <strong>{{ offer.name }} · {{ offer.members }}/{{ offer.capacity }}</strong>
        <p>已完成 {{ offer.cleared }}/{{ offer.stages }} 场 · {{ offer.professions.map(profession).join('、') }}</p>
        <IonButton :disabled="busy" fill="outline" @click="apply(offer.id)">申请补员</IonButton>
      </article>
      <p v-if="replacements.data.value?.offers.length === 0" class="empty">当前页暂无补员招募</p>
      <div class="actions">
        <IonButton
          v-if="replacementHistory.length"
          fill="clear"
          @click="replacementAfter = replacementHistory.pop() ?? '0'"
          >上一页补员</IonButton
        >
        <IonButton v-if="replacements.data.value?.next" fill="clear" @click="nextReplacement">下一页补员</IonButton>
      </div>
    </template>
    <template #footer>
      <template v-if="lobby && !lobby.closed">
        <IonButton
          v-if="self && !self.confirmed"
          :disabled="busy || (!lobby.targetRun && lobby.members.length !== 3)"
          @click="command('/' + lobby.id + '/confirm')"
          >确认加入
        </IonButton>
        <IonButton v-if="self?.confirmed" :disabled="busy || self.inBattle" @click="command('/' + lobby.id + '/ready')">
          确认出战准备
        </IonButton>
        <IonButton v-if="!lobby.leader" :disabled="busy" @click="command('/' + lobby.id + '/lead')">担任队长</IonButton>
        <IonButton
          v-if="self?.id === lobby.leader"
          :disabled="busy || !lobby.canLaunch"
          @click="command('/' + lobby.id + '/launch')"
          >进入副本
        </IonButton>
      </template>
      <template v-else>
        <IonButton :disabled="busy || current.isPending.value" @click="create">发起招募</IonButton>
        <IonButton :disabled="busy || current.isPending.value" fill="outline" @click="command('/match')"
          >自动匹配</IonButton
        >
      </template>
    </template>
  </AppPage>
</template>
<style scoped>
summary {
  padding: 12px 0;
  cursor: pointer;
  font-size: 14px;
}

.leave {
  margin-top: 20px;
}
</style>
