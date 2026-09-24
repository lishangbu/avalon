<script setup lang="ts">
/** 单次强化确认页面：属性、概率、材料与绑定结果均采用服务端预览，不在客户端重算。 */
import {computed, nextTick, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useInventory} from '@/features/inventory/queries'
import {useEnhancementAccess, useEnhancementActions} from '@/features/inventory/enhancement'

const active = usePageActive(),
  access = useEnhancementAccess(active),
  action = useEnhancementActions()
const enabled = computed(() => active.value && !!access.data.value?.available)
const inventory = useInventory(enabled)
const equipment = computed(() => inventory.data.value?.filter((item) => item.slot) ?? [])
const selected = ref(''),
  mode = ref<'' | 'UNBOUND_ONLY' | 'BOUND_FIRST'>(''),
  open = ref(false)
const names: Record<string, string> = {
  health: '生命',
  mana: '魔力',
  physicalAttack: '物攻',
  magicAttack: '法攻',
  physicalDefense: '物防',
  magicDefense: '法防',
  speed: '速度',
}
async function inspect() {
  if (await action.inspect(selected.value, mode.value)) open.value = true
}
async function submit() {
  if (await action.submit()) {
    // 先把提交结束状态传给Ionic，确保canDismiss更新后再关闭确认弹层。
    await nextTick()
    open.value = false
  }
}
</script>
<template>
  <AppPage title="装备强化" back="/world">
    <AppNotice v-if="action.error.value || access.error.value || inventory.error.value" tone="danger">{{
      action.error.value || access.error.value?.message || inventory.error.value?.message
    }}</AppNotice>
    <p v-if="access.isPending.value">正在查询强化资格…</p>
    <AppNotice v-else-if="!access.data.value?.available"
      >请达到{{ access.data.value?.requiredLevel ?? 30 }}级后前往城镇强化设施。</AppNotice
    >
    <template v-else>
      <AppNotice
        >最高强化 +7。每次独立判定，没有保底；失败降低一级，最低 +0，金币与材料照常消耗，装备不会损坏。</AppNotice
      >
      <label class="form-field"
        >装备<select v-model="selected" :disabled="action.busy.value">
          <option value="">选择装备</option>
          <option v-for="item in equipment" :key="item.id" :value="item.id" :disabled="item.enhancementLevel >= 7">
            {{ item.name }} +{{ item.enhancementLevel }} · {{ item.equipped ? '已穿戴' : '背包' }} ·
            {{ item.bound ? '已绑定' : '未绑定' }}
          </option>
        </select></label
      >
      <label class="form-field"
        >材料使用方式<select v-model="mode" :disabled="action.busy.value">
          <option value="">按装备绑定状态选择</option>
          <option value="UNBOUND_ONLY">仅使用未绑定材料</option>
          <option value="BOUND_FIRST">优先使用绑定材料</option>
        </select></label
      >
      <p>未绑定装备默认只用未绑定材料；已绑定装备默认绑定材料优先。使用绑定强化石，失败也会绑定装备。</p>
      <IonButton :disabled="!selected || action.busy.value" @click="inspect">查看强化预览</IonButton>
    </template>
    <AppNotice v-if="action.result.value" :tone="action.result.value.success ? 'success' : 'warning'">
      {{ action.result.value.success ? '强化成功' : '强化失败，消耗已扣除' }}：{{ action.result.value.item.name }} +{{
        action.result.value.item.enhancementLevel
      }}，{{ action.result.value.item.bound ? '已绑定' : '未绑定' }}。
    </AppNotice>
    <AppSheet v-model="open" title="确认单次强化" :busy="action.busy.value">
      <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
      <template v-if="action.preview.value">
        <h2>
          {{ action.preview.value.before.name }} +{{ action.preview.value.cost.current }} → +{{
            action.preview.value.cost.target
          }}
        </h2>
        <p>
          成功率 {{ action.preview.value.cost.successPercent }}%；失败变为 +{{
            action.preview.value.cost.failureLevel
          }}。
        </p>
        <p>
          消耗 {{ action.preview.value.cost.gold }} 金币及{{
            action.preview.value.cost.stone === 'ENHANCEMENT_STONE_I' ? '强化石I' : '强化石II'
          }}
          ×{{ action.preview.value.cost.quantity }}，成功或失败均扣除。
        </p>
        <AppNotice :tone="action.preview.value.materials.bindsTarget ? 'warning' : 'info'"
          >结果{{ action.preview.value.resultBound ? '已绑定' : '不额外绑定' }}。{{
            action.preview.value.materials.bindsTarget ? '本次包含绑定材料，失败也会绑定。' : ''
          }}</AppNotice
        >
        <div v-for="(value, key) in action.preview.value.before.stats" :key="key" class="row">
          <strong>{{ names[key] ?? key }}</strong>
          <p>
            当前 {{ value }} · 成功 {{ action.preview.value.successStats[key] }} · 失败
            {{ action.preview.value.failureStats[key] }}
          </p>
        </div>
        <p>强化不恢复生命或魔力；穿戴装备掉级后，超过新上限的资源将截断。回收价格不变，不支持强化转移。</p>
      </template>
      <template #footer
        ><IonButton :disabled="action.busy.value || !action.preview.value" @click="submit"
          >确认强化一次</IonButton
        ></template
      >
    </AppSheet>
  </AppPage>
</template>
