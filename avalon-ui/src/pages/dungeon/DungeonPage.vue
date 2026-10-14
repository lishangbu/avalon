<script lang="ts" setup>
import ContentLink from '@/features/content/ContentLink.vue'
/** 副本状态来自服务端，轮询不会自动准备。回合草稿与整轮提交复用单人战斗组件。 */
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import CombatPanel from '@/features/battle/CombatPanel.vue'
import {dungeonCombatOptions, dungeonCurrentOptions, dungeonRunOptions} from '@/features/dungeon/queries'
import {useDungeonActions} from '@/features/dungeon/actions'
import {useDungeonPresence} from '@/features/dungeon/presence'
import {usePageActive} from '@/shared/usePageActive'
import {usePageClock} from '@/shared/usePageClock'
import {HttpError} from '@/shared/http'
import type {Order} from '@/features/battle/types'

const id = String(useRoute().params.id),
  router = useRouter(),
  active = usePageActive(),
  now = usePageClock(active)
const interval = computed(() => (active.value ? 2000 : false))
const current = useQuery({...dungeonCurrentOptions(), enabled: active, refetchInterval: interval})
const progress = useQuery({...dungeonRunOptions(id), enabled: active, refetchInterval: interval}),
  run = progress.data
const presenceError = useDungeonPresence(
  id,
  computed(() => active.value && !!run.value && !run.value.battle && !run.value.completed),
)
// 由服务端恢复本人参与过的战报，刷新或通关后仍可读取；不继承其他成员的历史。
const lastBattle = computed(() => run.value?.battle ?? run.value?.previousBattle ?? ''),
  leaving = ref(false)
const combat = useQuery(
  computed(() => ({
    ...dungeonCombatOptions(id, lastBattle.value),
    enabled: active.value && !!lastBattle.value,
    refetchInterval: active.value && run.value?.battle ? 1500 : false,
  })),
)
watch(
  () => run.value?.battle,
  (battle, previous) => {
    if (active.value && !battle && previous) void combat.refetch()
  },
)
const action = useDungeonActions()
const seconds = computed(() =>
  Math.max(0, Math.ceil((Date.parse(combat.data.value?.deadline ?? '') - now.value) / 1000)),
)
const self = computed(() => run.value?.members.find((member) => member.id === run.value?.self))
const owners = computed(() =>
  Object.fromEntries(
    (combat.data.value?.units ?? [])
      .filter((unit) => unit.owner)
      .map((unit) => [
        unit.detail.id,
        unit.owner === combat.data.value?.self
          ? '你'
          : (run.value?.members.find((member) => member.id === unit.owner)?.name ?? '队友'),
      ]),
  ),
)
const orders = ref<Order[]>([]),
  replacements = ref<Record<string, number>>({}),
  valid = ref(false)
const locked = computed(() => action.busy.value || !combat.data.value?.canSubmit || !seconds.value)
const professions: Record<string, string> = {WARRIOR: '战士', MAGE: '法师', PRIEST: '牧师', RANGER: '游侠'}
// 离场投影可能早于确认弹层关闭返回；关闭后再次检查，不能只依赖 current 数据变化。
watch(
  [() => current.data.value, leaving, action.busy, active],
  ([state]) => {
    if (active.value && !leaving.value && !action.busy.value && state && !state.run && !run.value?.completed)
      void progress.refetch().then((result) => {
        if (result.error && (!(result.error instanceof HttpError) || result.error.status !== 404)) return
        if (active.value && (!result.data?.completed || result.error)) void router.replace('/world')
      })
  },
  {flush: 'post'},
)

function draft(value: Order[], positions: Record<string, number>, ready: boolean) {
  orders.value = value
  replacements.value = positions
  valid.value = ready
}

async function submit() {
  const battle = combat.data.value
  if (run.value?.battle === battle?.id && battle)
    await action.submit(id, battle.id, {
      round: battle.round,
      revision: battle.revision,
      orders: orders.value,
      replacements: replacements.value,
    })
}

