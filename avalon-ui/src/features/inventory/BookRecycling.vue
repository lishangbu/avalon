<script setup lang="ts">
/** 确认时固定报价和数量，网络重试沿用凭证；关闭后重选才生成新的交易。 */
import {computed, ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {usePageActive} from '@/shared/usePageActive'

interface Offer {
  code: string
  name: string
  unitPrice: number
  total: number
  unbound: number
  maximum: number
}
const active = usePageActive(),
  client = useQueryClient(),
  action = useAction(true)
const query = useQuery({
  queryKey: ['inventory', 'book-recycling'],
  enabled: active,
  queryFn: () => api<{available: boolean; offers: Offer[]}>('/api/inventory/books/recycling'),
})
const quantities = ref<Record<string, number>>({}),
  open = ref(false)
const pending = ref<{requestId: string; quantity: number; expected: Offer}>()
const gold = computed(() => (pending.value ? pending.value.quantity * pending.value.expected.unitPrice : 0))
function valid(offer: Offer) {
  const quantity = quantities.value[offer.code] ?? 1
  return Number.isInteger(quantity) && quantity >= 1 && quantity <= offer.maximum
}
function confirm(offer: Offer) {
  if (!valid(offer)) return
  pending.value = {requestId: crypto.randomUUID(), quantity: quantities.value[offer.code] ?? 1, expected: {...offer}}
  action.error.value = ''
  open.value = true
}
async function sell() {
  if (!pending.value) return
  if (
    await action.run(async () => {
      await api('/api/inventory/books/recycling', 'POST', pending.value)
      await client.invalidateQueries()
    })
  )
    open.value = false
}
</script>
<template>
  <AppNotice v-if="query.error.value" tone="danger">{{ query.error.value.message }}</AppNotice>
  <section v-if="query.data.value?.available">
    <h2>技能书回收</h2>
    <article v-for="offer in query.data.value.offers" :key="offer.code" class="row">
      <strong>{{ offer.name }}</strong>
      <p>每本 {{ offer.unitPrice }} 金币 · 可出售 {{ offer.maximum }} 本</p>
      <label class="form-field"
        >{{ offer.name }}回收数量
        <input
          v-model.number="quantities[offer.code]"
          type="number"
          min="1"
          :max="offer.maximum"
          step="1"
          placeholder="1"
          :disabled="action.busy.value"
        />
      </label>
      <IonButton :disabled="action.busy.value || !valid(offer)" fill="outline" @click="confirm(offer)"
        >出售 {{ offer.name }}</IonButton
      >
    </article>
    <p v-if="!query.data.value.offers.length" class="empty">暂无可回收的重复技能书</p>
  </section>
  <AppSheet v-model="open" title="确认回收技能书" :busy="action.busy.value">
    <template v-if="pending">
      <h2>{{ pending.expected.name }} ×{{ pending.quantity }}</h2>
      <p>获得 {{ gold }} 金币</p>
      <p>出售后同名书剩余 {{ pending.expected.total - pending.quantity }} 本</p>
    </template>
    <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
    <template #footer>
      <IonButton fill="outline" :disabled="action.busy.value" @click="open = false">取消</IonButton>
      <IonButton :disabled="action.busy.value" @click="sell">确认出售</IonButton>
    </template>
  </AppSheet>
</template>
