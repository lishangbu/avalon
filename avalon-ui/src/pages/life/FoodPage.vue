<script setup lang="ts">
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useFood, type FoodResult} from '@/features/life/food'
const food = useFood(usePageActive())
const item = ref(''),
  target = ref(''),
  result = ref<FoodResult | null>(null)
const selected = computed(() =>
  food.query.data.value?.foods.find(
    (value) => value.item === food.query.data.value?.inventory.find((held) => held.id === item.value)?.code,
  ),
)
async function eat() {
  result.value = await food.eat(item.value, target.value)
}
</script>
<template>
  <AppPage title="食用料理" back="/life">
    <p>战斗外为人物或随身伙伴恢复生命、魔法，无额外增益。无需学习烹饪即可食用。</p>
    <AppNotice v-if="food.error.value || food.query.error.value" tone="danger">{{
      food.error.value || food.query.error.value?.message
    }}</AppNotice>
    <AppNotice v-if="result" tone="info"
      >恢复生命{{ result.restoredHealth }}点、魔法{{ result.restoredMana }}点。</AppNotice
    >
    <p v-if="food.query.isPending.value">正在读取料理…</p>
    <p v-if="food.query.data.value?.inventory.length === 0">背包中没有料理，可通过烹饪或玩家交易获得。</p>
    <label class="form-field"
      >料理<select v-model="item" aria-label="料理" :disabled="food.busy.value">
        <option value="" disabled>选择料理</option>
        <option v-for="value in food.query.data.value?.inventory" :key="value.id" :value="value.id">
          {{ value.name }} × {{ value.quantity }} · {{ value.bound ? '绑定' : '未绑定' }}
        </option>
      </select></label
    >
    <p v-if="selected">每份最多恢复生命{{ selected.health }}点、魔法{{ selected.mana }}点，超出上限部分不保留。</p>
    <label class="form-field"
      >食用目标<select v-model="target" aria-label="食用目标" :disabled="food.busy.value">
        <option value="" disabled>选择人物或随身伙伴</option>
        <option v-for="value in food.query.data.value?.targets" :key="value.id" :value="value.id">
          {{ value.name }} · 生命{{ value.health }}/{{ value.maximumHealth }} · 魔法{{ value.mana }}/{{
            value.maximumMana
          }}
        </option>
      </select></label
    >
    <IonButton :disabled="food.busy.value || !item || !target" @click="eat">食用一份</IonButton>
    <IonButton router-link="/life/production" fill="outline">前往烹饪</IonButton>
  </AppPage>
</template>
