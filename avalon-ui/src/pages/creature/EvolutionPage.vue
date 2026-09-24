<script setup lang="ts">
/** 研究所办理页：教学与进化分开确认，换路线或材料模式后必须重新取得预览。 */
import {computed, ref, watch} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useCreature} from '@/features/creature/queries'
import {useEvolutionResearch, useEvolutionActions, type MaterialMode} from '@/features/creature/evolution'

const active = usePageActive(),
  research = useEvolutionResearch(active),
  pets = useCreature(active)
const action = useEvolutionActions()
const selected = ref(''),
  petId = ref(''),
  mode = ref<MaterialMode | ''>(''),
  accepted = ref(false)
const route = computed(() => research.data.value?.routes.find((route) => route.source === selected.value))
const candidates = computed(
  () =>
    pets.data.value?.filter((pet) => research.data.value?.routes.some((route) => route.source === pet.species)) ?? [],
)
const evolutionRoute = computed(() =>
  research.data.value?.routes.find((route) => route.target === action.preview.value?.target),
)
const exchangeCode = ref(''),
  exchangeQuantity = ref(1),
  exchangeAccepted = ref(false)
const exchangeOffer = computed(() => research.data.value?.offers.find((offer) => offer.code === exchangeCode.value))
const exchangeCost = computed(() => (exchangeOffer.value?.price ?? 0) * exchangeQuantity.value)
const canExchange = computed(
  () =>
    !!exchangeOffer.value &&
    Number.isSafeInteger(exchangeQuantity.value) &&
    exchangeQuantity.value > 0 &&
    Number.isSafeInteger(exchangeCost.value) &&
    exchangeCost.value <= (research.data.value?.tokens ?? 0) &&
    exchangeAccepted.value,
)
watch([exchangeCode, exchangeQuantity], () => {
  exchangeAccepted.value = false
})
watch([petId, mode], () => {
  action.preview.value = null
})
watch(selected, () => {
  accepted.value = false
})
</script>
<template>
  <AppPage title="进化研究所" back="/world">
    <AppNotice v-if="action.error.value || research.error.value || pets.error.value" tone="danger">
      {{ action.error.value || research.error.value?.message || pets.error.value?.message }}
    </AppNotice>
    <p v-if="research.isPending.value">正在查询研究服务…</p>
    <AppNotice v-else-if="!research.data.value?.available">当前地点或等级尚不能办理进化研究。</AppNotice>
    <template v-else>
      <AppNotice v-if="action.result.value" tone="success"
        >{{ action.result.value.name }}进化完成。生命与魔力不会因进化恢复。</AppNotice
      >
      <template v-if="!research.data.value.taught">
        <h2>学习进化</h2>
        <p>选择一条路线，领取一套绑定进化材料。每名角色只能领取一次，达到伙伴等级要求后才能进化。</p>
        <label class="form-field"
          >教学路线
          <select v-model="selected" :disabled="action.busy.value">
            <option value="">选择路线</option>
            <option v-for="item in research.data.value.routes" :key="item.source" :value="item.source">
              {{ item.name }} · 伙伴 {{ item.level }} 级
            </option>
          </select>
        </label>
        <p v-for="material in route?.materials" :key="material.code">
          {{ material.name }} ×{{ material.quantity }} · 已绑定
        </p>
        <label class="row"
          ><input v-model="accepted" type="checkbox" :disabled="action.busy.value" />
          我已了解：使用这些材料进化会绑定伙伴，不能交易。</label
        >
        <IonButton :disabled="!selected || !accepted || action.busy.value" @click="action.teach(selected)"
          >确认领取教学材料</IonButton
        >
      </template>
      <template v-else>
        <template v-if="research.data.value.offers.length">
          <h2>研究凭证兑换</h2>
          <p>可用研究凭证：{{ research.data.value.tokens }} 枚。兑换物品均绑定，不能交易。</p>
          <AppNotice v-if="action.exchangeMessage.value" tone="success">{{ action.exchangeMessage.value }}</AppNotice>
          <label class="form-field"
            >兑换项目
            <select v-model="exchangeCode" :disabled="action.busy.value">
              <option value="">选择材料或技能书</option>
              <option v-for="offer in research.data.value.offers" :key="offer.code" :value="offer.code">
                {{ offer.name }} · {{ offer.price }} 枚/份
              </option>
            </select>
          </label>
          <label class="form-field"
            >兑换数量<input
              v-model.number="exchangeQuantity"
              type="number"
              min="1"
              step="1"
              inputmode="numeric"
              :disabled="action.busy.value"
          /></label>
          <p v-if="exchangeOffer && Number.isSafeInteger(exchangeCost) && exchangeQuantity > 0">
            共消耗 {{ exchangeCost }} 枚研究凭证。
          </p>
          <label class="row"
            ><input v-model="exchangeAccepted" type="checkbox" :disabled="action.busy.value" />
            确认兑换绑定物品；使用绑定进化材料会绑定伙伴。</label
          >
          <IonButton
            :disabled="!canExchange || action.busy.value"
            @click="exchangeOffer && action.exchange(exchangeOffer, exchangeQuantity)"
            >确认兑换</IonButton
          >
        </template>
        <AppNotice v-else>这里可以为伙伴进化。材料兑换请前往研究设施办理。</AppNotice>
        <h2>伙伴进化</h2>
        <p>进化不可撤销。保留个体、槽位、技能和自定义昵称，不自动学习新技能。</p>
        <label class="form-field"
          >选择伙伴
          <select v-model="petId" :disabled="action.busy.value">
            <option value="">选择伙伴</option>
            <option v-for="pet in candidates" :key="pet.id" :value="pet.id">{{ pet.name }} · {{ pet.level }} 级</option>
          </select>
        </label>
        <label class="form-field"
          >材料使用方式
          <select v-model="mode" :disabled="action.busy.value">
            <option value="">按伙伴绑定状态选择默认方式</option>
            <option value="UNBOUND_ONLY">仅未绑定材料</option>
            <option value="BOUND_FIRST">优先使用绑定材料</option>
          </select>
        </label>
        <IonButton :disabled="!petId || action.busy.value" fill="outline" @click="action.prepare(petId, mode)"
          >查看进化方案</IonButton
        >
        <section v-if="action.preview.value">
          <h2>{{ action.preview.value.before.name }} → {{ evolutionRoute?.name }}</h2>
          <p v-for="material in evolutionRoute?.materials" :key="material.code">
            消耗 {{ material.name }} ×{{ material.quantity }}
          </p>
          <AppNotice :tone="action.preview.value.resultBound ? 'warning' : 'info'"
            >进化后{{ action.preview.value.resultBound ? '已绑定，不能交易' : '保持未绑定' }}。{{
              action.preview.value.materials.bindsTarget ? '本次会消耗绑定材料。' : '本次仅消耗未绑定材料。'
            }}</AppNotice
          >
          <IonButton :disabled="action.busy.value" @click="action.evolve()">确认进化（不可撤销）</IonButton>
        </section>
      </template>
    </template>
  </AppPage>
</template>
