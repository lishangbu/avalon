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
const purchaseMode = ref<'quantity' | 'target'>('quantity')
const currentShopOnly = ref(true)
const purchaseSearch = ref('')
const purchaseAmounts = ref<Record<string, number>>({})
const purchaseOffers = computed(() =>
  (production.query.data.value?.offers ?? [])
    .filter((offer) => !currentShopOnly.value || offer.available)
    .filter((offer) => `${offer.name} ${offer.locations.join(' ')}`.includes(purchaseSearch.value.trim()))
    .map((offer) => {
      const owned = (production.query.data.value?.inventory ?? [])
        .filter((item) => item.code === offer.item)
        .reduce((sum, item) => sum + item.quantity, 0)
      const amount = purchaseAmounts.value[offer.item] ?? 1
      const valid = Number.isInteger(amount) && amount >= 1 && amount <= 99
      const count = valid ? (purchaseMode.value === 'target' ? Math.max(0, amount - owned) : amount) : 0
      return {...offer, owned, amount, valid, count, total: count * offer.price}
    }),
)
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
const toolPurchaseSources = computed(() => {
  const view = production.query.data.value,
    selectedRecipe = recipe.value
  if (!view || !selectedRecipe) return []
  const eligible = new Set(
    view.tools
      .filter(
        (definition) =>
          definition.kind === toolKinds[selectedRecipe.skill] && definition.tier >= selectedRecipe.minimumToolTier,
      )
      .map((definition) => definition.item),
  )
  return view.offers.filter((offer) => eligible.has(offer.item))
})
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
const supplyUnboundOnly = ref(false)
const supplyShortages = computed(() =>
  materialTotals.value
    .map((item) => ({
      ...item,
      missing: Math.max(0, item.needed - (supplyUnboundOnly.value ? item.unbound : item.owned)),
      offer: production.query.data.value?.offers.find((offer) => offer.item === item.code),
    }))
    .filter((item) => item.missing > 0),
)
const supplyPurchaseBudget = computed(() =>
  supplyShortages.value.reduce((total, item) => total + item.missing * (item.offer?.price ?? 0), 0),
)
const batchCapacity = computed(() => ({
  unbound: Math.min(99, ...materialTotals.value.map((item) => Math.floor(item.unbound / item.quantity))),
  all: Math.min(99, ...materialTotals.value.map((item) => Math.floor(item.owned / item.quantity))),
}))
const validQuantity = computed(() => Number.isInteger(quantity.value) && quantity.value >= 1 && quantity.value <= 99)
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
function prepareBatch(includeBound: boolean) {
  if (production.busy.value || !recipe.value) return
  const capacity = includeBound ? batchCapacity.value.all : batchCapacity.value.unbound
  if (capacity < 1) return
  quantity.value = capacity
  fillMaterials(includeBound)
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
      <AppNotice v-if="tools.length === 0" tone="warning"> 随身没有符合档位的制造工具，请先准备工具与备用。 </AppNotice>
      <details v-if="toolPurchaseSources.length">
        <summary>查看合格工具的采购来源</summary>
        <ul>
          <li v-for="offer in toolPurchaseSources" :key="offer.item">
            {{ offer.name }}：单价{{ offer.price }}金币，出售地点{{ offer.locations.join('、') }}。{{
              offer.available ? '当前地点可购买。' : '需前往出售地点。'
            }}
          </li>
        </ul>
        <p>工具可能报废，请按计划准备备用；工具费用不包含在材料采购预算内。</p>
      </details>
      <p v-if="recipe.proficiencyGain !== null && recipe.proficiencyGain > 0">
        下一件成功制造获得{{ recipe.proficiencyGain }}熟练度。批量制造中升级后，后续每件按新等级计算收益。
      </p>
      <AppNotice v-if="recipe.proficiencyGain === 0" tone="warning">
        {{ recipe.skillLevel === 10 ? '该专业已满级' : '配方低于当前专业等级至少两级' }}，制造不会增加熟练度。
      </AppNotice>
      <p v-else-if="recipe.skillLevel - recipe.level === 1">配方低于当前专业一级，熟练度收益减半。</p>
      <AppNotice v-if="!recipe.available" tone="warning">当前地点、专业熟练度或特殊图纸不满足要求。</AppNotice>
      <section aria-label="本批供料清单">
        <h2>本批供料清单</h2>
        <label><input v-model="supplyUnboundOnly" type="checkbox" />仅使用未绑定库存计算缺额</label>
        <p>按随身可用材料计算，不计仓库与待领取。清单只帮助备料，不会自动购买、交易或选择绑定材料。</p>
        <template v-if="validQuantity">
          <ul v-if="supplyShortages.length" aria-label="待补材料" aria-live="polite">
            <li v-for="item in supplyShortages" :key="item.code">
              {{ item.name }}：需补{{ item.missing }}件
              <p v-if="item.offer">
                城市采购：{{ item.offer.locations.join('、') }}，单价{{ item.offer.price }}金币，补齐需{{
                  item.offer.price * item.missing
                }}金币。{{ item.offer.available ? '当前地点可购买。' : '需前往出售地点。' }}
              </p>
              <p v-else>目录未提供直接采购来源，可展开下方材料来源查看采集或制造途径。</p>
            </li>
          </ul>
          <p v-else>当前库存足够本批材料，无需额外供料。</p>
          <p v-if="supplyPurchaseBudget > 0" aria-label="材料采购预算">
            可直接采购的缺料合计{{
              supplyPurchaseBudget
            }}金币，不含工具、补给及无法直接购买的材料。单次最多购买99件，较大缺额需分批准备。
          </p>
        </template>
        <p v-else>输入有效制造次数后查看供料缺额。</p>
      </section>
      <ul aria-label="本批材料汇总" aria-live="polite">
        <li v-for="ingredient in materialTotals" :key="ingredient.code">
          {{ ingredient.name }}：本批需要{{ ingredient.needed }}，已选{{ ingredient.chosen }}
          <span>，库存{{ ingredient.owned }}（未绑定{{ ingredient.unbound }}）</span>
          <template v-if="validQuantity">
            <span v-if="ingredient.owned < ingredient.needed"
              >，库存缺少{{ ingredient.needed - ingredient.owned }}</span
            >
            <span v-if="ingredient.chosen < ingredient.needed"
              >，尚需选入{{ ingredient.needed - ingredient.chosen }}</span
            >
          </template>
          <!-- 来源随材料展示，折叠内容不执行地图移动，也不会自动购买或消耗材料。 -->
          <details>
            <summary>查看{{ ingredient.name }}的获取来源</summary>
            <ul>
              <li v-for="source in ingredient.sources" :key="source">{{ source }}</li>
            </ul>
            <p>以上为生活系统直接来源。实际获取仍需满足地图、专业、工具及图纸要求。</p>
          </details>
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
      <p>
        按材料库存最多可制造{{ batchCapacity.unbound }}次（仅未绑定），允许绑定材料时为{{
          batchCapacity.all
        }}次，每批上限99次。
      </p>
      <p>实际完成次数还受工具损耗与背包空间影响。</p>
      <IonButton
        fill="outline"
        :disabled="production.busy.value || batchCapacity.unbound < 1"
        @click="prepareBatch(false)"
        >按未绑定库存备料</IonButton
      >
      <IonButton fill="outline" :disabled="production.busy.value || batchCapacity.all < 1" @click="prepareBatch(true)"
        >按全部库存备料（允许绑定）</IonButton
      >
      <AppNotice v-if="!validQuantity" tone="warning">制造次数须为1至99的整数。</AppNotice>
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
          production.busy.value ||
          !validQuantity ||
          !recipe.available ||
          !tool ||
          !Object.values(materials).some((value) => value > 0)
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
    <h3>采购生活用品</h3>
    <p>
      基础矿石、木材等原料可用于主线交付；购买不需要学习对应采集专业，也不占专业名额。各城市另有不同档位的工具和生产辅料，查看全部商品可查询出售地点。
    </p>
    <label><input v-model="currentShopOnly" type="checkbox" />仅显示当前地点出售的商品</label>
    <label class="form-field"
      >商品搜索<input v-model="purchaseSearch" type="search" placeholder="商品名称或出售地点"
    /></label>
    <p v-if="production.query.data.value && purchaseOffers.length === 0">
      当前筛选没有商品。可查看全部商品的出售地点，到达对应地点后采购。
    </p>
    <label class="form-field"
      >采购方式<select v-model="purchaseMode" :disabled="production.busy.value">
        <option value="quantity">按数量购买</option>
        <option value="target">按目标库存补齐</option>
      </select></label
    >
    <p>每次最多购买99件。目标库存计入绑定与未绑定的随身物品，不计仓库和待领取物品。</p>
    <section v-for="offer in purchaseOffers" :key="offer.item" :aria-label="`${offer.name}采购`">
      <h4>{{ offer.name }}</h4>
      <p>出售地点：{{ offer.locations.join('、') }}。{{ offer.available ? '当前地点可购买。' : '需前往出售地点。' }}</p>
      <p>随身库存{{ offer.owned }}件，单价{{ offer.price }}金币。</p>
      <label class="form-field"
        >{{ purchaseMode === 'target' ? '目标库存' : '购买数量'
        }}<input
          v-model.number="purchaseAmounts[offer.item]"
          :placeholder="'1'"
          :disabled="production.busy.value"
          type="number"
          min="1"
          max="99"
          inputmode="numeric"
      /></label>
      <p v-if="!offer.valid" role="alert">请输入1至99的整数。</p>
      <p v-else-if="purchaseMode === 'target' && offer.count === 0">库存已达到目标，无需购买。</p>
      <p v-else>本次购买{{ offer.count }}件，合计{{ offer.total }}金币。</p>
      <IonButton
        fill="outline"
        :disabled="production.busy.value || !offer.available || !offer.valid || offer.count === 0"
        @click="production.buy(offer.item, offer.count)"
      >
        {{ purchaseMode === 'target' ? '补齐' : '购买' }}{{ offer.name }} · {{ offer.total }}金币
      </IonButton>
    </section>
  </LifeActivityPage>
</template>
