<script setup lang="ts">
/** 非战斗实践课只展示隔离投影，所有操作都提交会话版本，不调用正式背包或伙伴写接口。 */
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useWorkshop, useWorkshopActions} from '@/features/academy/workshop'

const query = useWorkshop(usePageActive())
const command = useWorkshopActions()
const router = useRouter()
const exitOpen = ref(false)
const page = ref(0)
const view = computed(() => query.data.value?.active ?? command.result.value)
const busy = computed(() => command.busy.value || Boolean(command.pending.value))
const items = computed(() => view.value?.items.slice(page.value * 8, page.value * 8 + 8) ?? [])
const pages = computed(() => Math.max(1, Math.ceil((view.value?.items.length ?? 0) / 8)))
const title = computed(() => (view.value?.course === 'BQ021' ? '技能学习实践' : '装备与仓库实践'))
const labels: Record<string, string> = {
  VIEW_POOL: '查看技能池',
  INSPECT_EQUIPMENT: '比较训练装备',
  RESET_VITALS: '请教员重置训练生命',
  UNEQUIP: '卸下训练装备',
  START_BAG: '进入背包与仓库练习',
  USE_HEALTH: '使用训练生命药',
  CLAIM_MANA: '领取待领魔力药',
}
const progressLabels: Record<string, string> = {
  POOL_VIEWED: '已查看物种技能池',
  LEARNED: '已完成一次真实学习',
  EQUIPMENT_VIEWED: '已比较装备需求与属性',
  EQUIPPED_B: '已穿戴外衣，当前资源未增加',
  TEACHER_RESET: '教员已重置临时生命与魔力',
  UNEQUIP_CLAMPED: '已卸装并观察资源截断',
  POTION_USED: '已使用训练药品腾出空间',
  PENDING_CLAIMED: '已领取待领物品',
  PET_STORED: '已将芽灵存仓',
  PET_WITHDRAWN: '已取回同一只芽灵',
  PET_LOCKED: '已锁定保护芽灵',
}

function allowed(type: string) {
  return view.value?.actions.includes(type) ?? false
}
async function operate(type: string, target?: string, position?: number) {
  if (!view.value) return
  if (await command.operate(view.value, type, target, position)) {
    page.value = 0
    if (type === 'EXIT') {
      exitOpen.value = false
      await router.replace('/world')
    }
  }
}
async function retry() {
  if (await command.send()) {
    page.value = 0
    if (command.result.value?.status === 'EXITED') await router.replace('/world')
  }
}
</script>

