<script lang="ts" setup>
/** 路由在活动战斗中锁定。父页面只提交共享面板的一整轮草稿，不自动执行回合。 */
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useQuery} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import CombatPanel from '@/features/battle/CombatPanel.vue'
import DungeonLobbyNotice from '@/features/dungeon/DungeonLobbyNotice.vue'
import {battleOptions} from '@/features/battle/queries'
import {useBattleActions} from '@/features/battle/actions'
import {usePageActive} from '@/shared/usePageActive'
import type {Order} from '@/features/battle/types'
import ContentLink from '@/features/content/ContentLink.vue'

const router = useRouter(),
  pageActive = usePageActive()
const query = useQuery({...battleOptions(), enabled: pageActive}),
  battle = query.data
const action = useBattleActions(),
  surrenderOpen = ref(false)
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
    })[battle.value?.status ?? ''] ?? '战斗',
)

function draft(value: Order[], positions: Record<string, number>, ready: boolean) {
  orders.value = value
  replacements.value = positions
  valid.value = ready
}

async function submit(surrender = false) {
  if (battle.value && (await action.turn(battle.value, orders.value, surrender, replacements.value)))
    surrenderOpen.value = false
}
</script>
<template>
  <AppPage :title="battle?.name ?? '战斗'" locked>
    <DungeonLobbyNotice :active="pageActive" />
    <AppNotice v-if="action.error.value || query.error.value" tone="danger">
      {{ action.error.value || query.error.value?.message }}
    </AppNotice>
    <template v-if="battle"
      ><h1>{{ active ? `第 ${battle.round} 回合` : status }}</h1>
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
              :disabled="action.busy.value"
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
        :disabled="action.busy.value"
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
    <section v-if="battle && !battle.training" aria-label="封印球库存">
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
        <IonButton :disabled="action.busy.value" fill="clear" @click="surrenderOpen = true">
          {{ battle?.training ? '退出训练' : '认输' }}
        </IonButton>
        <IonButton :disabled="action.busy.value || !valid" @click="submit()">提交本回合</IonButton>
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
