<script setup lang="ts">
/** 全局任务记录只提供目标信息，交付仍在地图上的对应地点执行。 */
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {useQuests, useQuestInteraction, useQuestTracking, useQuestInstances} from '@/features/quest/queries'
import type {QuestJournalEntry, QuestInstanceView} from '@/features/quest/types'
import {useWorld} from '@/features/world/queries'
import {usePageActive} from '@/shared/usePageActive'
import {computed, ref} from 'vue'
import {IonButton, IonLabel, IonSegment, IonSegmentButton, alertController} from '@ionic/vue'
const interaction = useQuestInteraction()
const active = usePageActive()
const tracking = useQuestTracking(active)
const instances = useQuestInstances(active)
const statusLabels = {
  AVAILABLE: '可接取',
  ACTIVE: '进行中',
  ABANDONED: '已放弃',
  COMPLETED: '已完成',
  FAILED: '已失败',
  EXPIRED: '已过期',
}
const quests = useQuests(active),
  world = useWorld()
const view = ref('CURRENT'),
  tag = ref(''),
  location = ref('')
const journal = computed(() => quests.data.value?.journal ?? [])
const tags = computed(() => [
  ...new Map(journal.value.flatMap((entry) => entry.tags).map((value) => [value.id, value])).values(),
])
const locations = computed(() => [
  ...new Set(journal.value.flatMap((entry) => entry.steps.map((step) => step.location))),
])
const groups = computed(() =>
  [
    {type: 'MAIN', label: '主线'},
    {type: 'SIDE', label: '支线'},
  ].map((group) => ({
    ...group,
    entries: journal.value.filter(
      (entry) =>
        entry.type === group.type &&
        entry.completed === (view.value === 'COMPLETED') &&
        (!tag.value || entry.tags.some((value) => value.id === tag.value)) &&
        (!location.value || entry.steps.some((step) => step.location === location.value)),
    ),
  })),
)
const empty = computed(() => groups.value.every((group) => !group.entries.length))
function placeName(id: string) {
  return world.data.value?.content.places.find((place) => place.id === id)?.name ?? id
}
function actions(id: string) {
  return quests.data.value?.available.filter((action) => action.quest === id) ?? []
}
function tracked(entry: QuestJournalEntry) {
  return (entry.type === 'MAIN' ? tracking.data.value?.main : tracking.data.value?.side) === entry.id
}
async function track(entry: QuestJournalEntry) {
  if (!tracking.data.value) return
  await interaction.track(entry.type, tracked(entry) ? null : entry.id, tracking.data.value.revision)
}
/** 最新实例决定本轮目标展示，历史尝试仍由服务端保留供领奖与次数统计。 */
function instanceFor(quest: string) {
  return instances.data.value?.filter((value) => value.quest === quest).at(-1)
}

/** 路线选择不可反悔，确认后仅提交服务器返回的组和选项，不在浏览器决定任务完成。 */
async function choose(instance: QuestInstanceView, branch: QuestInstanceView['branches'][number]) {
  const alert = await alertController.create({
    header: `选择「${branch.title}」？`,
    message: '确认后本次任务不能更改路线。',
    buttons: [
      {text: '取消', role: 'cancel'},
      {
        text: '确认选择',
        handler: () => {
          void interaction.instanceCommand({
            instance: instance.id,
            round: instance.round,
            operation: 'CHOOSE',
            group: branch.group,
            option: branch.option,
          })
        },
      },
    ],
  })
  await alert.present()
}

