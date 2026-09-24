<script lang="ts" setup>
/** 背包分页只做投影展示。交易确认复制完整实例，服务器检查快照和幂等请求号。 */
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useQueryClient} from '@tanstack/vue-query'
import {IonButton, IonIcon} from '@ionic/vue'
import {chevronForwardOutline} from 'ionicons/icons'
import AppPage from '@/shared/ui/AppPage.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useBag, useLoadout} from '@/features/inventory/queries'
import {useEquipment} from '@/features/inventory/actions'
import {usePendingItems} from '@/features/inventory/pending'
import {useChests} from '@/features/inventory/chests'
import {equipmentSlots, statNames} from '@/features/inventory/labels'
import type {BagView, OwnedItem} from '@/features/inventory/types'
import {useAction} from '@/shared/useAction'
import {api} from '@/shared/http'
import {usePageCategory} from '@/app/pageState'
import ContentDetails from '@/features/content/ContentDetails.vue'
import ContentLink from '@/features/content/ContentLink.vue'

const route = useRoute(),
  router = useRouter(),
  client = useQueryClient(),
  active = usePageActive()
const tabs = [
  {id: 'items', name: '物品'},
  {id: 'equipment', name: '装备'},
  {id: 'tasks', name: '任务'},
  {
    id: 'pending',
    name: '待领',
  },
]
const tab = usePageCategory('inventory', 'items')
watch(
  () => route.query.tab,
  (value) => {
    if (typeof value === 'string' && tabs.some((item) => item.id === value)) tab.value = value
  },
  {immediate: true},
)
const bagQuery = useBag(active),
  bag = bagQuery.data,
  equipment = useEquipment()
const pending = usePendingItems(computed(() => active.value && tab.value === 'pending'))
const chests = useChests(active)
const loadout = useLoadout(computed(() => active.value && tab.value === 'equipment'))
const professionNames: Record<string, string> = {WARRIOR: '战士', MAGE: '法师', PRIEST: '牧师', RANGER: '游侠'}
const quantities = ref<Record<string, number>>({})
const selected = ref<OwnedItem>(),
  detailOpen = ref(false),
  confirmOpen = ref(false)
const selectedStackKey = ref<string>(),
  stackOpen = ref(false)
// 堆叠详情随背包投影更新，避免领取或恢复后仍展示打开时的旧数量。
const selectedStack = computed(() => bag.value?.stacks.find((stack) => stack.key === selectedStackKey.value))

function inspectStack(stack: BagView['stacks'][number]) {
  selectedStackKey.value = stack.key
  stackOpen.value = true
}

const trade = useAction(true)
let tradeCompleted = false
const transaction = ref<{requestId: string; operation: 'SELL' | 'DESTROY'; code: null; expected: OwnedItem}>()
const busy = computed(() => equipment.busy.value || trade.busy.value || pending.busy.value || chests.busy.value)
const error = computed(
  () =>
    equipment.error.value ||
    trade.error.value ||
    pending.error.value ||
    chests.error.value ||
    chests.query.error.value?.message ||
    bagQuery.error.value?.message ||
    loadout.error.value?.message ||
    pending.query.error.value?.message,
)
const equipped = computed(() => bag.value?.equipment.filter((item) => item.equipped) ?? [])
const inventoryEquipment = computed(() => bag.value?.equipment.filter((item) => !item.equipped) ?? [])

function inspect(item: OwnedItem) {
  selected.value = item
  detailOpen.value = true
}

function confirm(operation: 'SELL' | 'DESTROY') {
  if (!selected.value) return
  transaction.value = {
    requestId: crypto.randomUUID(),
    operation,
    code: null,
    expected: JSON.parse(JSON.stringify(selected.value)),
  }
  confirmOpen.value = true
}

async function executeTrade() {
  if (!transaction.value) return
  if (
    await trade.run(async () => {
      await api('/api/inventory/equipment/commerce', 'POST', transaction.value)
      await client.invalidateQueries()
    })
  ) {
    tradeCompleted = true
    confirmOpen.value = false
  }
}

// 先让确认层完成 Ionic 的焦点和可访问性恢复，再关闭装备详情，避免两层同时退出互相覆盖状态。
function finishTrade() {
  if (!tradeCompleted) return
  tradeCompleted = false
  detailOpen.value = false
  transaction.value = undefined
}

