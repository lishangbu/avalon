<script setup lang="ts">
import {formatDateTime} from '@/shared/dateTime'
/** 地点服务页面，不改变地图位置；所有交易资格与最终容量仍由服务端复核。 */
import {computed, nextTick, ref, watch} from 'vue'
import {IonButton} from '@ionic/vue'
import {useQuery} from '@tanstack/vue-query'
import type {OwnedCreature} from '@/features/creature/types'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useInventory} from '@/features/inventory/queries'
import {
  useMarketAccess,
  useMarketPage,
  useMarketAction,
  quote,
  type Listing,
  type Detail,
} from '@/features/market/market'
import {api, HttpError} from '@/shared/http'

const active = usePageActive(),
  access = useMarketAccess(active),
  action = useMarketAction()
const enabled = computed(() => active.value && !!access.data.value?.available && !!access.data.value?.taught)
const inventory = useInventory(enabled)
const candidates = useQuery({
  queryKey: ['market', 'eligible-creatures'],
  enabled,
  queryFn: () => api<OwnedCreature[]>('/api/market/creatures/eligible'),
})
const sellKind = ref('ITEM'),
  petId = ref(''),
  hidden = ref(''),
  skill = ref('')
const sellPet = computed(() => candidates.data.value?.find((pet) => pet.id === petId.value))
const tab = ref('search'),
  page = ref(0),
  name = ref(''),
  category = ref('ALL'),
  sort = ref('NEWEST'),
  state = ref('ALL')
const slot = ref(''),
  quality = ref(''),
  minimum = ref(''),
  maximum = ref(''),
  enhancementLevel = ref('')
const path = computed(() => {
  const params = new URLSearchParams({page: String(page.value), size: '20'})
  if (tab.value === 'mine') params.set('state', state.value)
  else {
    params.set('name', name.value)
    params.set('category', category.value)
    params.set('sort', sort.value)
    if (slot.value) params.set('slot', slot.value)
    if (quality.value) params.set('quality', quality.value)
    if (minimum.value) params.set('minimum', minimum.value)
    if (maximum.value) params.set('maximum', maximum.value)
    if (enhancementLevel.value !== '') params.set('enhancementLevel', enhancementLevel.value)
    if (hidden.value) params.set('hidden', hidden.value)
    if (skill.value) params.set('skill', skill.value)
  }
  return `${tab.value === 'mine' ? 'mine' : 'search'}?${params}`
})
const listingEnabled = computed(() => enabled.value && tab.value !== 'sell')
const listings = useMarketPage(path, listingEnabled)
watch([tab, name, category, sort, state, slot, quality, minimum, maximum, hidden, skill, enhancementLevel], () => {
  page.value = 0
})
const teachingAccepted = ref(false),
  selected = ref<Listing>(),
  detail = ref<Detail>(),
  open = ref(false),
  quantity = ref(1)
const itemId = ref(''),
  sellQuantity = ref(1),
  price = ref('2'),
  sellOpen = ref(false)
