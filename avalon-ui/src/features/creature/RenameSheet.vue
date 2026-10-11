<script setup lang="ts">
/** 每次打开读取正式报价；网络重试复用同一请求，费用与次数由服务端复核。 */
import {computed, onMounted, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import {useQueryClient} from '@tanstack/vue-query'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

const props = defineProps<{id: string}>()
const emit = defineEmits<{closed: []}>()
const open = ref(true),
  name = ref('')
const quote = ref<{name: string; renamed: number; price: number; maxLength: number}>()
const action = useAction(true),
  client = useQueryClient()
let pending: {requestId: string; name: string; expectedName: string; renamed: number; expectedPrice: number} | undefined
const valid = computed(
  () =>
    quote.value &&
    name.value !== quote.value.name &&
    /^[\p{L}\p{N}]+$/u.test(name.value) &&
    [...name.value].length <= quote.value.maxLength,
)
async function load() {
  await action.run(async () => {
    quote.value = await api(`/api/creatures/${props.id}/name`)
    name.value = quote.value!.name
    pending = undefined
  })
}
onMounted(load)
async function confirm() {
  if (!quote.value || !valid.value) return
  // 格式校验只负责按钮可用性，违禁词由服务端在扣费前核验，直接访问接口也适用。
  // 修改输入后创建新请求；原文重试沿用请求号，不因失败清空名称或消耗免费次数。
  if (!pending || pending.name !== name.value)
    pending = {
      requestId: crypto.randomUUID(),
      name: name.value,
      expectedName: quote.value.name,
      renamed: quote.value.renamed,
      expectedPrice: quote.value.price,
    }
  if (
    await action.run(async () => {
      await api(`/api/creatures/${props.id}/name`, 'POST', pending)
      await client.invalidateQueries()
    })
  )
    // run失败会在当前弹窗显示服务端拒绝信息，保留输入与报价；成功才关闭弹窗。
    open.value = false
}
</script>
<template>
  <AppSheet v-model="open" title="伙伴改名" :busy="action.busy.value" @dismissed="emit('closed')">
    <AppNotice v-if="action.error.value" tone="danger">{{ action.error.value }}</AppNotice>
    <template v-if="quote">
      <p>当前名称：{{ quote.name }}</p>
      <label class="form-field"
        >新名称
        <input v-model="name" :disabled="action.busy.value" :maxlength="quote.maxLength" autocomplete="off" />
      </label>
      <p>费用：{{ quote.price === 0 ? '首次免费' : `${quote.price} 金币` }}</p>
      <p>最多 {{ quote.maxLength }} 个字，仅限文字和数字。</p>
    </template>
    <IonButton v-else :disabled="action.busy.value" @click="load">重新获取费用</IonButton>
    <template #footer>
      <IonButton fill="outline" :disabled="action.busy.value" @click="open = false">取消</IonButton>
      <IonButton :disabled="!valid || action.busy.value" @click="confirm">确认改名</IonButton>
    </template>
  </AppSheet>
</template>
