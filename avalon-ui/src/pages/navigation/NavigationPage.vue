<script setup lang="ts">
/** 草稿不随后台刷新覆盖；保存成功后更新共享缓存，失败保留选择供重试。 */
import {computed, ref, watch} from 'vue'
import {useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {defaultNavigation, navigationEntries, navigationKey, useNavigation} from '@/features/navigation/queries'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
const query = useNavigation(),
  client = useQueryClient(),
  action = useAction()
const draft = ref([...defaultNavigation]),
  initialized = ref(false),
  saved = ref(false)
watch(
  query.data,
  (value) => {
    if (value && !initialized.value) {
      draft.value = [...value]
      initialized.value = true
    }
  },
  {immediate: true},
)
const valid = computed(() => new Set(draft.value).size === 3)
function reset() {
  draft.value = [...defaultNavigation]
  saved.value = false
}
async function save() {
  saved.value = false
  saved.value = await action.run(async () => {
    const result = await api<string[]>('/api/account/navigation', 'PUT', draft.value)
    client.setQueryData(navigationKey, result)
  })
}
</script>
<template>
  <AppPage title="导航设置" back="/more">
    <AppNotice v-if="query.error.value || action.error.value" tone="danger">{{
      query.error.value?.message || action.error.value
    }}</AppNotice>
    <AppNotice v-if="saved">已保存</AppNotice>
    <form @submit.prevent="save">
      <label v-for="(_, index) in draft" :key="index" class="field">
        位置 {{ index + 1 }}
        <select v-model="draft[index]" :disabled="!initialized || action.busy.value" @change="saved = false">
          <option v-for="entry in navigationEntries" :key="entry.id" :value="entry.id">{{ entry.name }}</option>
        </select>
      </label>
      <AppNotice v-if="!valid" tone="danger">导航入口不能重复</AppNotice>
      <IonButton type="submit" :disabled="!initialized || !valid || action.busy.value">保存</IonButton>
      <IonButton fill="clear" :disabled="!initialized || action.busy.value" @click="reset">恢复默认</IonButton>
    </form>
  </AppPage>
</template>