const sellItem = computed(() => inventory.data.value?.find((item) => item.id === itemId.value))
const sellable = computed(() => inventory.data.value?.filter((item) => !item.bound && !item.equipped) ?? [])
const materialBatches = computed(() =>
  sellItem.value && !sellItem.value.slot
    ? sellable.value.filter((item) => item.code === sellItem.value?.code && !item.slot)
    : [],
)
const materialTotal = computed(() => materialBatches.value.reduce((sum, item) => sum + item.quantity, 0))
const saleOptions = computed(() => {
  const seen = new Set<string>()
  return sellable.value.filter((item) => {
    if (item.slot) return true
    if (seen.has(item.code)) return false
    seen.add(item.code)
    return true
  })
})
function saleQuantity(item: NonNullable<typeof sellItem.value>) {
  return item.slot
    ? 1
    : sellable.value
        .filter((batch) => batch.code === item.code && !batch.slot)
        .reduce((sum, batch) => sum + batch.quantity, 0)
}
const saleQuote = computed(() => quote(price.value, sellKind.value === 'CREATURE' ? 1 : sellQuantity.value))
// 确认框使用固定资料，后台刷新库存不能悄悄改变正在确认或重试的上架请求。
const confirmation = ref<{
  endpoint: string
  body: Record<string, unknown>
  name: string
  quantity: number
  price: string
  quote: NonNullable<ReturnType<typeof quote>>
}>()
watch(sellKind, () => {
  sellQuantity.value = 1
})
watch(category, () => {
  enhancementLevel.value = ''
  slot.value = ''
  quality.value = ''
  hidden.value = ''
  skill.value = ''
})
const buyQuote = computed(() => (selected.value ? quote(selected.value.unitPrice, quantity.value) : null))
const validQuantity = computed(
  () =>
    Number.isSafeInteger(quantity.value) && quantity.value > 0 && quantity.value <= (selected.value?.remaining ?? 0),
)
const states: Record<string, string> = {
  ACTIVE: '出售中',
  RETURN_PENDING: '退货待领',
  SOLD: '已售完',
  RETURNED: '已领回',
}
const statNames: Record<string, string> = {
  health: '生命',
  mana: '魔力',
  physicalAttack: '物攻',
  magicAttack: '法攻',
  physicalDefense: '物防',
  magicDefense: '法防',
  speed: '速度',
}

