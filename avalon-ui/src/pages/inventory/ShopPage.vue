<script lang="ts" setup>
/** 商品和价格由当前位置授权查询提供，购买重试沿用原请求号，避免响应丢失后重复扣款。 */
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {usePageActive} from '@/shared/usePageActive'
import {useWorld} from '@/features/world/queries'
import {useBattleLobby} from '@/features/battle/queries'
import {useBattleActions} from '@/features/battle/actions'
import {equipmentSlots} from '@/features/inventory/labels'
import BookRecycling from '@/features/inventory/BookRecycling.vue'
import ContentLink from '@/features/content/ContentLink.vue'

const active = usePageActive(),
  client = useQueryClient()
const {data: world} = useWorld(),
  lobby = useBattleLobby(active),
  battle = useBattleActions()
const shop = useQuery({
  queryKey: ['equipment-shop'],
  enabled: active,
  queryFn: () =>
    api<{
      available: boolean
      products: {code: string; name: string; slot: string; level: number; quality: string; price: number}[]
    }>('/api/inventory/equipment/shop'),
})
const {busy, error, run} = useAction(true)
let pending: {requestId: string; operation: string; code: string; expected: null} | undefined

async function buy(code: string) {
  await run(async () => {
    if (!pending || pending.code !== code)
      pending = {
        requestId: crypto.randomUUID(),
        operation: 'BUY',
        code,
        expected: null,
      }
    await api('/api/inventory/equipment/commerce', 'POST', pending)
    pending = undefined
    await client.invalidateQueries()
  })
}
</script>
<template>
  <AppPage title="商店">
    <p>持有 {{ world?.character?.gold ?? 0 }} 金币</p>
    <AppNotice v-if="error || battle.error.value || shop.error.value || lobby.error.value" tone="danger">
      {{ error || battle.error.value || shop.error.value?.message || lobby.error.value?.message }}
    </AppNotice>
    <section v-if="lobby.data.value?.products.length">
      <h2>补给</h2>
      <article v-for="product in lobby.data.value.products" :key="product.code" class="row">
        <ContentLink kind="item" :code="product.code" :name="product.name" />
        <div class="actions">
          <IonButton :disabled="busy || battle.busy.value" @click="battle.purchase(product.code)"
            >购买 · {{ product.price }} 金币
          </IonButton>
        </div>
      </article>
    </section>
    <section v-if="shop.data.value?.available">
      <h2>装备</h2>
      <article v-for="product in shop.data.value.products" :key="product.code" class="row">
        <ContentLink kind="item" :code="product.code" :name="product.name" />
        <p>{{ product.quality }} · {{ equipmentSlots[product.slot] }} · Lv.{{ product.level }}</p>
        <IonButton :disabled="busy || battle.busy.value" @click="buy(product.code)"
          >购买 · {{ product.price }} 金币
        </IonButton>
      </article>
      <p v-if="!shop.data.value.products.length">暂无可购买装备。</p>
    </section>
    <BookRecycling />
    <p
      v-if="
        !shop.isPending.value &&
        !lobby.isPending.value &&
        !shop.data.value?.available &&
        !lobby.data.value?.products.length
      "
      class="empty"
    >
      暂无可购买商品。
    </p>
  </AppPage>
</template>
