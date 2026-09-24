<script setup lang="ts">
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useGathering} from '@/features/life/gathering'
const gathering = useGathering(usePageActive())
const supplies = ref<Record<string, number>>({})
const selectedSupplies = computed(() =>
  Object.fromEntries(Object.entries(supplies.value).filter(([, count]) => count > 0)),
)
const nodeId = ref(''),
  pet = ref(''),
  tool = ref(''),
  count = ref(1)
const node = computed(() => gathering.query.data.value?.nodes.find((value) => value.id === nodeId.value))
const tools = computed(
  () =>
    gathering.query.data.value?.inventory.filter((item) =>
      gathering.query.data.value?.tools.some(
        (value) =>
          value.item === item.code &&
          value.kind === node.value?.toolKind &&
          value.tier >= Math.floor(((node.value?.level ?? 1) - 1) / 2),
      ),
    ) ?? [],
)
const labels: Record<string, string> = {
  ACTIVE: '正在采集',
  PAUSED: '已暂停',
  TOOLS_EXHAUSTED: '选定工具已耗尽，请结束派遣后重新选择',
  RESOURCES_LOW: '伙伴生命或魔法不足，已暂停',
  BAG_FULL: '背包已满，整理后继续',
}
</script>
<template>
  <AppPage title="野外采集" back="/life">
    <p>资源不限次数。保持页面在线前台才推进，离页或断线暂停；结束派遣后才能移动或战斗。</p>
    <p>
      铁档及以上资源有机会额外获得稀有材料，概率随熟练度从5%提升至最多8%。请为普通和稀有产物都留出背包空间，空间不足会暂停且不消耗资源。
    </p>
    <AppNotice
      v-if="gathering.error.value || gathering.heartbeatError.value || gathering.query.error.value"
      tone="danger"
      >{{ gathering.error.value || gathering.heartbeatError.value || gathering.query.error.value?.message }}</AppNotice
    >
    <section v-if="gathering.session.value">
      <h2>{{ labels[gathering.session.value.status] }}</h2>
      <p v-for="(remaining, id) in gathering.session.value.supplies" :key="id">
        {{ gathering.query.data.value?.supplies.find((item) => item.id === id)?.name ?? '所选补给' }}：剩余额度
        {{ remaining }}
      </p>
      <p>
        完成 {{ gathering.session.value.completed }} 轮 · 选定工具剩余 {{ gathering.session.value.toolsRemaining }} 把
      </p>
      <progress
        aria-label="本轮采集进度"
        :value="gathering.session.value.progressMillis"
        :max="gathering.session.value.roundMillis"
      />
      <p>
        本轮 {{ (gathering.session.value.progressMillis / 1000).toFixed(1) }} /
        {{ gathering.session.value.roundMillis / 1000 }} 秒
      </p>
      <IonButton :disabled="gathering.busy.value" @click="gathering.stop">结束派遣</IonButton>
    </section>
    <template v-else>
      <p v-if="gathering.query.data.value?.nodes.length === 0">当前地点没有采集资源，请前往野外。</p>
      <label class="form-field"
        >资源点<select v-model="nodeId" aria-label="资源点" @change="tool = ''">
          <option value="" disabled>选择资源点</option>
          <option v-for="item in gathering.query.data.value?.nodes" :key="item.id" :value="item.id">
            {{ item.name }} · {{ item.skillName }}{{ item.level }}级
          </option>
        </select></label
      >
      <template v-if="node">
        <p>
          每轮获得{{ node.product }} × {{ node.quantity }}，消耗伙伴{{ node.health }}生命、{{
            node.mana
          }}魔法，至少保留1点生命。
        </p>
        <AppNotice v-if="!node.available" tone="warning">需要学习对应专业并达到要求的技能等级。</AppNotice>
        <label class="form-field"
          >派遣伙伴<select v-model="pet" aria-label="派遣伙伴">
            <option value="" disabled>选择随身伙伴</option>
            <option v-for="item in gathering.query.data.value?.creatures" :key="item.id" :value="item.id">
              {{ item.name }} · {{ item.level }}级
            </option>
          </select></label
        >
        <label class="form-field"
          >采集工具<select v-model="tool" aria-label="采集工具">
            <option value="" disabled>选择工具</option>
            <option v-for="item in tools" :key="item.id" :value="item.id">{{ item.name }} × {{ item.quantity }}</option>
          </select></label
        >
        <label class="form-field"
          >使用工具总数（含备用）<input v-model.number="count" type="number" min="1" max="99" inputmode="numeric"
        /></label>
        <p>自动补药默认关闭。最多选择两种药，并填写本次派遣允许消耗的数量；只在本轮资源不足时使用。</p>
        <p>恢复量超过伙伴生命或魔法上限的部分会损失。低等级伙伴使用高阶药可能浪费药效，请按需选择。</p>
        <label v-for="item in gathering.query.data.value?.supplies" :key="item.id" class="form-field">
          {{ item.name }} · {{ item.bound ? '绑定' : '未绑定' }} · 持有{{ item.quantity }}
          <input
            v-model.number="supplies[item.id]"
            type="number"
            min="0"
            :max="Math.min(99, item.quantity)"
            placeholder="0"
            inputmode="numeric"
          />
        </label>
        <IonButton
          :disabled="
            gathering.busy.value || !node.available || !pet || !tool || Object.keys(selectedSupplies).length > 2
          "
          @click="gathering.start(nodeId, pet, tool, count, selectedSupplies)"
          >开始采集</IonButton
        >
      </template>
    </template>
  </AppPage>
</template>