async function abandon(entry: QuestJournalEntry) {
  const alert = await alertController.create({
    header: `放弃「${entry.title}」？`,
    // 普通任务保留进度；环任务和限时任务终止本次尝试，统一提示不能承诺所有任务都恢复。
    message: '放弃期间不累计目标。普通任务重接保留进度；委托环和限时任务重新开始。已发奖励保留，已消耗物品不返还。',
    buttons: [
      {text: '取消', role: 'cancel'},
      {
        text: '放弃任务',
        role: 'destructive',
        handler: () => {
          void interaction.abandon(entry.id, entry.revision)
        },
      },
    ],
  })
  await alert.present()
}
</script>
<template>
  <AppPage title="任务" tab="/quests">
    <AppNotice v-if="quests.error.value" tone="danger">{{ quests.error.value.message }}</AppNotice>
    <AppNotice v-if="interaction.error.value" tone="danger">{{ interaction.error.value }}</AppNotice>
    <AppNotice v-if="tracking.error.value" tone="danger">{{ tracking.error.value.message }}</AppNotice>
    <!-- 结果未知时用同一请求取回首次响应，不能让已消失的分支按钮成为唯一重试入口。 -->
    <div v-if="interaction.pendingInstance.value">
      <p>任务操作结果尚未确认，请重试获取结果。</p>
      <IonButton :disabled="interaction.busy.value" @click="interaction.retryInstance">重试任务操作</IonButton>
    </div>
    <IonSegment v-model="view" aria-label="任务记录">
      <IonSegmentButton value="CURRENT"><IonLabel>当前任务</IonLabel></IonSegmentButton>
      <IonSegmentButton value="COMPLETED"><IonLabel>已完成</IonLabel></IonSegmentButton>
    </IonSegment>
    <div class="quest-filters">
      <div class="form-field">
        <label for="quest-tag">标签</label>
        <select id="quest-tag" v-model="tag">
          <option value="">全部标签</option>
          <option v-for="item in tags" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select>
      </div>
      <div class="form-field">
        <label for="quest-location">地点</label>
        <select id="quest-location" v-model="location">
          <option value="">全部地点</option>
          <option v-for="id in locations" :key="id" :value="id">{{ placeName(id) }}</option>
        </select>
      </div>
    </div>
    <p v-if="quests.isPending.value">正在读取任务…</p>
    <p v-else-if="!quests.error.value && empty">
      {{ view === 'COMPLETED' ? '暂无符合条件的完成记录' : '暂无符合条件的任务' }}
    </p>
    <template v-for="group in groups" :key="group.type">
      <section v-if="group.entries.length">
        <h2>{{ group.label }}</h2>
        <article v-for="entry in group.entries" :key="entry.id" class="row">
          <h3>{{ entry.title }}</h3>
          <p v-if="tracked(entry)">追踪中</p>
          <p>{{ statusLabels[entry.status] }}</p>
          <p class="quest-summary">
            {{ entry.completed ? '已完成' : `${entry.completedSteps} / ${entry.totalSteps}`
            }}<span v-if="entry.tags.length"> · {{ entry.tags.map((value) => value.name).join(' · ') }}</span>
          </p>
          <div v-for="action in actions(entry.id)" :key="action.id">
            <p>{{ action.guide ?? action.label }}</p>
            <p v-if="action.acceptRequired">{{ action.accepted ? '已接取' : '待前往接取' }}</p>
          </div>
          <!-- 只显示已开放目标，不从任务编号或永久事实推算当前委托轮次。 -->
          <div v-if="instanceFor(entry.id)">
            <p v-if="instanceFor(entry.id)!.ringSize">
              委托第 {{ instanceFor(entry.id)!.round }} / {{ instanceFor(entry.id)!.ringSize }} 次
            </p>
            <p v-if="instanceFor(entry.id)!.deadline">
              截止时间：{{ new Date(instanceFor(entry.id)!.deadline!).toLocaleString() }}
            </p>
            <p v-for="goal in instanceFor(entry.id)!.goals" :key="`${goal.step}:${goal.id}`">
              {{ goal.title }}：{{ goal.value }} / {{ goal.amount }}
              <span v-if="goal.measure === 'HOLDING'">（当前持有）</span>
            </p>
            <IonButton
              v-for="branch in instanceFor(entry.id)!.branches"
              :key="`${branch.group}:${branch.option}`"
              :disabled="interaction.busy.value"
              @click="choose(instanceFor(entry.id)!, branch)"
              >{{ branch.title }}</IonButton
            >
          </div>
          <details>
            <summary>任务步骤</summary>
            <ol class="quest-steps">
              <li v-for="step in entry.steps" :key="step.id">
                <span>{{ step.title }} · {{ step.completed ? '已完成' : '未完成' }}</span>
                <small>{{ placeName(step.location) }}</small>
              </li>
            </ol>
          </details>
          <IonButton
            v-if="!entry.completed && entry.status !== 'ABANDONED'"
            fill="clear"
            :disabled="interaction.busy.value || !tracking.data.value"
            @click="track(entry)"
          >
            {{ tracked(entry) ? '取消追踪' : '追踪任务' }}
          </IonButton>
          <IonButton
            v-if="entry.abandonable && entry.status === 'ACTIVE'"
            fill="clear"
            color="danger"
            :disabled="interaction.busy.value"
            @click="abandon(entry)"
            >放弃任务</IonButton
          >
        </article>
      </section>
    </template>
  </AppPage>
</template>

<style scoped>
.quest-filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.quest-filters select {
  width: 100%;
  min-width: 0;
}
.quest-summary,
.quest-steps small {
  color: var(--game-muted);
}
.quest-steps {
  padding-left: 20px;
}
.quest-steps li {
  margin: 12px 0;
  overflow-wrap: anywhere;
}
.quest-steps small {
  display: block;
}
summary {
  padding: 12px 0;
  cursor: pointer;
}
h3 {
  font-size: 16px;
  overflow-wrap: anywhere;
}
</style>