async function inspect(listing: Listing) {
  detail.value = undefined
  selected.value = listing
  quantity.value = 1
  open.value = true
  if (listing.state === 'ACTIVE' || listing.state === 'RETURN_PENDING') {
    await action.run(async () => {
      detail.value = await api<Detail>(`/api/market/listings/${listing.id}`)
      selected.value = detail.value.listing
    })
  }
}
async function trade(endpoint: string) {
  if (!selected.value || action.busy.value) return
  const body: Record<string, unknown> = {listingId: selected.value.id}
  if (endpoint !== 'cancel') body.quantity = quantity.value
  if (endpoint === 'purchase') body.unitPrice = selected.value.unitPrice
  if (await action.submit(endpoint, body)) {
    await nextTick()
    open.value = false
  } else if (endpoint === 'purchase') {
    // 详情打开后可能被别人买走；保留本次失败原因，同时复核剩余量与购买资格。
    const message = action.error.value
    const id = selected.value.id
    await action.run(async () => {
      try {
        detail.value = await api<Detail>(`/api/market/listings/${id}`)
        selected.value = detail.value.listing
      } catch (cause) {
        if (!(cause instanceof HttpError) || cause.status !== 404) throw cause
        detail.value = undefined
        selected.value = undefined
      }
    })
    action.error.value = message || action.error.value
  }
}
async function sell() {
  if (!confirmation.value) return
  if (await action.submit(confirmation.value.endpoint, confirmation.value.body)) {
    await nextTick()
    sellOpen.value = false
  }
}
function confirmSale() {
  if (!saleQuote.value) return
  const pet = sellKind.value === 'CREATURE'
  const expected = pet ? sellPet.value : sellItem.value
  if (!expected) return
  const batch = !pet && !sellItem.value?.slot
  let remaining = sellQuantity.value
  const selections = batch
    ? materialBatches.value.flatMap((item) => {
        const count = Math.min(remaining, item.quantity)
        remaining -= count
        return count > 0 ? [{itemId: item.id, quantity: count, expected: item}] : []
      })
    : []
  if (batch && remaining !== 0) return
  confirmation.value = {
    endpoint: pet ? 'creatures' : batch ? 'items/batch' : 'items',
    name: expected.name,
    quantity: pet ? 1 : sellQuantity.value,
    price: price.value,
    quote: saleQuote.value,
    body: JSON.parse(
      JSON.stringify(
        pet
          ? {creatureId: expected.id, unitPrice: price.value, expected}
          : batch
            ? {items: selections, unitPrice: price.value}
            : {itemId: expected.id, quantity: sellQuantity.value, unitPrice: price.value, expected},
      ),
    ),
  }
  sellOpen.value = true
}
</script>
<template>
  <AppPage title="交易所" back="/world">
    <AppNotice
      v-if="action.error.value || access.error.value || listings.error.value || candidates.error.value"
      tone="danger"
      >{{
        action.error.value ||
        access.error.value?.message ||
        listings.error.value?.message ||
        candidates.error.value?.message
      }}</AppNotice
    >
    <AppNotice v-if="action.message.value" tone="success">{{ action.message.value }}</AppNotice>
    <p v-if="access.isPending.value">正在查询交易资格…</p>
    <AppNotice v-else-if="!access.data.value?.available">请达到开放等级后前往城镇交易所。</AppNotice>
    <section v-else-if="!access.data.value.taught">
      <h2>交易须知</h2>
      <p>每角色最多20个挂单，每单48小时。上架和取消免费，每次成交收取5%税，向上取整；分次成交可能增加总税。</p>
      <p>
        上架后实物托管。取消或到期进入退货待领，未领完前不能新上架。物品需要背包空间，伙伴直接进入仓库，购买不额外绑定。
      </p>
      <label class="row"><input v-model="teachingAccepted" type="checkbox" /> 我已了解交易规则</label>
      <IonButton :disabled="!teachingAccepted || action.busy.value" @click="action.submit('teaching', {accepted: true})"
        >开始使用交易所</IonButton
      >
    </section>
    <template v-else>
      <p>出售中 {{ access.data.value.activeListings }}/20</p>
      <AppNotice v-if="access.data.value.pendingReturns" tone="warning"
        >有退货尚未领完，暂不能新上架；仍可浏览和购买。</AppNotice
      >
      <label class="form-field"
        >交易功能<select v-model="tab" :disabled="action.busy.value">
          <option value="search">购买商品</option>
          <option value="mine">我的挂单与退货</option>
          <option value="sell">上架商品</option>
        </select></label
      >
      <template v-if="tab === 'sell'">
        <label class="form-field"
          >商品种类<select v-model="sellKind">
            <option value="ITEM">物品</option>
            <option value="CREATURE">伙伴</option>
          </select></label
        >
        <template v-if="sellKind === 'CREATURE'">
          <label class="form-field"
            >伙伴<select v-model="petId">
              <option value="">选择可交易伙伴</option>
              <option v-for="pet in candidates.data.value" :key="pet.id" :value="pet.id">
                {{ pet.name }} · Lv.{{ pet.level }} · {{ pet.abilitySlot === 'H' ? '隐藏' : '普通' }}特性
              </option>
            </select></label
          >
          <p>仅显示未绑定、未锁定且不在出战或替补位置的普通捕获伙伴。初始与剧情伙伴不能交易。</p>
          <p v-if="sellPet">特性：{{ sellPet.ability }}；技能：{{ sellPet.skills.join('、') }}</p>
        </template>
        <label v-else class="form-field"
          >物品<select v-model="itemId">
            <option value="">选择未绑定物品</option>
            <option v-for="item in saleOptions" :key="item.id" :value="item.id">
              {{ item.name }} ×{{ saleQuantity(item) }}
            </option>
          </select></label
        >
        <p v-if="sellKind === 'ITEM' && materialBatches.length > 1">
          该物品共有{{ materialTotal }}件可出售，可合并为一张挂单。输入本次要出售的总数量。
        </p>
        <label v-if="sellKind === 'ITEM'" class="form-field"
          >数量<input v-model.number="sellQuantity" type="number" min="1" :max="sellItem?.slot ? 1 : materialTotal"
        /></label>
        <label class="form-field">单价（金币）<input v-model="price" inputmode="numeric" /></label>
        <IonButton
          :disabled="
            !saleQuote ||
            (sellKind === 'CREATURE'
              ? !sellPet
              : !sellItem || sellQuantity > saleQuantity(sellItem) || (!!sellItem.slot && sellQuantity !== 1)) ||
            access.data.value.pendingReturns ||
            action.busy.value
          "
          @click="confirmSale"
          >查看上架确认</IonButton
        >
      </template>
      <template v-else>
        <template v-if="tab === 'search'">
          <label class="form-field">名称<input v-model="name" maxlength="100" /></label>
          <label class="form-field"
            >类别<select v-model="category">
              <option value="ALL">全部商品</option>
              <option value="CREATURE">伙伴</option>
              <option value="EQUIPMENT">装备</option>
              <option value="BOOK">技能书</option>
              <option value="FRAGMENT">碎片</option>
              <option value="MATERIAL">材料与补给</option>
            </select></label
          >
          <details v-if="category !== 'CREATURE'">
            <summary>装备筛选</summary>
            <label class="form-field"
              >强化等级<select v-model="enhancementLevel">
                <option value="">不限</option>
                <option v-for="level in 8" :key="level" :value="String(level - 1)">+{{ level - 1 }}</option>
              </select></label
            >
            <label class="form-field"
              >部位<select v-model="slot">
                <option value="">不限</option>
                <option
                  v-for="(label, code) in {
                    WEAPON: '武器',
                    HEAD: '头部',
                    BODY: '上衣',
                    HANDS: '手部',
                    LEGS: '腿部',
                    FEET: '鞋子',
                    ACCESSORY: '饰品',
                  }"
                  :key="code"
                  :value="code"
                >
                  {{ label }}
                </option>
              </select></label
            >
            <label class="form-field"
              >品质<select v-model="quality">
                <option value="">不限</option>
                <option v-for="value in ['普通', '优秀', '精良', '史诗', '传说']" :key="value">{{ value }}</option>
              </select></label
            >
          </details>
          <template v-if="category === 'CREATURE'">
            <p>名称按物种名搜索，不按玩家昵称搜索。</p>
            <label class="form-field"
              >特性类别<select v-model="hidden">
                <option value="">不限</option>
                <option value="false">普通</option>
                <option value="true">隐藏</option>
              </select></label
            >
            <label class="form-field">已学技能（完整名称）<input v-model="skill" maxlength="100" /></label>
          </template>
          <label class="form-field">最低等级<input v-model="minimum" type="number" min="0" /></label>
          <label class="form-field">最高等级<input v-model="maximum" type="number" min="0" /></label>
          <label class="form-field"
            >排序<select v-model="sort">
              <option value="NEWEST">最新上架</option>
              <option value="PRICE_ASC">价格从低到高</option>
              <option value="PRICE_DESC">价格从高到低</option>
            </select></label
          >
        </template>
        <label v-else class="form-field"
          >状态<select v-model="state">
            <option value="ALL">全部</option>
            <option v-for="(label, value) in states" :key="value" :value="value">{{ label }}</option>
          </select></label
        >
        <p v-if="listings.isFetching.value">正在查询…</p>
        <p v-else-if="!listings.data.value?.listings.length">暂无挂单。</p>
        <div v-for="listing in listings.data.value?.listings" :key="listing.id" class="row">
          <strong
            >{{ listing.name }}<span v-if="listing.enhancementLevel != null"> +{{ listing.enhancementLevel }}</span> ×{{
              listing.remaining
            }}</strong
          >
          <p>{{ listing.unitPrice }} 金币/份 · {{ states[listing.state] }}</p>
          <IonButton fill="outline" :disabled="action.busy.value" @click="inspect(listing)">查看详情</IonButton>
        </div>
        <div class="row-title">
          <IonButton :disabled="page === 0 || action.busy.value" @click="page--">上一页</IonButton
          ><span>第 {{ page + 1 }} 页</span
          ><IonButton :disabled="!listings.data.value?.hasMore || action.busy.value" @click="page++">下一页</IonButton>
        </div>
      </template>
    </template>
    <AppSheet v-model="open" title="商品详情" :busy="action.busy.value">
      <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
      <template v-if="selected"
        ><h2>{{ selected.name }}</h2>
        <p>{{ states[selected.state] }} · 剩余 {{ selected.remaining }} · 单价 {{ selected.unitPrice }} 金币</p>
        <p>到期：{{ formatDateTime(selected.expiresAt) }}</p></template
      >
      <template v-if="detail">
        <template v-if="detail.item">
          <p>{{ detail.item.quality }} · {{ detail.item.bound ? '已绑定' : '未绑定，购买不额外绑定' }}</p>
          <p v-if="detail.item.slot">
            穿戴等级 {{ detail.item.requiredLevel }} · {{ detail.item.requiredProfession ?? '不限职业' }}；穿戴后绑定。
            强化 +{{ detail.item.enhancementLevel }}。
          </p>
          <p v-for="(value, stat) in detail.item.stats" :key="stat">{{ statNames[stat] ?? stat }}：{{ value }}</p>
          <p v-for="set in detail.sets" :key="set.name">
            {{ set.name }}：{{ set.healthPieces }}件增加{{ set.health }}生命；{{
              set.shieldPieces
            }}件提供随等级成长的防御护盾。
            <span v-if="set.weapons.length"
              >五件防具配合同档武器，额外增加{{ set.weaponHealth }}生命与{{ set.weaponShield }}防御护盾。</span
            >
          </p>
        </template>
        <template v-if="detail.creature">
          <p>昵称：{{ detail.creature.name }} · 物种：{{ detail.creature.speciesName }}</p>
          <p>
            Lv.{{ detail.creature.level }} · 累计经验 {{ detail.creature.experience }} ·
            {{ detail.creature.element }}属性
          </p>
          <p>{{ detail.creature.abilitySlot === 'H' ? '隐藏' : '普通' }}特性：{{ detail.creature.ability }}</p>
          <p>已学技能：{{ detail.creature.skills.join('、') }}</p>
          <p>
            原捕获者：{{ detail.creature.captorName ?? '未知'
            }}<span v-if="detail.creature.captorId">（{{ detail.creature.captorId }}）</span>
          </p>
          <p>
            生命 {{ detail.creature.resources.hp }}/{{ detail.creature.resources.health }} · 魔力
            {{ detail.creature.resources.mp }}/{{ detail.creature.resources.mana }}
          </p>
          <p>
            物攻 {{ detail.creature.resources.physicalAttack }} · 法攻 {{ detail.creature.resources.magicAttack }} ·
            物防 {{ detail.creature.resources.physicalDefense }} · 法防 {{ detail.creature.resources.magicDefense }} ·
            速度 {{ detail.creature.resources.speed }}
          </p>
          <AppNotice
            >未绑定。成交或退货直接入仓，不治疗、不复活；高于人物等级的伙伴可购买，但不能编入出战或替补。</AppNotice
          >
        </template>
        <label
          v-if="selected?.kind === 'ITEM' && (!detail.own || selected?.state === 'RETURN_PENDING')"
          class="form-field"
          >数量<input v-model.number="quantity" type="number" min="1" :max="selected?.remaining"
        /></label>
        <p v-if="!detail.own && buyQuote">
          合计 {{ buyQuote.gross }} 金币。{{ detail.creature ? '伙伴仓库' : '背包' }}空间不足则整笔失败。
        </p>
      </template>
      <template #footer>
        <IonButton
          v-if="detail && !detail.own && selected?.state === 'ACTIVE'"
          :disabled="!validQuantity || !buyQuote || action.busy.value"
          @click="trade('purchase')"
          >确认购买</IonButton
        >
        <IonButton
          v-if="detail?.own && selected?.state === 'ACTIVE'"
          :disabled="action.busy.value"
          @click="trade('cancel')"
          >取消挂单并转待领</IonButton
        >
        <IonButton
          v-if="detail?.own && selected?.state === 'RETURN_PENDING'"
          :disabled="!validQuantity || action.busy.value"
          @click="trade('returns/claim')"
          >领取退货</IonButton
        >
      </template>
    </AppSheet>
    <AppSheet v-model="sellOpen" title="确认上架" :busy="action.busy.value">
      <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
      <p>{{ confirmation?.name }} ×{{ confirmation?.quantity }}，单价 {{ confirmation?.price }} 金币，48小时后到期。</p>
      <p>
        整单成交税 {{ confirmation?.quote.tax }} 金币，税后收入
        {{ confirmation?.quote.net }} 金币。分次成交向上取整，实际总税可能更高。
      </p>
      <p>上架立即托管，不能再使用。取消或到期后需要领取退货。</p>
      <template #footer
        ><IonButton :disabled="action.busy.value || !confirmation" @click="sell">确认上架</IonButton></template
      >
    </AppSheet>
  </AppPage>
</template>
