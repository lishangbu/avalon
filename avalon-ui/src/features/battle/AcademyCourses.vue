<script lang="ts" setup>
/** 学院借用名单由服务器提供，选取顺序明确对应两名出战与两名替补，不修改真实编队。 */
import {computed, nextTick, onBeforeUnmount, ref, toRef, watch} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {battleKey} from './queries'
import type {Battle} from './types'

type Course = {id: string; name: string; goal: string; selectPartners: boolean; completed: boolean}
type Academy = {available: boolean; partners: {id: string; name: string}[]; courses: Course[]}
type Admission = {course: string; partners: string[]; requestId: string}
const props = defineProps<{active: boolean; location: string}>()
const emit = defineEmits<{enter: []; busy: [value: boolean]}>()
const client = useQueryClient()
const router = useRouter()
const action = useAction(true)
const selected = ref<string[]>([])
const pending = ref<Admission>()
watch(
  () => action.busy.value || Boolean(pending.value),
  (value) => emit('busy', value),
  {immediate: true},
)
onBeforeUnmount(() => emit('busy', false))
const query = useQuery({
  queryKey: computed(() => ['academy-courses', props.location]),
  queryFn: () => api<Academy>('/api/battle/academy'),
  enabled: toRef(props, 'active'),
  retry: false,
})
const error = computed(() => action.error.value || query.error.value?.message)
const positions = ['第一出战位', '第二出战位', '第一替补位', '第二替补位']

function choose(id: string) {
  if (action.busy.value || pending.value) return
  const index = selected.value.indexOf(id)
  if (index >= 0) selected.value.splice(index, 1)
  else if (selected.value.length < 4) selected.value.push(id)
}

async function start(course: Course) {
  if (pending.value || action.busy.value || (course.selectPartners && selected.value.length !== 4)) return
  pending.value = {
    course: course.id,
    partners: course.selectPartners ? [...selected.value] : [],
    requestId: crypto.randomUUID(),
  }
  await send()
}

async function send() {
  const admission = pending.value
  if (!admission) return
  const entered = await action.run(async () => {
    let battle: Battle
    try {
      battle = await api<Battle>('/api/battle/academy', 'POST', admission)
    } catch (cause) {
      if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500) pending.value = undefined
      await client.invalidateQueries()
      throw cause
    }
    pending.value = undefined
    client.setQueryData(battleKey, battle)
    await client.invalidateQueries()
  })
  if (entered) {
    // run 返回后共享写锁才真正释放；先让弹层收到可关闭状态，再关闭并进入战场。
    // 不能在 run 内跳转，否则其他服务组件仍会通过共享写锁阻止弹层消失。
    await nextTick()
    emit('enter')
    await nextTick()
    await router.push('/battle')
  }
}
</script>

<template>
  <section v-if="query.data.value?.available || error || pending" aria-label="学院实践课">
    <h2>学院实践课</h2>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <div v-if="pending">
      <p>课程入场结果尚未确认，重试将恢复同一次申请。</p>
      <IonButton :disabled="action.busy.value" @click="send">重试进入课程</IonButton>
    </div>
    <template v-if="query.data.value?.available">
      <p>课程使用借用伙伴与教学资源，离开后恢复原来的冒险状态。</p>
      <div v-for="course in query.data.value.courses" :key="course.id" class="row">
        <strong>{{ course.name }}</strong>
        <p>{{ course.goal }}</p>
        <small v-if="course.completed">已通过，可再次练习</small>
        <div v-if="course.selectPartners">
          <p>从五只伙伴中依次选择四只：前两只出战，后两只替补。</p>
          <div class="stack">
            <IonButton
              v-for="partner in query.data.value.partners"
              :key="partner.id"
              :aria-pressed="selected.includes(partner.id)"
              :fill="selected.includes(partner.id) ? 'solid' : 'outline'"
              :disabled="
                action.busy.value || Boolean(pending) || (!selected.includes(partner.id) && selected.length === 4)
              "
              @click="choose(partner.id)"
              >{{ partner.name
              }}{{ selected.includes(partner.id) ? ` · ${positions[selected.indexOf(partner.id)]}` : '' }}</IonButton
            >
          </div>
          <p>已选 {{ selected.length }}/4，再次点击可取消，后续位置随之顺移。</p>
        </div>
        <IonButton
          :disabled="action.busy.value || Boolean(pending) || (course.selectPartners && selected.length !== 4)"
          @click="start(course)"
          >开始{{ course.name }}</IonButton
        >
      </div>
    </template>
  </section>
</template>
