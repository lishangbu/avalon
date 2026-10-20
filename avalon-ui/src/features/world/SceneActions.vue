<script lang="ts" setup>
/** 设施与人物资料直接显示；实际操作提交目标与位置版本，由服务端核验。 */
import {computed, onBeforeUnmount, onMounted, ref, toRef, watch} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {
  clearSceneSubmission,
  readSceneSubmission,
  saveSceneSubmission,
  sceneSubmissionKey,
  type SceneSubmission,
} from './scene-submission-storage'

type SceneAction = {
  id: string
  type: 'COLLECT_TASK' | 'DELIVER_TASK' | 'CARE' | 'PRESENT_RECORD' | 'OPERATE'
  label: string
  description: string
  choices: {id: string; label: string}[]
  completed: boolean
  recordedAt?: string | null
  npcId: string | null
}
type Information = {id: string; label: string; description: string; npcId: string | null}
type SceneState = {location: string; revision: number; actions: SceneAction[]; information: Information[]}
type SceneResult = {actionId: string; completed: boolean; dialogue: string}
const props = defineProps<{active: boolean; characterId: string; location: string; revision: number; npcId?: string}>()
const emit = defineEmits<{busy: [value: boolean]}>()
const client = useQueryClient()
const command = useAction(true)
const result = ref<SceneResult>()
// 请求结果未知时保留完整负载；重试不能换成新 UUID、答案或移动后的版本。
const pending = ref<SceneSubmission>()
const storageError = ref('')
const storageKey = computed(() => sceneSubmissionKey(props.characterId, props.npcId))
watch(
  storageKey,
  (key) => {
    // 刷新或重新打开浏览器后只恢复原负载，等待玩家主动确认；不自动重放写命令。
    pending.value = undefined
    result.value = undefined
    storageError.value = ''
    try {
      pending.value = readSceneSubmission(localStorage, key)
    } catch {
      storageError.value = '暂时无法读取本机操作记录，请稍后重试。'
    }
  },
  {immediate: true},
)
watch(
  () => command.busy.value || Boolean(pending.value),
  (value) => emit('busy', value),
  {immediate: true},
)
onBeforeUnmount(() => emit('busy', false))
/**
 * 多标签页共用localStorage但不会共享Vue内存。监听其他标签页的变更后只重新读取当前
 * 角色与人物面板的槽位，既能及时显示“待确认”或下一条命令，也绝不因同步事件自动发送写请求。
 */
function synchronizeOtherTab(event: StorageEvent) {
  const key = storageKey.value
  if (!event.key || (event.key !== key && !event.key.startsWith(`${key}:`))) return
  try {
    pending.value = readSceneSubmission(localStorage, key)
    storageError.value = ''
  } catch {
    storageError.value = '暂时无法同步其他标签页的操作记录，请稍后重试。'
  }
}
onMounted(() => window.addEventListener('storage', synchronizeOtherTab))
onBeforeUnmount(() => window.removeEventListener('storage', synchronizeOtherTab))
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
const information = computed(() => {
  const state = query.data.value
  return state?.location === props.location && state.revision === props.revision
    ? state.information.filter((entry) => (entry.npcId ?? undefined) === props.npcId)
    : []
})
const error = computed(() => storageError.value || command.error.value || query.error.value?.message)

async function submit(actionId: string, choiceId?: string) {
  if (command.busy.value || pending.value) return
  result.value = undefined
  const request = {actionId, choiceId, requestId: crypto.randomUUID(), expectedRevision: props.revision}
  const critical = async () => {
    // 必须先持久化再发请求；存储失败时不发送无法跨刷新恢复的写命令。
    try {
      // 另一标签页可能留下尚未确认的操作，先展示其重试入口，不以当前点击覆盖或自动重发。
      const saved = readSceneSubmission(localStorage, storageKey.value)
      if (saved) {
        pending.value = saved
        return
      }
      saveSceneSubmission(localStorage, storageKey.value, request)
      storageError.value = ''
    } catch {
      storageError.value = '暂时无法保存本机操作记录，请稍后重试。'
      return
    }
    pending.value = request
    await send()
  }
  // Web Locks让支持它的浏览器把“读取、落盘、发送”作为一个跨标签页临界区；旧浏览器
  // 没有该API时仍由服务端角色锁、版本校验及请求幂等保证最终一致性，不伪造本地互斥成功。
  if ('locks' in navigator && navigator.locks) {
    await navigator.locks.request(`avalon-scene-write:${props.characterId}`, critical)
  } else {
    await critical()
  }
}

async function send() {
  const request = pending.value
  const key = storageKey.value
  if (!request) return
  await command.run(async () => {
    try {
      const {actionId, ...body} = request
      result.value = await api<SceneResult>(`/api/scene-actions/${encodeURIComponent(actionId)}`, 'POST', body)
      // 错误报告也是已确认的响应；允许换答案，用新请求再次提交。
      clearSceneSubmission(localStorage, key, request.requestId)
      if (storageKey.value === key) pending.value = readSceneSubmission(localStorage, key)
    } catch (cause) {
      // 明确的业务拒绝可以重新操作；网络错误与服务端异常须沿原负载恢复。
      if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500) {
        clearSceneSubmission(localStorage, key, request.requestId)
        if (storageKey.value === key) pending.value = readSceneSubmission(localStorage, key)
      }
      throw cause
    } finally {
      // 包含冲突与断线：刷新角色、任务及库存，页面不自行模拟任务完成或追加物品。
      await client.invalidateQueries()
    }
  })
}
</script>

<template>
  <section v-if="information.length" :aria-label="npcId ? '相关资料' : '地点资料'">
    <h2>{{ npcId ? '相关资料' : '地点资料' }}</h2>
    <div v-for="entry in information" :key="entry.id" class="row">
      <strong>{{ entry.label }}</strong>
      <p>{{ entry.description }}</p>
    </div>
  </section>
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
      <!-- 使用服务端首次写入时间，保留标准时间供回看核对；不以浏览器刷新时间冒充现场观察时间。 -->
      <p v-if="action.completed && action.recordedAt" class="muted">
        记录时间：<time :datetime="action.recordedAt">{{ action.recordedAt }}</time>
      </p>
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
