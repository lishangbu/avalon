<script setup lang="ts">
/** 只展示后端允许的学院课程，入课之后由角色活动会话锁定到实践页面。 */
import {nextTick, onBeforeUnmount, toRef, watch} from 'vue'
import {useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {useWorkshop, useWorkshopActions} from './workshop'
const props = defineProps<{active: boolean}>()
const emit = defineEmits<{enter: []; busy: [value: boolean]}>()
const query = useWorkshop(toRef(props, 'active'))
const action = useWorkshopActions()
const router = useRouter()
watch(
  () => action.busy.value || Boolean(action.pending.value),
  (value) => emit('busy', value),
  {immediate: true},
)
onBeforeUnmount(() => emit('busy', false))
async function enter() {
  // 等共享写锁的解除同步到弹层关闭保护，再发出关闭请求。
  await nextTick()
  emit('enter')
  await nextTick()
  await router.push('/academy/workshop')
}
async function start(id: string) {
  if (await action.start(id)) await enter()
}
async function retry() {
  if (await action.send()) await enter()
}
</script>
<template>
  <section
    v-if="query.data.value?.courses.some((course) => course.available) || action.pending.value || action.error.value"
  >
    <h2>学院操作课</h2>
    <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
    <IonButton v-if="action.pending.value" :disabled="action.busy.value" @click="retry">重试进入操作课</IonButton>
    <template v-for="course in query.data.value?.courses" :key="course.id">
      <div v-if="course.available" class="row">
        <strong>{{ course.title }}</strong>
        <p v-if="course.completed">已通过，可以再次练习。</p>
        <IonButton :disabled="action.busy.value || Boolean(action.pending.value)" @click="start(course.id)"
          >开始{{ course.title }}</IonButton
        >
      </div>
    </template>
  </section>
</template>