async function leave() {
  if (await action.command(id, 'leave')) leaving.value = false
}
</script>
<template>
  <AppPage :title="run?.name ?? '副本'" locked>
    <AppNotice v-if="presenceError" tone="danger">{{ presenceError }}</AppNotice>
    <AppNotice
      v-if="action.error.value || progress.error.value || current.error.value || combat.error.value"
      tone="danger"
      >{{
        action.error.value ||
        progress.error.value?.message ||
        current.error.value?.message ||
        combat.error.value?.message
      }}
    </AppNotice>
    <template v-if="run"
      ><p>
        进度 {{ run.cleared }}/{{ run.route.length - 1 }} · 今日剩余发起
        {{ current.data.value?.remainingStarts ?? '-' }} 次
      </p>
      <template v-if="run.battle">
        <template v-if="combat.data.value?.id === run.battle"
          ><h1>{{ combat.data.value.name }} · 第 {{ combat.data.value.round }} 回合</h1>
          <p>提交剩余 {{ seconds }} 秒</p>
          <div aria-label="提交状态">
            <p v-for="member in combat.data.value.members" :key="member.id">
              {{ run.members.find((item) => item.id === member.id)?.name ?? '队员' }} ·
              {{ member.removed ? '已移出本场' : member.leaving ? '等待离场' : member.submitted ? '已提交' : '选招中' }}
            </p>
          </div>
          <CombatPanel
            :active="combat.data.value.status === 'ACTIVE'"
            :battle-id="combat.data.value.id"
            :controlled="
              combat.data.value.units
                .filter((unit) => unit.owner === combat.data.value?.self)
                .map((unit) => unit.detail.id)
            "
            :disabled="locked"
            :log="combat.data.value.log"
            :owners="owners"
            :round="combat.data.value.round"
            :telegraph="combat.data.value.telegraph"
            :units="combat.data.value.units.map((unit) => unit.detail)"
            @draft="draft"
          />
          <AppNotice v-if="!combat.data.value.canSubmit">等待本轮结算</AppNotice>
          <section v-if="combat.data.value.loot?.length" aria-label="本场获得物品">
            <h2>本场获得物品</h2>
            <ContentLink
              v-for="(item, index) in combat.data.value.loot"
              :key="index"
              kind="item"
              :code="item.code"
              :name="`${item.name} ×${item.quantity}`"
            />
          </section>
        </template>
        <p v-else role="status">正在恢复战局…</p></template
      >
      <template v-else>
        <AppNotice v-if="run.completed" tone="success">副本已通关</AppNotice>
        <section aria-label="副本路线">
          <h2>路线</h2>
          <ol class="route">
            <li v-for="node in run.route" :key="node.position" :class="{current: self?.location === node.position}">
              <strong>{{ node.name }}</strong>
              <p>
                {{ node.cleared ? '已通行' : '未清理' }}{{ node.rest ? ' · 休整点' : ''
                }}{{ self?.location === node.position ? ' · 当前位置' : '' }}
              </p>
              <IonButton
                v-if="run.destinations.includes(node.position)"
                :disabled="action.busy.value"
                fill="outline"
                @click="action.command(id, 'move', {location: node.position})"
                >前往{{ node.name }}
              </IonButton>
            </li>
          </ol>
        </section>
        <section aria-label="队伍准备">
          <h2>队伍</h2>
          <IonButton
            v-if="run.canRecruit"
            :disabled="action.busy.value"
            fill="outline"
            @click="action.command(id, 'recruit', {enabled: !run.recruiting})"
            >{{ run.recruiting ? '停止补员招募' : '发布补员招募' }}</IonButton
          >
          <p v-if="run.canRecruit && run.recruiting">补员招募中</p>
          <article v-for="member in run.members" :key="member.id" class="row">
            <div class="row-title">
              <strong>{{ member.name }}{{ member.id === run.self ? '（你）' : '' }}</strong
              ><small>{{ member.ready ? '已准备' : '未准备' }}</small>
            </div>
            <p>
              {{ professions[member.profession] }} · Lv.{{ member.level }} ·
              {{ run.route.find((node) => node.position === member.location)?.name }}
            </p>
          </article>
        </section>
        <details v-if="combat.data.value">
          <summary>上一场战报</summary>
          <p v-for="(line, index) in combat.data.value.log" :key="index">{{ line }}</p>
        </details>
      </template>
      <p>
        生命药 {{ (run.battle ? combat.data.value?.supplies : run.supplies)?.HEALTH_POTION ?? 0 }} · 魔力药
        {{ (run.battle ? combat.data.value?.supplies : run.supplies)?.MANA_POTION ?? 0 }}
      </p>
      <IonButton v-if="!run.completed" :disabled="action.busy.value" color="danger" fill="clear" @click="leaving = true"
        >退出副本
      </IonButton>
    </template>
    <template #footer>
      <template v-if="run?.battle">
        <IonButton :disabled="locked || !valid || combat.data.value?.id !== run.battle" @click="submit">
          {{ combat.data.value?.canSubmit ? '提交本回合' : '等待结算' }}
        </IonButton>
      </template>
      <template v-else-if="run">
        <IonButton v-if="run.canRest" :disabled="action.busy.value" fill="outline" @click="action.command(id, 'rest')">
          休整
        </IonButton>
        <IonButton v-if="run.canReady" :disabled="action.busy.value" @click="action.command(id, 'ready')"
          >准备就绪
        </IonButton>
        <IonButton v-if="run.canBegin" :disabled="action.busy.value" @click="action.command(id, 'begin')"
          >开始遭遇
        </IonButton>
        <IonButton v-if="run.completed" @click="router.replace('/world')">返回地图</IonButton>
      </template>
    </template>
    <AppSheet v-model="leaving" :busy="action.busy.value" title="退出副本"
      ><p>退出后不能返回本次进度。战斗中退出将在本轮结算后离场，已提交行动仍会执行。</p>
      <template #footer>
        <IonButton :disabled="action.busy.value" fill="outline" @click="leaving = false">取消</IonButton>
        <IonButton :disabled="action.busy.value" color="danger" @click="leave">确认退出</IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
<style scoped>
.route {
  list-style: none;
  margin: 0;
  padding-left: 12px;
}

.route li {
  border-left: 2px solid var(--game-border);
  padding: 8px 0 16px 16px;
}

.route li.current {
  border-color: var(--ion-color-primary);
}

summary {
  padding: 16px 0;
  min-height: 44px;
  cursor: pointer;
}
</style>
