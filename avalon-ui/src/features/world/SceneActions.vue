<script lang="ts" setup>
/** 场景操作只提交目标与位置版本；观察、照护和采集结果均由服务端核验。 */
import {computed, onBeforeUnmount, ref, toRef, watch} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppNotice from '@/shared/ui/AppNotice.vue'

type SceneAction = {
  id: string
  type: 'OBSERVE' | 'COLLECT_TASK' | 'DELIVER_TASK' | 'CARE' | 'PRESENT_RECORD'
  label: string
  description: string
  choices: {id: string; label: string}[]
  completed: boolean
  npcId: string | null
}
type SceneState = {location: string; revision: number; actions: SceneAction[]}
type SceneResult = {actionId: string; completed: boolean; dialogue: string}
type Submission = {actionId: string; requestId: string; expectedRevision: number; choiceId?: string}

const props = defineProps<{active: boolean; location: string; revision: number; npcId?: string}>()
const emit = defineEmits<{busy: [value: boolean]}>()
const client = useQueryClient()
const command = useAction(true)
const result = ref<SceneResult>()
// 请求结果未知时保留完整负载；重试不能换成新 UUID、答案或移动后的版本。
const pending = ref<Submission>()
watch(
  () => command.busy.value || Boolean(pending.value),
  (value) => emit('busy', value),
  {immediate: true},
)
onBeforeUnmount(() => emit('busy', false))
const query = useQuery({
  queryKey: computed(() => ['scene-actions', props.location, props.revision]),
  queryFn: () => api<SceneState>('/api/scene-actions'),
  enabled: toRef(props, 'active'),
  retry: false,
})
// 缓存或并行移动可能使响应落后。只有位置及版本都匹配时才显示可执行目标。
const actions = computed(() => {
  const state = query.data.value
  return state?.location === props.location && state.revision === props.revision
    ? state.actions.filter((action) => (action.npcId ?? undefined) === props.npcId)
    : []
})
const error = computed(() => command.error.value || query.error.value?.message)

async function submit(actionId: string, choiceId?: string) {
  if (command.busy.value || pending.value) return
  result.value = undefined
  pending.value = {actionId, choiceId, requestId: crypto.randomUUID(), expectedRevision: props.revision}
  await send()
}

async function send() {
  const request = pending.value
  if (!request) return
  await command.run(async () => {
    try {
      const {actionId, ...body} = request
      result.value = await api<SceneResult>(`/api/scene-actions/${encodeURIComponent(actionId)}`, 'POST', body)
      // 错误报告也是已确认的响应；允许换答案，用新请求再次提交。
      pending.value = undefined
    } catch (cause) {
      // 明确的业务拒绝可以重新操作；网络错误与服务端异常须沿原负载恢复。
      if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500) pending.value = undefined
      throw cause
    } finally {
      // 包含冲突与断线：刷新角色、任务及库存，页面不自行模拟任务完成或追加物品。
      await client.invalidateQueries()
    }
  })
}
</script>

<template>
  <section v-if="actions.length || error || pending || result" aria-label="现场行动">
    <h2>现场行动</h2>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <div v-if="pending">
      <p>尚未确认这次操作的结果，可以重试查询原操作。</p>
      <IonButton :disabled="command.busy.value" @click="send">重试原操作</IonButton>
    </div>
    <p v-if="result" role="status">{{ result.dialogue }}</p>
    <div v-for="action in actions" :key="action.id" class="row">
      <strong>{{ action.label }}</strong>
      <p>{{ action.description }}</p>
      <small v-if="action.completed">已完成</small>
      <div v-if="action.choices.length && !action.completed" class="stack">
        <IonButton
          v-for="choice in action.choices"
          :key="choice.id"
          :disabled="command.busy.value || Boolean(pending)"
          fill="outline"
          @click="submit(action.id, choice.id)"
          >{{ choice.label }}</IonButton
        >
      </div>
      <IonButton v-else :disabled="command.busy.value || Boolean(pending)" @click="submit(action.id)">
        {{ action.completed ? '回看记录' : action.label }}
      </IonButton>
    </div>
  </section>
</template>
