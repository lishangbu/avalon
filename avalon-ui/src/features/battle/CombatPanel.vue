<script lang="ts" setup>
/** 单人和副本共用选招面板。仅自己的单位可编队，技能目标按服务器声明筛选。 */
import {computed, reactive, ref, watch} from 'vue'
import {IonButton} from '@ionic/vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import type {BattleUnit, Order} from './types'
import ContentLink from '@/features/content/ContentLink.vue'

const props = defineProps<{
  battleId: string
  round: number
  units: BattleUnit[]
  controlled: string[]
  active: boolean
  disabled: boolean
  telegraph: string | null
  log: string[]
  owners?: Record<string, string>
}>()
const emit = defineEmits<{draft: [orders: Order[], replacements: Record<string, number>, valid: boolean]}>()
const orders = reactive<Record<string, Order>>({}),
  replacements = reactive<Record<number, string>>({})
const selected = ref(''),
  logOpen = ref(false)
const own = computed(() => props.units.filter((unit) => props.controlled.includes(unit.id)))
const enemies = computed(() => props.units.filter((unit) => unit.enemy))
const allies = computed(() => props.units.filter((unit) => !unit.enemy && !props.controlled.includes(unit.id)))
const actors = computed(() =>
  own.value.filter(
    (unit) => unit.hp > 0 && (unit.hero || unit.position < 2 || Object.values(replacements).includes(unit.id)),
  ),
)
const reserves = computed(() => own.value.filter((unit) => !unit.hero && unit.position >= 2 && unit.position < 4))
const vacancies = computed(() =>
  [0, 1].filter((position) => !own.value.some((unit) => !unit.hero && unit.position === position && unit.hp > 0)),
)
const actor = computed(() => actors.value.find((unit) => unit.id === selected.value))
const selectedSkill = computed(() => {
  const action = actor.value?.actions.find((value) => value.id === orders[actor.value!.id]?.action)
  return action && action.id === action.name ? action.id : undefined
})
const multiple = computed(
  () => actor.value?.actions.find((action) => action.id === orders[actor.value!.id]?.action)?.target === 'ENEMIES',
)

// 多人目标仍提交单位编号；归属只用于区分同名伙伴，不能改变原始单位名称或控制权。
function unitLabel(unit: BattleUnit) {
  const owner = props.owners?.[unit.id]
  return owner && !unit.hero ? `${owner} · ${unit.name}` : unit.name
}

function targets(unit: BattleUnit, actionId: string) {
  const mode = unit.actions.find((action) => action.id === actionId)?.target
  return props.units.filter((target) => {
    if (target.hp <= 0 || target.captured) return false
    if (mode === 'RESERVE')
      return (
        props.controlled.includes(target.id) &&
        !target.hero &&
        target.position >= 2 &&
        target.position < 4 &&
        !Object.values(replacements).includes(target.id)
      )
    if (!target.enemy && !target.hero && target.position >= 2 && !Object.values(replacements).includes(target.id))
      return false
    if (mode === 'SELF') return target.id === unit.id
    if (mode === 'ENEMY' || mode === 'ENEMIES') return target.enemy
    return !target.enemy && (mode !== 'OTHER' || target.id !== unit.id)
  })
}

function change(unit: BattleUnit) {
  const order = orders[unit.id]
  if (order) {
    order.target = targets(unit, order.action)[0]?.id ?? ''
    order.targets = []
  }
}

function initialize(unit: BattleUnit) {
  orders[unit.id] = {
    actor: unit.id,
    action:
      unit.actions.find((action) => action.id === 'ATTACK' && action.available)?.id ??
      unit.actions.find((action) => action.available)?.id ??
      '',
    target: '',
  }
  change(unit)
}

// 同一回合的轮询只能更新状态，不覆盖草稿；下一回合重新选择多目标和补位。
watch(
  () => `${props.battleId}:${props.round}`,
  () => {
    for (const id of Object.keys(orders)) delete orders[id]
    for (const position of Object.keys(replacements)) delete replacements[Number(position)]
    for (const unit of actors.value) initialize(unit)
    selected.value = actors.value[0]?.id ?? ''
  },
  {immediate: true},
)
watch(actors, (units) => {
  for (const id of Object.keys(orders)) if (!units.some((unit) => unit.id === id)) delete orders[id]
  for (const unit of units) if (!orders[unit.id]) initialize(unit)
  if (!units.some((unit) => unit.id === selected.value)) selected.value = units[0]?.id ?? ''
})
const valid = computed(() =>
  actors.value.every((unit) => {
    const order = orders[unit.id],
      action = unit.actions.find((item) => item.id === order?.action)
    if (!order || !action?.available) return false
    const available = targets(unit, order.action).map((item) => item.id)
    return action.target === 'ENEMIES'
      ? !!order.targets?.length && order.targets.length <= 3 && order.targets.every((id) => available.includes(id))
      : available.includes(order.target)
  }),
)
watch(
  [orders, replacements, valid, actors],
  () => {
    emit(
      'draft',
      actors.value.map((unit) => ({
        ...orders[unit.id]!,
        targets: [...(orders[unit.id]?.targets ?? [])],
      })),
      Object.fromEntries(
        Object.entries(replacements)
          .filter(([, id]) => id)
          .map(([position, id]) => [id, Number(position)]),
      ),
      valid.value,
    )
  },
  {deep: true, immediate: true},
)

