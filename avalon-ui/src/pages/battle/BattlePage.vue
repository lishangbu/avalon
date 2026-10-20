<script lang="ts" setup>
/** 路由在活动战斗中锁定。父页面只提交共享面板的一整轮草稿，不自动执行回合。 */
import {computed, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import CombatPanel from '@/features/battle/CombatPanel.vue'
import DungeonLobbyNotice from '@/features/dungeon/DungeonLobbyNotice.vue'
import {battleOptions, battleKey} from '@/features/battle/queries'
import {useBattleActions} from '@/features/battle/actions'
import {usePageActive} from '@/shared/usePageActive'
import type {Order, PvpView} from '@/features/battle/types'
import {api} from '@/shared/http'
import ContentLink from '@/features/content/ContentLink.vue'

const router = useRouter(),
  pageActive = usePageActive()
const query = useQuery({...battleOptions(), enabled: pageActive}),
  battle = query.data
const action = useBattleActions(),
  surrenderOpen = ref(false)
const client = useQueryClient()
const pvp = useQuery({
  queryKey: computed(() => ['pvp', battle.value?.id]),
  enabled: computed(() => pageActive.value && !!battle.value?.pvp && battle.value.status === 'ACTIVE'),
  queryFn: () => api<PvpView>(`/api/battle/pvp/${battle.value!.id}`),
  refetchInterval: 2000,
  retry: false,
})
watch(pvp.data, (value) => {
  if (
    value &&
    value.battle.id === battle.value?.id &&
    value.battle.revision >= battle.value.revision &&
    (battle.value.status === 'ACTIVE' || value.battle.status !== 'ACTIVE')
  )
    client.setQueryData(battleKey, value.battle)
})
const submitted = computed(() => pvp.data.value?.battle.round === battle.value?.round && pvp.data.value?.submitted)
// 仅恢复当前服务端战局对应的原命令，不在挂载或轮询时自动发送。
watch(
  () => battle.value?.id,
  (id) => {
    if (id) action.restoreTurn(id)
  },
  {immediate: true},
)
const orders = ref<Order[]>([]),
  replacements = ref<Record<string, number>>({}),
  valid = ref(false)
const active = computed(() => battle.value?.status === 'ACTIVE')
const captureTarget = computed(() =>
  battle.value?.units.find((unit) => unit.id === battle.value?.academyCapture?.targetId),
)
const status = computed(
  () =>
    ({
      WON: '战斗胜利',
      LOST: '队伍倒下',
      SURRENDERED: '已认输',
      RETREATED: '撤退成功',
      DRAW: '对战结束',
    })[battle.value?.status ?? ''] ?? '战斗',
)

function draft(value: Order[], positions: Record<string, number>, ready: boolean) {
  orders.value = value
  replacements.value = positions
  valid.value = ready
}

async function submit(surrender = false) {
  if (!battle.value) return
  const confirmed = await action.turn(battle.value, orders.value, surrender, replacements.value)
  // 响应丢失时原回合已经保存，必须让出弹窗遮挡，玩家才能访问页面上的原请求重试入口。
  // 这里只关闭确认弹窗，不推定认输成功；战局状态仍来自正式查询或原请求回执。
  // 明确业务拒绝且没有待确认命令时保留弹窗，继续显示错误，不自动重复提交。
  if (confirmed || action.pendingTurn.value) surrenderOpen.value = false
}
</script>
<template>
  <AppPage :title="battle?.name ?? '战斗'" locked>
    <DungeonLobbyNotice :active="pageActive" />
    <AppNotice v-if="action.error.value || query.error.value || pvp.error.value" tone="danger">
      {{ action.error.value || query.error.value?.message || pvp.error.value?.message }}
    </AppNotice>
    <AppNotice v-if="action.pendingTurn.value">
      有一轮操作尚未确认。重试将提交原来的行动，请先确认结果再继续选招。
      <IonButton :disabled="action.busy.value" @click="submit()">重试原回合</IonButton>
    </AppNotice>
    <template v-if="battle"
      ><h1>{{ active ? `第 ${battle.round} 回合` : status }}</h1>
      <AppNotice v-if="active && battle.pvp">
        {{ submitted ? '行动已锁定，等待对方提交或本轮结束。' : '双方提交后同时结算；超时未提交则本轮不行动。' }}
        <span v-if="pvp.data.value">本轮截止 {{ new Date(pvp.data.value.deadline).toLocaleTimeString() }}</span>
      </AppNotice>
      <section v-if="battle.academyCapture" aria-label="捕捉实践资料">
        <h2>捕捉实践资料</h2>
        <p>{{ battle.academyCapture.instruction }}</p>
        <p v-if="captureTarget">
          {{ captureTarget.name }} · Lv.{{ captureTarget.level }} · {{ captureTarget.element }}属性
        </p>
        <div v-if="active" class="stack">
          <div v-for="code in battle.academyCapture.balls" :key="code" class="row">
            <p>
              {{ battle.captureSupplies.find((ball) => ball.code === code)?.name ?? code }} · 适用 Lv.{{
                battle.captureSupplies.find((ball) => ball.code === code)?.level
              }}
              · 当前数量 {{ battle.supplies[code] ?? 0 }}
            </p>
            <p v-if="captureTarget?.captures.find((ball) => ball.code === code)">
              当前捕捉概率
              {{ ((captureTarget.captures.find((ball) => ball.code === code)?.chance ?? 0) * 100).toFixed(1) }}%
            </p>
            <IonButton
              :disabled="action.busy.value || !!action.pendingTurn.value"
              :fill="battle.academyCapture.selectedBall === code ? 'solid' : 'outline'"
              @click="action.captureInfo(battle.id, code)"
              >{{ battle.academyCapture.selectedBall === code ? '已查看并选择' : '查看并选择此球' }}</IonButton
            >
          </div>
        </div>
        <p v-if="battle.academyCapture.capturedAbilitySlot">
          已捕获伙伴：{{ battle.academyCapture.capturedAbilitySlot }}槽 · {{ battle.academyCapture.capturedAbility }}
        </p>
        <p>
          训练生命药II {{ battle.supplies.HEALTH_POTION_II ?? 0 }} · 训练魔力药II
          {{ battle.supplies.MANA_POTION_II ?? 0 }}
        </p>
      </section>
      <CombatPanel
        :active="active"
        :battle-id="battle.id"
        :controlled="battle.units.filter((unit) => !unit.enemy).map((unit) => unit.id)"
        :disabled="action.busy.value || !!action.pendingTurn.value || !!submitted"
        :log="battle.log"
        :round="battle.round"
        :telegraph="battle.telegraph"
        :units="battle.units"
        @draft="draft"
      />
      <section v-if="!active && battle.loot?.length" aria-label="获得物品">
        <h2>获得物品</h2>
        <ContentLink
          v-for="(item, index) in battle.loot"
          :key="index"
          kind="item"
          :code="item.code"
          :name="`${item.name} ×${item.quantity}`"
        />
      </section>
      <p v-if="!battle.training">
        生命药 {{ battle.supplies.HEALTH_POTION ?? 0 }} · 魔力药
        {{ battle.supplies.MANA_POTION ?? 0 }}
      </p></template
    >
    <section v-if="battle && !battle.training && !battle.pvp" aria-label="封印球库存">
      <h2>封印球</h2>
      <div v-for="ball in battle.captureSupplies" :key="ball.code" class="row row-title">
        <span
          >{{ ball.name }} <small>适用 Lv.{{ ball.level }}</small></span
        ><strong>×{{ ball.quantity }}</strong>
      </div>
      <p v-if="!battle.captureSupplies?.length" class="muted">暂无封印球</p>
    </section>
    <template #footer>
      <template v-if="active">
        <IonButton
          :disabled="action.busy.value || !!action.pendingTurn.value"
          fill="clear"
          @click="surrenderOpen = true"
        >
          {{ battle?.training ? '退出训练' : '认输' }}
        </IonButton>
        <IonButton
          :disabled="action.busy.value || !!submitted || (!valid && !action.pendingTurn.value)"
          @click="submit()"
          >提交本回合</IonButton
        >
      </template>
      <IonButton v-else @click="router.replace('/world')">返回地图</IonButton>
    </template>
    <AppSheet v-model="surrenderOpen" :busy="action.busy.value" :title="battle?.training ? '确认退出' : '确认认输'"
      ><p>
        {{
          battle?.training
            ? '本次训练进度将结束，真实队伍资源保持不变。'
            : '返回已解锁的安全城镇，已消耗的药品和封印球不会返还。'
        }}
      </p>
      <template #footer>
        <IonButton :disabled="action.busy.value" fill="outline" @click="surrenderOpen = false">取消</IonButton>
        <IonButton :disabled="action.busy.value" color="danger" @click="submit(true)">
          {{ battle?.training ? '确认退出' : '确认认输' }}
        </IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
