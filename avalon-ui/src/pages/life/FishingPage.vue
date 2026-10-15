<script setup lang="ts">
import {lifeActivities} from '@/features/life/activities'
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import LifeActivityPage from '@/features/life/ui/LifeActivityPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useFishing} from '@/features/life/fishing'
const fishing = useFishing(usePageActive()),
  session = fishing.session
const fishId = ref(''),
  tool = ref(''),
  bait = ref('')
const fish = computed(() => fishing.query.data.value?.fish.find((value) => value.id === fishId.value))
const rods = computed(
  () =>
    fishing.query.data.value?.inventory.filter((item) =>
      fishing.query.data.value?.tools.some(
        (value) => value.item === item.code && value.tier >= Math.floor(((fish.value?.level ?? 1) - 1) / 2),
      ),
    ) ?? [],
)
const baits = computed(
  () => fishing.query.data.value?.inventory.filter((item) => item.code === fishing.query.data.value?.bait) ?? [],
)
const playing = computed(() => session.value?.state.status === 'PLAYING')
const statuses: Record<string, string> = {
  CAUGHT: '钓获成功',
  ESCAPED: '鱼逃走了，本局结束',
  BAG_FULL: '背包已满，鱼已放回水中',
  TOOLS_EXHAUSTED: '鱼竿已不在背包，本局结束',
}
const zone = computed(() => {
  const state = session.value
  if (!state) return {}
  const low = Math.max(0, state.fishPosition - state.tolerance),
    high = Math.min(1, state.fishPosition + state.tolerance)
  return {bottom: `${low * 100}%`, height: `${(high - low) * 100}%`}
})
function press(event: PointerEvent) {
  if (!event.isPrimary || event.button !== 0) return
  if (event.currentTarget instanceof HTMLElement) event.currentTarget.setPointerCapture(event.pointerId)
  fishing.press(true)
}
function resetTool() {
  tool.value = ''
}
</script>
<template>
  <LifeActivityPage
    activity="fishing"
    :pending="fishing.query.isPending.value"
    :errors="[fishing.error.value, fishing.connectionError.value, fishing.query.error.value?.message]"
  >
    <template v-if="playing && session">
      <p>已用{{ (session.state.elapsedMillis / 1000).toFixed(1) }}秒 · 最长20秒</p>
      <label>钓获进度<progress :value="session.state.caughtMillis" max="8000" /></label>
      <div class="fishing-track" role="img" aria-label="钓鱼水域：让浮标保持在鱼区内">
        <div class="fish-zone" :style="zone">鱼区</div>
        <div class="bobber" :style="{bottom: `${session.state.position * 100}%`}">● 浮标</div>
      </div>
      <button
        class="hold-button"
        :disabled="!fishing.online.value"
        type="button"
        @pointerdown.prevent="press"
        @pointerup="fishing.press(false)"
        @pointercancel="fishing.press(false)"
        @lostpointercapture="fishing.press(false)"
        @keydown.space.prevent="fishing.press(true)"
        @keyup.space.prevent="fishing.press(false)"
        @keydown.enter.prevent="fishing.press(true)"
        @keyup.enter.prevent="fishing.press(false)"
        @blur="fishing.press(false)"
        @contextmenu.prevent
      >
        按住上升 · 松开下降
      </button>
      <IonButton fill="outline" @click="fishing.cancel">收竿结束本局</IonButton>
    </template>
    <template v-else>
      <AppNotice v-if="session" tone="info"
        >{{ statuses[session.state.status] }}。{{ session.broken ? '鱼竿已报废。' : '' }}</AppNotice
      >
      <p v-if="fishing.query.data.value?.level === 0">请先学习钓鱼。</p>
      <p v-if="fishing.query.data.value?.fish.length === 0">
        当前地点没有钓鱼水域。初学者可前往河岸；更高档鱼种分布在赤铜湿地、铁脊高地、银霜林地和晨金谷地。
      </p>
      <label class="form-field"
        >目标鱼种<select v-model="fishId" aria-label="目标鱼种" @change="resetTool">
          <option value="" disabled>选择鱼种</option>
          <option
            v-for="value in fishing.query.data.value?.fish"
            :key="value.id"
            :value="value.id"
            :disabled="value.level > (fishing.query.data.value?.level ?? 0)"
          >
            {{ value.name }} · 钓鱼{{ value.level }}级
          </option>
        </select></label
      >
      <label class="form-field"
        >鱼竿<select v-model="tool" aria-label="鱼竿">
          <option value="" disabled>选择鱼竿</option>
          <option v-for="value in rods" :key="value.id" :value="value.id">
            {{ value.name }} × {{ value.quantity }}
          </option>
        </select></label
      >
      <label class="form-field"
        >鱼饵<select v-model="bait" aria-label="鱼饵">
          <option value="" disabled>选择鱼饵</option>
          <option v-for="value in baits" :key="value.id" :value="value.id">
            {{ value.name }} × {{ value.quantity }} · {{ value.bound ? '绑定' : '未绑定' }}
          </option>
        </select></label
      >
      <IonButton
        :disabled="fishing.busy.value || !fishing.online.value || !fishId || !tool || !bait"
        @click="fishing.start(fishId, tool, bait)"
        >抛竿 · 消耗1份鱼饵</IonButton
      >
      <IonButton :router-link="lifeActivities.production.path" fill="outline">购买鱼饵与木鱼竿</IonButton>
    </template>
  </LifeActivityPage>
</template>
<style scoped>
.fishing-track {
  position: relative;
  height: min(38vh, 300px);
  width: min(100%, 220px);
  margin: 1.5rem auto;
  border: 2px solid var(--ion-color-medium);
  background: #102e47;
  border-radius: 12px;
}
.fish-zone {
  position: absolute;
  width: 100%;
  background: #166534;
  color: #fff;
  border-block: 2px dashed #fff;
  display: grid;
  place-items: center;
}
.bobber {
  position: absolute;
  width: 100%;
  text-align: center;
  color: #fff;
  background: #7c2d12;
  transform: translateY(50%);
  font-weight: bold;
}
.hold-button {
  width: 100%;
  min-height: 64px;
  font-size: 1.1rem;
  color: var(--ion-color-primary-contrast);
  background: var(--ion-color-primary);
  border-radius: 12px;
  touch-action: none;
  user-select: none;
}
.hold-button:disabled {
  opacity: 0.55;
}
.hold-button:active {
  filter: brightness(0.85);
}
progress {
  width: 100%;
}
</style>