<template>
  <AppPage :title="title" locked>
    <AppNotice v-if="command.error.value || query.error.value" tone="danger">{{
      command.error.value || query.error.value?.message
    }}</AppNotice>
    <div v-if="command.pending.value">
      <p>操作结果尚未确认，重试不会重新消耗训练物品。</p>
      <IonButton :disabled="command.busy.value" @click="retry">重试原操作</IonButton>
    </div>
    <p v-if="!view">正在读取课程…</p>
    <template v-else>
      <p>本页全部伙伴和物品为教学借用，退出后回收。</p>
      <AppNotice>{{ view.message }}</AppNotice>
      <p>Lv.{{ view.level }} · 生命 {{ view.hp }}/{{ view.maxHp }} · 魔力 {{ view.mp }}/{{ view.maxMp }}</p>
      <section v-if="view.skills.length">
        <h2>借用伙伴技能</h2>
        <p>{{ view.skills.join('、') }}</p>
        <p v-if="view.skillPool.length">本物种技能池：{{ view.skillPool.join('、') }}</p>
        <details v-if="view.forgetProbabilities.length">
          <summary>查看学习与遗忘概率</summary>
          <p>技能最多七项；新技能不会被本次遗忘，是否遗忘不影响本课通过。</p>
          <table>
            <thead>
              <tr>
                <th>已有技能</th>
                <th>不忘</th>
                <th>忘1项</th>
                <th>忘2项</th>
                <th>忘3项</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(probabilities, count) in view.forgetProbabilities" :key="count">
                <th>{{ count }}</th>
                <td v-for="(probability, index) in probabilities" :key="index">{{ probability }}%</td>
              </tr>
            </tbody>
          </table>
        </details>
        <div v-for="book in view.books" :key="book.code" class="row">
          <strong>{{ book.skill }}技能书 ×{{ book.quantity }}</strong>
          <p v-if="book.reason">{{ book.reason }}</p>
          <IonButton :disabled="busy || !allowed('LEARN') || !book.available" @click="operate('LEARN', book.code)"
            >学习{{ book.skill }}</IonButton
          >
        </div>
        <div v-if="view.learningResult">
          <h3>学习结果</h3>
          <p>学会：{{ view.learningResult.learned }}</p>
          <p>
            遗忘：{{ view.learningResult.forgotten.length ? view.learningResult.forgotten.join('、') : '本次没有遗忘' }}
          </p>
          <p>保留技能：{{ view.learningResult.remaining.join('、') }}</p>
        </div>
      </section>
      <section v-if="view.items.length">
        <h2>训练背包</h2>
        <p>已用 {{ view.bagSlots }}/{{ view.bagCapacity }} 格 · 仓库容量 {{ view.storageCapacity }}</p>
        <div v-for="item in items" :key="item.id" class="row">
          <strong>{{ item.name }} ×{{ item.quantity }}</strong>
          <p v-if="item.pending">待领 {{ item.pending }}</p>
          <p v-if="item.equipment">
            需求 Lv.{{ item.requiredLevel }} · 生命 +{{ item.health }} · 魔力 +{{ item.mana }}
          </p>
          <small v-if="item.equipped">已穿戴</small>
          <IonButton
            v-if="allowed('EQUIP') && item.equipment"
            :disabled="busy || item.equipped"
            @click="operate('EQUIP', item.id)"
            >穿戴{{ item.name }}</IonButton
          >
        </div>
        <div v-if="pages > 1" class="row-title">
          <IonButton :disabled="page === 0" fill="clear" @click="page--">上一页</IonButton>
          <span>{{ page + 1 }}/{{ pages }}</span>
          <IonButton :disabled="page + 1 >= pages" fill="clear" @click="page++">下一页</IonButton>
        </div>
      </section>
      <section v-if="view.pets.length">
        <h2>借用伙伴与仓库</h2>
        <div v-for="pet in view.pets" :key="pet.id" class="row">
          <strong>{{ pet.name }}</strong>
          <p>
            {{ pet.position >= 100 ? '仓库' : pet.position < 2 ? '出战' : '替补或随身' }} ·
            {{ pet.locked ? '已锁定' : '未锁定' }}
          </p>
          <p>生命 {{ pet.hp }} · 魔力 {{ pet.mp }}</p>
          <IonButton
            v-if="allowed('DEPOSIT') && pet.position < 100"
            :disabled="busy"
            @click="operate('DEPOSIT', pet.id)"
            >存入仓库</IonButton
          >
          <template v-if="allowed('WITHDRAW') && pet.position >= 100">
            <IonButton
              v-for="position in [2, 3]"
              :key="position"
              :disabled="busy || view.pets.some((other) => other.position === position)"
              @click="operate('WITHDRAW', pet.id, position)"
              >取回第{{ position - 1 }}替补位</IonButton
            >
          </template>
          <IonButton v-if="allowed('LOCK') && !pet.locked" :disabled="busy" @click="operate('LOCK', pet.id)"
            >锁定保护</IonButton
          >
        </div>
      </section>
      <section class="stack">
        <template v-for="(label, type) in labels" :key="type">
          <IonButton v-if="allowed(type)" :disabled="busy" fill="outline" @click="operate(type)">{{ label }}</IonButton>
        </template>
      </section>
      <section v-if="view.progress.length">
        <h2>已完成的练习</h2>
        <p v-for="progress in view.progress" :key="progress">{{ progressLabels[progress] ?? '已完成课程步骤' }}</p>
      </section>
    </template>
    <template #footer>
      <template v-if="view?.status === 'ACTIVE'">
        <IonButton :disabled="busy" fill="clear" @click="exitOpen = true">退出课程</IonButton>
        <IonButton v-if="allowed('FINISH')" :disabled="busy" @click="operate('FINISH')">完成课程</IonButton>
      </template>
      <IonButton v-else-if="view" @click="router.replace('/world')">返回地图</IonButton>
    </template>
    <AppSheet v-model="exitOpen" :busy="busy" title="退出课程">
      <p>退出会回收本次教学资源，真实伙伴和背包保持不变。未完成的课程可以重新开始。</p>
      <template #footer><IonButton :disabled="busy" @click="operate('EXIT')">确认退出</IonButton></template>
    </AppSheet>
  </AppPage>
</template>