function summary(unit: BattleUnit) {
  const order = orders[unit.id]
  if (!order) return '尚未选择'
  return `${unit.actions.find((action) => action.id === order.action)?.name ?? '尚未选择'} → ${(order.targets?.length
    ? order.targets
    : [order.target]
  )
    .map((id) => {
      const target = props.units.find((unit) => unit.id === id)
      return target ? unitLabel(target) : '待选目标'
    })
    .join('、')}`
}
</script>
<template>
  <AppNotice v-if="active && telegraph" tone="warning">{{ telegraph }}</AppNotice>
  <section aria-label="敌方" class="enemy">
    <article v-for="unit in enemies" :key="unit.id" class="row">
      <div class="row-title">
        <strong>{{ unit.name }} · Lv.{{ unit.level }}</strong
        ><small>{{ unit.element }}</small>
      </div>
      <progress :aria-label="unit.name + '生命'" :max="unit.stats.health" :value="unit.hp" />
      <p>
        生命 {{ unit.hp }}/{{ unit.stats.health }} · 魔力 {{ unit.mp }}/{{ unit.stats.mana
        }}{{ unit.captured ? ' · 已捕获' : '' }}
      </p>
      <p v-if="unit.effects.length">{{ unit.effects.join(' · ') }}</p>
      <details v-if="active && !unit.captured && unit.captures.length">
        <summary>捕捉信息</summary>
        <p v-for="capture in unit.captures" :key="capture.code">
          {{ capture.name }} · {{ Number((capture.chance * 100).toFixed(2)) }}% · 适用 Lv.{{ capture.level }} 以下
        </p>
        <p v-if="unit.captureStats">
          捕获后：生命 {{ unit.captureStats.health }} · 魔力 {{ unit.captureStats.mana }} · 物攻
          {{ unit.captureStats.physicalAttack }} · 法攻 {{ unit.captureStats.magicAttack }} · 物防
          {{ unit.captureStats.physicalDefense }} · 法防 {{ unit.captureStats.magicDefense }} · 速度
          {{ unit.captureStats.speed }}
        </p>
      </details>
    </article>
  </section>
  <section aria-label="我方">
    <h2>我方</h2>
    <div class="actor-strip">
      <button
        v-for="unit in own.filter(
          (item) => item.hero || item.position < 2 || Object.values(replacements).includes(item.id),
        )"
        :key="unit.id"
        :class="{selected: selected === unit.id}"
        :disabled="!active || disabled || unit.hp <= 0"
        @click="selected = unit.id"
      >
        <strong>{{ unit.name }}</strong
        ><small>{{ unit.hp === 0 ? '已倒下' : `生命 ${unit.hp}/${unit.stats.health}` }}</small>
        <progress :max="unit.stats.health" :value="unit.hp" />
      </button>
    </div>
    <template v-if="active && actor && orders[actor.id]"
      ><h3>{{ actor.name }} · 魔力 {{ actor.mp }}/{{ actor.stats.mana }}</h3>
      <p v-if="actor.effects.length">{{ actor.effects.join(' · ') }}</p>
      <p v-if="actor.shield || actor.slowRounds">护盾 {{ actor.shield }} · 减速 {{ actor.slowRounds }} 回合</p>
      <label class="form-field"
        >行动<select
          v-model="orders[actor.id]!.action"
          :aria-label="actor.name + '行动'"
          :disabled="disabled"
          @change="change(actor)"
        >
          <option v-for="action in actor.actions" :key="action.id" :disabled="!action.available" :value="action.id">
            {{ action.name }}{{ action.mana ? ` · ${action.mana}魔力` : ''
            }}{{ action.cooldown ? ` · 冷却${action.cooldown}` : '' }}
          </option>
        </select></label
      >
      <ContentLink v-if="selectedSkill" :key="selectedSkill" kind="skill" :code="selectedSkill" name="查看招式说明" />
      <ContentLink
        v-if="actor.ability"
        :key="actor.ability"
        kind="ability"
        :code="actor.ability"
        :name="`特性：${actor.ability}`"
      />
      <fieldset v-if="multiple">
        <legend>目标（最多 3 个）</legend>
        <label v-for="target in targets(actor, orders[actor.id]!.action)" :key="target.id" class="check-target"
          ><input
            v-model="orders[actor.id]!.targets"
            :disabled="
              disabled ||
              ((orders[actor.id]!.targets?.length ?? 0) >= 3 && !orders[actor.id]!.targets?.includes(target.id))
            "
            :value="target.id"
            type="checkbox"
          />{{ target.name }}</label
        >
      </fieldset>
      <label v-else class="form-field"
        >目标<select v-model="orders[actor.id]!.target" :aria-label="actor.name + '目标'" :disabled="disabled">
          <option v-for="target in targets(actor, orders[actor.id]!.action)" :key="target.id" :value="target.id">
            {{ unitLabel(target) }}
          </option>
        </select></label
      >
    </template>
    <details v-if="reserves.length">
      <summary>替补（{{ reserves.length }}）</summary>
      <p v-for="unit in reserves" :key="unit.id">
        {{ unit.name }} · 生命 {{ unit.hp }}/{{ unit.stats.health }} · 魔力 {{ unit.mp }}/{{ unit.stats.mana }}
      </p>
    </details>
    <template v-if="active && reserves.some((unit) => unit.hp > 0)"
      ><label v-for="position in vacancies" :key="position" class="form-field"
        >第 {{ position + 1 }} 出战位补位<select v-model="replacements[position]" :disabled="disabled">
          <option value="">暂不补位</option>
          <option
            v-for="unit in reserves.filter((item) => item.hp > 0)"
            :key="unit.id"
            :disabled="Object.entries(replacements).some(([slot, id]) => Number(slot) !== position && id === unit.id)"
            :value="unit.id"
          >
            {{ unit.name }}
          </option>
        </select></label
      ></template
    >
    <details v-if="allies.length">
      <summary>队友单位（{{ allies.length }}）</summary>
      <p v-for="unit in allies" :key="unit.id">
        {{ unitLabel(unit) }} · 生命 {{ unit.hp }}/{{ unit.stats.health }} · {{ unit.effects.join(' · ') }}
      </p>
    </details>
    <template v-if="active"
      ><h2>本回合指令</h2>
      <div v-for="unit in actors" :key="unit.id" class="order-summary">
        <strong>{{ unit.name }}</strong
        ><span>{{ summary(unit) }}</span>
      </div>
    </template>
  </section>
  <section aria-label="战报">
    <h2>最近战报</h2>
    <p v-for="(line, index) in log.slice(-3)" :key="index">{{ line }}</p>
    <IonButton fill="clear" @click="logOpen = true">完整战报</IonButton>
  </section>
  <AppSheet v-model="logOpen" title="完整战报"
    ><p v-for="(line, index) in log" :key="index">{{ line }}</p></AppSheet
  >
