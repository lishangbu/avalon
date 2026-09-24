<script lang="ts" setup>
/** 合成提交精确的来源实例与数量；绑定规则和消耗数量均由服务器再次检查。 */
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useCrafting} from '@/features/inventory/crafting'

const active = usePageActive(),
  crafting = useCrafting(active)
const selected = ref(''),
  quantities = ref<Record<string, number>>({})
const recipe = computed(() => crafting.query.data.value?.recipes.find((item) => item.id === selected.value))
const total = computed(() =>
  Object.values(quantities.value).reduce((sum, value) => sum + (Number.isFinite(value) ? value : 0), 0),
)
// 预览按玩家选择的来源计算；混入任意绑定碎片就会得到绑定成品。
const bound = computed(
  () => recipe.value?.fragments.some((fragment) => fragment.bound && (quantities.value[fragment.id] ?? 0) > 0) ?? false,
)
const valid = computed(
  () =>
    !!recipe.value &&
    total.value === recipe.value.quantity &&
    Object.entries(quantities.value).every(([id, count]) => {
      const fragment = recipe.value?.fragments.find((item) => item.id === id)
      return !!fragment && Number.isInteger(count) && count >= 0 && count <= fragment.quantity
    }),
)

function select(id: string) {
  selected.value = id
  quantities.value = {}
}

async function submit() {
  if (
    valid.value &&
    !crafting.busy.value &&
    recipe.value &&
    (await crafting.craft(
      recipe.value.id,
      Object.fromEntries(Object.entries(quantities.value).filter(([, count]) => count > 0)),
    ))
  )
    quantities.value = {}
}
</script>
<template>
  <AppPage back="/inventory" title="碎片合成">
    <AppNotice v-if="crafting.error.value || crafting.query.error.value" tone="danger">
      {{ crafting.error.value || crafting.query.error.value?.message }}
    </AppNotice>
    <AppNotice v-if="crafting.result.value" tone="success"
      >获得 {{ crafting.result.value.name }} ·
      {{ crafting.result.value.bound ? '已绑定' : '未绑定' }}
    </AppNotice>
    <label class="form-field" for="recipe"
      >配方<select
        id="recipe"
        :disabled="crafting.busy.value"
        :value="selected"
        @change="select(($event.target as HTMLSelectElement).value)"
      >
        <option disabled value="">选择配方</option>
        <option v-for="item in crafting.query.data.value?.recipes" :key="item.id" :value="item.id">
          {{ item.product }}
        </option>
      </select></label
    >
    <template v-if="recipe"
      ><p>消耗 {{ recipe.quantity }} 个碎片、{{ recipe.gold }} 金币</p>
      <label v-for="fragment in recipe.fragments" :key="fragment.id" :for="'fragment-' + fragment.id" class="form-field"
        >{{ fragment.name }} · {{ fragment.bound ? '已绑定' : '未绑定' }} · 持有 {{ fragment.quantity
        }}<input
          :id="'fragment-' + fragment.id"
          v-model.number="quantities[fragment.id]"
          :disabled="crafting.busy.value"
          :max="fragment.quantity"
          inputmode="numeric"
          min="0"
          placeholder="0"
          step="1"
          type="number"
      /></label>
      <p>已选 {{ total }} / {{ recipe.quantity }}</p>
      <AppNotice v-if="total > 0" :tone="bound ? 'warning' : 'info'"
        >成品：{{ bound ? '已绑定' : '未绑定' }}。</AppNotice
      >
    </template>
    <p v-if="crafting.query.data.value?.recipes.length === 0" class="empty">暂无可用配方</p>
    <template #footer>
      <IonButton :disabled="crafting.busy.value || !valid" @click="submit">合成</IonButton>
    </template>
  </AppPage>
</template>
