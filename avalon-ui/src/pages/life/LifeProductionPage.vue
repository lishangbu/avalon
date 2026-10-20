<script setup lang="ts">
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import LifeActivityPage from '@/features/life/ui/LifeActivityPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useProduction, type CraftResult} from '@/features/life/production'
const production = useProduction(usePageActive())
const sellItem = ref(''),
  sellQuantity = ref(1)
const sellable = computed(
  () =>
    production.query.data.value?.inventory.filter(
      (item) => !item.equipped && item.enhancementLevel === 0 && production.query.data.value?.sellPrices[item.code],
    ) ?? [],
)
const sellPrice = computed(
  () =>
    production.query.data.value?.sellPrices[sellable.value.find((item) => item.id === sellItem.value)?.code ?? ''] ?? 0,
)
const selected = ref(''),
  tool = ref(''),
  quantity = ref(1),
  toolCount = ref(1)
const materials = ref<Record<string, number>>({}),
  result = ref<CraftResult | null>(null)
const recipe = computed(() => production.query.data.value?.recipes.find((value) => value.id === selected.value))
const toolKinds: Record<string, string> = {ARTISAN: 'HAMMER', LEATHERWORK: 'NEEDLE', ALCHEMY: 'ROD', COOKING: 'POT'}
const toolTiers = ['木', '铜', '铁', '银', '金']
const toolNames: Record<string, string> = {ARTISAN: '锤', LEATHERWORK: '针', ALCHEMY: '棒', COOKING: '锅'}
const tools = computed(
  () =>
    production.query.data.value?.inventory.filter((item) =>
      production.query.data.value?.tools.some(
        (definition) =>
          definition.item === item.code &&
          definition.kind === toolKinds[recipe.value?.skill ?? ''] &&
          definition.tier >= (recipe.value?.minimumToolTier ?? 0),
      ),
    ) ?? [],
)
const ingredients = computed(
  () =>
    production.query.data.value?.inventory.filter(
      (item) =>
        !item.equipped &&
        item.enhancementLevel === 0 &&
        recipe.value?.ingredients.some((ingredient) => ingredient.code === item.code),
    ) ?? [],
)
const materialTotals = computed(
  () =>
    recipe.value?.ingredients.map((ingredient) => {
      const owned = ingredients.value.filter((item) => item.code === ingredient.code)
      const chosen = owned.reduce((total, item) => total + (Number(materials.value[item.id]) || 0), 0)
      return {
        ...ingredient,
        needed: ingredient.quantity * quantity.value,
        chosen,
        owned: owned.reduce((total, item) => total + item.quantity, 0),
        unbound: owned.filter((item) => !item.bound).reduce((total, item) => total + item.quantity, 0),
      }
    }) ?? [],
)
const usesBoundMaterials = computed(() =>
  ingredients.value.some((item) => item.bound && (materials.value[item.id] ?? 0) > 0),
)
const stops: Record<string, string> = {
  COMPLETED: '本批制造完成',
  TOOLS_EXHAUSTED: '选定工具全部报废，剩余制造已停止',
  MATERIALS_EXHAUSTED: '所选材料已不足，剩余制造已停止',
  BAG_FULL: '背包空间不足，剩余制造已停止',
}
const resultMessage = computed(() => {
  const value = result.value
  return value
    ? `${stops[value.stop] ?? value.stop}。完成${value.completed}次，获得${value.produced}件，报废${value.broken}把工具。当前累计熟练度${value.proficiency}。`
    : undefined
})
function reset() {
  tool.value = ''
  materials.value = {}
  result.value = null
}
function fillMaterials(includeBound: boolean) {
  if (
    production.busy.value ||
    !recipe.value ||
    !Number.isInteger(quantity.value) ||
    quantity.value < 1 ||
    quantity.value > 99
  )
    return
  const chosen: Record<string, number> = {}
  for (const ingredient of recipe.value.ingredients) {
    let remaining = ingredient.quantity * quantity.value
    const stacks = ingredients.value
      .filter((item) => item.code === ingredient.code && (includeBound || !item.bound))
      .sort((a, b) => Number(a.bound) - Number(b.bound))
    for (const item of stacks) {
      const take = Math.min(remaining, item.quantity)
      if (take > 0) chosen[item.id] = take
      remaining -= take
    }
  }
  materials.value = chosen
}
async function craft() {
  result.value = await production.craft(
    selected.value,
    quantity.value,
    tool.value,
    toolCount.value,
    Object.fromEntries(Object.entries(materials.value).filter(([, count]) => count > 0)),
  )
  if (result.value) materials.value = {}
}
</script>
<template>
  <LifeActivityPage
    activity="production"
    :pending="production.query.isPending.value"
    :errors="[production.error.value, production.query.error.value?.message]"
    :result="resultMessage"
  >
    <label class="form-field"
      >制造配方<select v-model="selected" :disabled="production.busy.value" @change="reset">
        <option value="" disabled>选择配方</option>
        <option v-for="item in production.query.data.value?.recipes" :key="item.id" :value="item.id">
          {{ item.name }} · 技能{{ item.level }}级 · 产出{{ item.quantity }}件
        </option>
      </select></label
    >
    <p v-if="production.query.data.value?.recipes.length === 0">学习工匠、皮匠、制药或烹饪后可查看配方。</p>
    <template v-if="recipe">
      <p v-if="recipe.blueprintName">特殊图纸：{{ recipe.blueprintName }}。需要随身携带，制造不消耗。</p>
      <p>制造地点：{{ recipe.locations.join('、') }}。要求技能{{ recipe.level }}级。</p>
      <p>
        当前专业{{ recipe.skillLevel }}级。需要{{ toolTiers[recipe.minimumToolTier]
        }}{{ toolNames[recipe.skill] }}或更高档工具。
      </p>
      <p v-if="recipe.proficiencyGain !== null && recipe.proficiencyGain > 0">
        下一件成功制造获得{{ recipe.proficiencyGain }}熟练度。批量制造中升级后，后续每件按新等级计算收益。
      </p>
      <AppNotice v-if="recipe.proficiencyGain === 0" tone="warning">
        {{ recipe.skillLevel === 10 ? '该专业已满级' : '配方低于当前专业等级至少两级' }}，制造不会增加熟练度。
      </AppNotice>
      <p v-else-if="recipe.skillLevel - recipe.level === 1">配方低于当前专业一级，熟练度收益减半。</p>
      <AppNotice v-if="!recipe.available" tone="warning">当前地点、专业熟练度或特殊图纸不满足要求。</AppNotice>
      <ul aria-label="本批材料汇总" aria-live="polite">
        <li v-for="ingredient in materialTotals" :key="ingredient.code">
          {{ ingredient.name }}：本批需要{{ ingredient.needed }}，已选{{ ingredient.chosen }}
          <span>，库存{{ ingredient.owned }}（未绑定{{ ingredient.unbound }}）</span>
          <span v-if="ingredient.chosen < ingredient.needed">，还差{{ ingredient.needed - ingredient.chosen }}</span>
        </li>
      </ul>
      <label class="form-field"
        >制造次数<input
          v-model.number="quantity"
          :disabled="production.busy.value"
          type="number"
          min="1"
          max="99"
          inputmode="numeric"
      /></label>
      <label class="form-field"
        >制造工具<select v-model="tool" :disabled="production.busy.value" aria-label="制造工具">
          <option value="" disabled>选择工具</option>
          <option v-for="item in tools" :key="item.id" :value="item.id">
            {{ item.name }} × {{ item.quantity }} · {{ item.bound ? '绑定' : '未绑定' }}
          </option>
        </select></label
      >
      <label class="form-field"
        >允许使用的工具总数（含备用）<input
          v-model.number="toolCount"
          :disabled="production.busy.value"
          type="number"
          min="1"
          max="99"
          inputmode="numeric"
      /></label>
      <p>逐项选择本批允许消耗的材料数量。使用绑定材料会绑定成品。</p>
      <p>按本批需求填入会替换当前材料选择；不足部分仍会显示缺口。填入后可逐项调整，再确认制造。</p>
      <IonButton fill="outline" :disabled="production.busy.value" @click="fillMaterials(false)"
        >填入未绑定材料</IonButton
      >
      <IonButton fill="outline" :disabled="production.busy.value" @click="fillMaterials(true)"
        >填入材料（允许绑定）</IonButton
      >
      <IonButton fill="clear" :disabled="production.busy.value" @click="materials = {}">清空材料选择</IonButton>
      <AppNotice v-if="usesBoundMaterials" tone="warning">已选择绑定材料，使用这些材料制造的成品将绑定。</AppNotice>
      <p>精制装备会消耗所选旧装备；已穿戴或强化过的装备不能作为材料。</p>
      <details :key="selected" :open="ingredients.length <= 12">
        <summary>逐项调整材料（{{ ingredients.length }}批）</summary>
        <label v-for="item in ingredients" :key="item.id" class="form-field"
          >{{ item.name }} · {{ item.bound ? '绑定' : '未绑定' }} · 持有{{ item.quantity
          }}<input
            v-model.number="materials[item.id]"
            :disabled="production.busy.value"
            type="number"
            min="0"
            :max="item.quantity"
            placeholder="0"
            inputmode="numeric"
        /></label>
      </details>
      <IonButton
        :disabled="
          production.busy.value || !recipe.available || !tool || !Object.values(materials).some((value) => value > 0)
        "
        @click="craft"
        >确认制造</IonButton
      >
    </template>
    <h2>生活用品</h2>
    <h3>出售生活材料与成品</h3>
    <p>工具、药剂和制造装备也可回收；装备须未穿戴、未强化。</p>
    <label class="form-field"
      >出售物品<select v-model="sellItem" :disabled="production.busy.value">
        <option value="" disabled>选择物品</option>
        <option v-for="item in sellable" :key="item.id" :value="item.id">
          {{ item.name }} × {{ item.quantity }} · {{ item.bound ? '绑定' : '未绑定' }}
        </option>
      </select></label
    >
    <label class="form-field"
      >出售数量<input
        v-model.number="sellQuantity"
        :disabled="production.busy.value"
        type="number"
        min="1"
        max="99"
        inputmode="numeric"
    /></label>
    <IonButton
      :disabled="
        production.busy.value ||
        !production.query.data.value?.canSell ||
        !sellItem ||
        sellQuantity < 1 ||
        sellQuantity > 99
      "
      @click="production.sell(sellItem, sellQuantity)"
      >确认出售 · {{ sellPrice * sellQuantity }}金币</IonButton
    >
    <p>晨露村补给店和诺姆城商店出售木工具与种子；购买不占专业名额。</p>
    <section v-for="offer in production.query.data.value?.offers" :key="offer.item">
      <IonButton
        fill="outline"
        :disabled="production.busy.value || !offer.available"
        @click="production.buy(offer.item, 1)"
      >
        购买{{ offer.name }} · {{ offer.price }}金币
      </IonButton>
    </section>
  </LifeActivityPage>
</template>
