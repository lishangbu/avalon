<script lang="ts" setup>
/** 接收明确选择后的快照；弹层显示期间不跟随后台刷新替换名单，重试复用请求号。 */
import {computed, ref, watch} from 'vue'
import {IonButton} from '@ionic/vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import type {OwnedCreature} from './types'
import {usePlacement} from './actions'

const selected = defineModel<OwnedCreature[] | null>({required: true})
const emit = defineEmits<{released: []}>()
const {busy, error, release} = usePlacement()
const requestId = ref('')
watch(
  selected,
  (value) => {
    if (value) {
      requestId.value = crypto.randomUUID()
      error.value = ''
    }
  },
  {immediate: true},
)
const open = computed({
  get: () => selected.value !== null,
  set: (value) => {
    if (!value) selected.value = null
  },
})

async function confirm() {
  if (selected.value && (await release(requestId.value, selected.value))) {
    selected.value = null
    emit('released')
  }
}
</script>
<template>
  <AppSheet v-model="open" :busy="busy" title="确认放生">
    <AppNotice tone="warning">放生后无法找回，不返还封印球、经验、金币或技能书。</AppNotice>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <article v-for="pet in selected" :key="pet.id" class="row">
      <strong>{{ pet.name }} · Lv.{{ pet.level }} · {{ pet.element }}</strong>
      <p>{{ pet.ability }} · {{ pet.partyPosition >= 100 ? '仓库' : '携带' }}</p>
      <AppNotice v-if="pet.abilitySlot === 'H'" tone="warning">隐藏特性个体</AppNotice>
      <p>技能：{{ pet.skills.join('、') }}</p>
    </article>
    <template #footer>
      <IonButton :disabled="busy" fill="outline" @click="selected = null">取消</IonButton>
      <IonButton :disabled="busy" color="danger" @click="confirm">确认放生</IonButton>
    </template>
  </AppSheet>
</template>