async function change() {
  if (!selected.value) return
  if (await (selected.value.equipped ? equipment.unequip(selected.value.id) : equipment.equip(selected.value.id)))
    detailOpen.value = false
}
</script>
<template>
  <AppPage :scroll-key="`inventory:${tab}`" tab="/inventory" title="背包">
    <div aria-label="背包分类" class="segments">
      <button v-for="item in tabs" :key="item.id" :aria-pressed="tab === item.id" @click="tab = item.id">
        {{ item.name }}
      </button>
    </div>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <p v-if="bag">{{ bag.usedSlots }} / {{ bag.capacity }} 格</p>
    <template v-if="tab === 'items'">
      <article v-for="chest in chests.query.data.value" :key="chest.id" class="row">
        <div class="row-title">
          <strong>{{ chest.name }}</strong
          ><span>已绑定</span>
        </div>
        <IonButton :disabled="busy" @click="chests.claim(chest.id)">打开宝箱</IonButton>
      </article>
      <IonButton expand="block" fill="outline" @click="router.push('/crafting')">碎片合成</IonButton>
      <article v-for="stack in bag?.stacks" :key="stack.key" class="row">
        <button class="stack-link" @click="inspectStack(stack)">
          <strong>{{ stack.name }}</strong
          ><span>×{{ stack.quantity }}</span>
          <IonIcon :icon="chevronForwardOutline" />
        </button>
        <p>{{ stack.category }} · {{ stack.bound ? '已绑定' : '未绑定' }}</p>
      </article>
      <p v-if="bag && !bag.stacks.length && !chests.query.data.value?.length" class="empty">暂无物品</p>
    </template>
    <template v-else-if="tab === 'equipment'">
      <h2>已穿戴</h2>
      <div v-for="(name, slot) in equipmentSlots" :key="slot" class="row">
        <div class="row-title">
          <span>{{ name }}</span>
          <button
            v-if="equipped.find((item) => item.slot === slot)"
            class="item-link"
            @click="inspect(equipped.find((item) => item.slot === slot)!)"
          >
            {{ equipped.find((item) => item.slot === slot)?.name }}
            <IonIcon :icon="chevronForwardOutline" />
          </button>
          <small v-else>未装备</small>
        </div>
      </div>
      <section v-for="set in loadout.data.value?.sets" :key="set.id" class="set-effects">
        <h2>{{ set.name }}套装 · {{ set.count }}/{{ set.total }}</h2>
        <p>
          {{ set.healthPieces }} 件 · 生命上限 +{{ set.health }} ·
          {{ set.count >= set.healthPieces ? '已激活' : '未激活' }}
        </p>
        <p>{{ set.shieldPieces }} 件 · {{ set.count >= set.shieldPieces ? '已激活' : '未激活' }}</p>
        <p>人物防御时，为己方生命比例最低的出战伙伴提供 {{ set.shield }} 点护盾，持续至本回合结束。</p>
        <p v-if="set.hasWeaponBonus">
          五件防具与配套武器：额外生命 +{{ set.weaponHealth }}、防御护盾 +{{ set.weaponShield }} ·
          {{ set.weaponActive ? '已激活' : '未激活' }}
        </p>
      </section>
      <h2>库存装备</h2>
      <button v-for="item in inventoryEquipment" :key="item.id" class="equipment-row" @click="inspect(item)">
        <span
          ><strong>{{ item.name }}</strong
          ><small
            >{{ item.quality }} · Lv.{{ item.requiredLevel }} · 强化 +{{ item.enhancementLevel }} ·
            {{ item.bound ? '已绑定' : '未绑定' }}</small
          ></span
        >
        <IonIcon :icon="chevronForwardOutline" />
      </button>
      <p v-if="!inventoryEquipment.length" class="empty">暂无库存装备</p>
    </template>
    <template v-else-if="tab === 'tasks'">
      <article v-for="item in bag?.tasks" :key="item.id" class="row">
        <div class="row-title">
          <strong>{{ item.name }}</strong
          ><span>×{{ item.quantity }}</span>
        </div>
        <p>任务专用 · 不占背包格</p>
        <ContentLink kind="item" :code="item.code" name="查看用途" />
      </article>
      <p v-if="bag && !bag.tasks.length" class="empty">暂无任务物品</p></template
    >
    <template v-else>
      <article v-for="item in pending.query.data.value" :key="item.id" class="row">
        <strong>{{ item.name }} ×{{ item.quantity }}</strong>
        <ContentLink kind="item" :code="item.code" name="查看用途" />
        <p>{{ item.bound ? '已绑定' : '未绑定' }} · {{ item.source }}</p>
        <label :for="'claim-' + item.id" class="form-field"
          >领取数量<input
            :id="'claim-' + item.id"
            v-model.number="quantities[item.id]"
            :max="item.quantity"
            :placeholder="String(item.quantity)"
            min="1"
            type="number"
        /></label>
        <IonButton :disabled="busy" @click="pending.claim(item.id, quantities[item.id] ?? item.quantity)"
          >领取
        </IonButton>
      </article>
      <p v-if="pending.query.data.value?.length === 0" class="empty">暂无待领物品</p>
    </template>
    <AppSheet v-model="stackOpen" :title="selectedStack?.name ?? '物品'">
      <template v-if="selectedStack">
        <ContentDetails kind="item" :code="selectedStack.code" />
        <div class="row row-title">
          <span>分类</span><strong>{{ selectedStack.category }}</strong>
        </div>
        <div class="row row-title">
          <span>数量</span><strong>{{ selectedStack.quantity }}</strong>
        </div>
        <p>{{ selectedStack.bound ? '已绑定' : '未绑定' }}</p></template
      >
      <p v-else>该堆叠已不在背包中。</p>
      <template #footer>
        <IonButton @click="stackOpen = false">关闭</IonButton>
      </template>
    </AppSheet>
    <AppSheet v-model="detailOpen" :busy="busy || confirmOpen" :title="selected?.name ?? '装备'">
      <template v-if="selected"
        ><p>
          {{ selected.quality }} · {{ selected.bound ? '已绑定' : '未绑定' }} · 需求 Lv.{{ selected.requiredLevel }}
          <span v-if="selected.slot"> · 强化 +{{ selected.enhancementLevel }}</span>
        </p>
        <p v-if="selected.requiredProfession">
          限{{ professionNames[selected.requiredProfession] ?? selected.requiredProfession }}使用
        </p>
        <ContentDetails kind="item" :code="selected.code" />
        <div v-for="(value, key) in selected.stats" :key="key" class="row-title row">
          <span>{{ statNames[key] ?? key }}</span
          ><strong>+{{ value }}</strong>
        </div>
        <p>{{ selected.source }}</p>
        <p>
          {{ selected.replacementPrice ? `补购价 ${selected.replacementPrice} 金币` : '不可补购' }} ·
          {{ selected.sellPrice ? `回收价 ${selected.sellPrice} 金币` : '不可出售' }}
        </p></template
      >
      <AppNotice v-if="equipment.error.value" tone="danger">{{ equipment.error.value }}</AppNotice>
      <template #footer>
        <IonButton :disabled="busy" @click="change">{{ selected?.equipped ? '卸下' : '穿戴' }}</IonButton>
        <IonButton
          v-if="selected && !selected.equipped && selected.sellPrice > 0"
          :disabled="busy"
          fill="outline"
          @click="confirm('SELL')"
          >出售
        </IonButton>
        <IonButton
          v-if="selected && !selected.equipped"
          :disabled="busy"
          color="danger"
          fill="clear"
          @click="confirm('DESTROY')"
          >销毁
        </IonButton>
      </template>
    </AppSheet>
    <AppSheet
      v-model="confirmOpen"
      :busy="trade.busy.value"
      :title="transaction?.operation === 'SELL' ? '确认出售' : '确认销毁'"
      @dismissed="finishTrade"
    >
      <template v-if="transaction"
        ><h2>{{ transaction.expected.name }}</h2>
        <p>
          {{ transaction.expected.quality }} · Lv.{{ transaction.expected.requiredLevel }} · 强化 +{{
            transaction.expected.enhancementLevel
          }}
          ·
          {{ transaction.expected.bound ? '已绑定' : '未绑定' }}
        </p>
        <p>
          {{
            transaction.expected.replacementPrice ? `补购价 ${transaction.expected.replacementPrice} 金币` : '不可补购'
          }}
        </p>
        <AppNotice tone="warning"
          >{{
            transaction.operation === 'SELL'
              ? `出售获得 ${transaction.expected.sellPrice} 金币。`
              : '销毁后无法恢复，不获得金币。'
          }}
        </AppNotice>
      </template>
      <AppNotice v-if="trade.error.value" tone="danger">{{ trade.error.value }}</AppNotice>
      <template #footer>
        <IonButton :disabled="trade.busy.value" fill="outline" @click="confirmOpen = false">取消</IonButton>
        <IonButton :disabled="trade.busy.value" color="danger" @click="executeTrade">
          {{ transaction?.operation === 'SELL' ? '确认出售' : '确认销毁' }}
        </IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
<style scoped>
.item-link {
  border: 0;
  background: transparent;
  color: var(--ion-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  max-width: 75%;
  text-align: right;
}

.equipment-row {
  width: 100%;
  background: transparent;
  color: var(--ion-text-color);
  border: 0;
  border-bottom: 1px solid var(--game-border);
  padding: 16px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.equipment-row small {
  display: block;
  margin-top: 6px;
}

.stack-link {
  width: 100%;
  min-height: 44px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ion-text-color);
  text-align: left;
}

.stack-link strong {
  overflow-wrap: anywhere;
}

ion-icon {
  flex-shrink: 0;
}
</style>
