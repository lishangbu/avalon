<script setup lang="ts">
import {lifeActivities} from '@/features/life/activities'
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import LifeActivityPage from '@/features/life/ui/LifeActivityPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useFarm} from '@/features/life/farming'
const farm = useFarm(usePageActive())
const view = farm.query.data
const selected = ref<number[]>([]),
  cropId = ref(''),
  seed = ref(''),
  tool = ref(''),
  tools = ref(1),
  message = ref('')
const crop = computed(() => view.value?.crops.find((value) => value.id === cropId.value))
const seeds = computed(() => view.value?.inventory.filter((value) => value.code === crop.value?.seed) ?? [])
const hoes = computed(
  () =>
    view.value?.inventory.filter((item) =>
      view.value?.tools.some(
        (value) => value.item === item.code && value.tier >= Math.floor(((crop.value?.level ?? 1) - 1) / 2),
      ),
    ) ?? [],
)
const ready = computed(() => !!view.value?.atFarm && !!view.value.learned && !farm.busy.value)
const selectedPlots = computed(() => view.value?.plots.filter((value) => selected.value.includes(value.index)) ?? [])
const canPlant = computed(() => selectedPlots.value.length > 0 && selectedPlots.value.every((value) => !value.crop))
const canWater = computed(
  () => selectedPlots.value.length > 0 && selectedPlots.value.every((value) => value.crop && !value.watered),
)
const canHarvest = computed(() => selectedPlots.value.length > 0 && selectedPlots.value.every((value) => value.mature))
const stops: Record<string, string> = {
  COMPLETED: '操作完成',
  TOOLS_EXHAUSTED: '选定锄头全部报废，后续播种已停止',
  SEEDS_EXHAUSTED: '所选种子已用完',
  BAG_FULL: '背包已满，剩余作物留在田里',
}
async function perform(action: string) {
  const result = await farm.perform(
    action,
    action === 'EXPAND' ? [] : selected.value,
    action === 'PLANT' ? {crop: cropId.value, seed: seed.value, tool: tool.value, tools: tools.value} : undefined,
  )
  if (result) {
    message.value =
      result === true
        ? '操作完成'
        : `${stops[result.stop]}。完成${result.completed}格，收获${result.produced}件，报废${result.broken}把锄头。当前累计熟练度${result.proficiency}。`
    selected.value = []
  }
}
function resetMaterials() {
  seed.value = ''
  tool.value = ''
}
function choose(kind: 'empty' | 'water' | 'mature') {
  selected.value =
    view.value?.plots
      .filter((value) =>
        kind === 'empty' ? !value.crop : kind === 'water' ? value.crop && !value.watered : value.mature,
      )
      .map((value) => value.index) ?? []
}
</script>
<template>
  <LifeActivityPage
    activity="farm"
    :pending="farm.query.isPending.value"
    :errors="[farm.error.value, farm.query.error.value?.message]"
    :result="message"
  >
    <template v-if="view">
      <AppNotice v-if="!view.atFarm" tone="warning">请到村庄广场或诺姆城操作农田。</AppNotice>
      <AppNotice v-if="!view.learned" tone="warning">请先学习种植。</AppNotice>
      <p>种植{{ view.level }}级 · 地块 {{ view.plots.length }} / {{ view.maximumPlots }}</p>
      <IonButton fill="outline" :disabled="!ready" @click="choose('empty')">选空地</IonButton>
      <IonButton fill="outline" :disabled="!ready" @click="choose('water')">选待浇水</IonButton>
      <IonButton fill="outline" :disabled="!ready" @click="choose('mature')">选成熟作物</IonButton>
      <div class="plots">
        <label v-for="plot in view.plots" :key="plot.index" class="plot" :class="{mature: plot.mature}">
          <span
            ><input
              v-model="selected"
              type="checkbox"
              :value="plot.index"
              :disabled="!ready"
              :aria-label="`选择地块${plot.index + 1}`"
            />地块{{ plot.index + 1 }}</span
          >
          <strong>{{ plot.name ?? '空地' }}</strong>
          <span v-if="plot.crop">{{ plot.bound ? '收获物绑定' : '收获物未绑定' }}</span>
          <span v-if="plot.crop">{{
            plot.mature ? '可收获' : !plot.watered ? '等待浇水' : `约${Math.ceil(plot.remainingSeconds / 60)}分钟后成熟`
          }}</span>
        </label>
      </div>
      <fieldset :disabled="!ready">
        <legend>批量种植</legend>
        <label class="form-field"
          >作物<select v-model="cropId" @change="resetMaterials">
            <option value="" disabled>选择作物</option>
            <option v-for="value in view.crops" :key="value.id" :value="value.id" :disabled="value.level > view.level">
              {{ value.name }} · 种植{{ value.level }}级
            </option>
          </select></label
        >
        <p v-if="crop">浇水后{{ crop.growthSeconds / 60 }}分钟成熟，每格收获{{ crop.quantity }}件。</p>
        <label class="form-field"
          >种子<select v-model="seed">
            <option value="" disabled>选择种子</option>
            <option v-for="value in seeds" :key="value.id" :value="value.id">
              {{ value.name }} × {{ value.quantity }} · {{ value.bound ? '绑定' : '未绑定' }}
            </option>
          </select></label
        >
        <label class="form-field"
          >锄头<select v-model="tool">
            <option value="" disabled>选择锄头</option>
            <option v-for="value in hoes" :key="value.id" :value="value.id">
              {{ value.name }} × {{ value.quantity }} · {{ value.bound ? '绑定' : '未绑定' }}
            </option>
          </select></label
        >
        <label class="form-field"
          >允许使用的锄头总数（含备用）<input v-model.number="tools" type="number" min="1" max="99" inputmode="numeric"
        /></label>
        <p>每格消耗一颗种子，播种成功后判定锄头报废。收获才增加熟练度。</p>
        <p>遗忘种植会保留田里的作物，重新学习后可以收获；高于当前技能等级的旧作物不增加熟练度。</p>
      </fieldset>
      <IonButton :disabled="!ready || !canPlant || !crop || !seed || !tool" @click="perform('PLANT')"
        >播种所选地块</IonButton
      >
      <IonButton :disabled="!ready || !canWater" @click="perform('WATER')">浇水所选地块</IonButton>
      <IonButton :disabled="!ready || !canHarvest" @click="perform('HARVEST')">收获所选地块</IonButton>
      <IonButton fill="outline" :disabled="!ready || view.plots.length >= view.maximumPlots" @click="perform('EXPAND')"
        >扩建一格 · {{ view.expansionGold }}金币</IonButton
      >
      <IonButton :router-link="lifeActivities.production.path" fill="outline">购买种子与木锄头</IonButton>
    </template>
  </LifeActivityPage>
</template>
<style scoped>
.plots {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 0.5rem;
  margin-block: 1rem;
}
.plot {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid var(--ion-color-medium);
  border-radius: 0.5rem;
  min-height: 100px;
}
.plot.mature {
  border: 2px solid var(--ion-color-success);
}
input[type='checkbox'] {
  min-width: 24px;
  min-height: 24px;
  vertical-align: middle;
  margin-right: 0.5rem;
}
</style>