</template>
<style scoped>
progress {
  margin-top: 9px;
}

.actor-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.actor-strip button {
  border: 1px solid var(--game-border);
  border-radius: 8px;
  padding: 10px 6px;
  color: var(--ion-text-color);
  background: var(--game-surface);
  text-align: left;
  min-height: 95px;
}

.actor-strip button.selected {
  border-color: var(--ion-color-primary);
  background: var(--game-tint);
}

.actor-strip strong {
  font-size: 13px;
  display: block;
}

.actor-strip small {
  font-size: 11px;
  display: block;
  margin-top: 6px;
}

details {
  border-bottom: 1px solid var(--game-border);
  padding: 8px 0;
}

summary {
  min-height: 44px;
  padding: 12px 0;
  font-size: 14px;
  cursor: pointer;
}

fieldset {
  border: 1px solid var(--game-border);
  border-radius: 8px;
  min-width: 0;
}

.check-target {
  display: flex;
  align-items: center;
  min-height: 44px;
  gap: 8px;
}

.order-summary {
  display: grid;
  gap: 6px;
  border-bottom: 1px solid var(--game-border);
  padding: 12px 0;
  font-size: 13px;
}

.order-summary span {
  color: var(--game-muted);
  overflow-wrap: anywhere;
}
.enemy progress::-webkit-progress-value {
  background: var(--game-enemy);
}
</style>
