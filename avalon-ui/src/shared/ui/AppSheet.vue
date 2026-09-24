<script lang="ts" setup>
/** 提交中的不可关闭状态同时约束背景、关闭按钮和手势，避免确认信息被替换。 */
import {IonButton, IonContent, IonFooter, IonHeader, IonIcon, IonModal, IonTitle, IonToolbar} from '@ionic/vue'
import {closeOutline} from 'ionicons/icons'
import {onBeforeUnmount, ref, watch} from 'vue'
import {registerSheet} from '@/router/sheets'

const open = defineModel<boolean>({required: true})
const emit = defineEmits<{dismissed: []}>()
const props = withDefaults(defineProps<{title: string; busy?: boolean}>(), {busy: false})
const modal = ref<{$el: HTMLIonModalElement}>()
// Ionic 9 只在初始化时把 aria-label 转交给 shadow DOM 内真正的 dialog。
// 复用弹层切换标题时显式同步该节点，不能把 aria-labelledby 留在无 role 的宿主上。
function syncTitle() {
  modal.value?.$el.shadowRoot?.querySelector('[role="dialog"]')?.setAttribute('aria-label', props.title)
}
watch(() => props.title, syncTitle, {flush: 'post'})

function dismissed() {
  open.value = false
  emit('dismissed')
}

let unregister: (() => void) | undefined
watch(
  open,
  (value) => {
    unregister?.()
    unregister = value
      ? registerSheet({
          close: () => {
            open.value = false
          },
          canClose: () => !props.busy,
        })
      : undefined
  },
  {immediate: true, flush: 'sync'},
)
// Ionic 将弹层内容挂到应用容器。身份失效可能发生在 busy 期间，必须绕过关闭保护清理旧账号弹层。
onBeforeUnmount(() => {
  unregister?.()
  const element = modal.value?.$el
  if (!element) return
  element.canDismiss = true
  element.animated = false
  void element.dismiss(undefined, 'owner-unmounted').finally(() => element.remove())
})
</script>
<template>
  <!-- Ionic 会移动 modal 到应用根部；固定宿主保留 Vue 的插入锚点，避免相邻提示更新时引用已搬走的节点。 -->
  <div class="sheet-host">
    <IonModal
      ref="modal"
      :aria-label="title"
      :breakpoints="[0, 1]"
      :can-dismiss="!busy"
      :initial-breakpoint="1"
      :is-open="open"
      class="game-sheet"
      @didDismiss="dismissed"
      @didPresent="syncTitle"
    >
      <IonHeader class="ion-no-border">
        <IonToolbar>
          <IonTitle>{{ title }}</IonTitle>
          <IonButton slot="end" :disabled="busy" aria-label="关闭" fill="clear" @click="open = false">
            <IonIcon slot="icon-only" :icon="closeOutline" />
          </IonButton>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <div class="sheet-body">
          <slot />
        </div>
      </IonContent>
      <IonFooter v-if="$slots.footer" class="ion-no-border">
        <div class="command-dock">
          <slot name="footer" />
        </div>
      </IonFooter>
    </IonModal>
  </div>
</template>

<style>
.sheet-host {
  display: contents;
}

/* Ionic 将浮层挂到应用容器；以专用类限定作用域，保留跨挂载位置的样式。 */
.game-sheet {
  --width: min(480px, 100%);
  --height: min(82dvh, 760px);
  --border-radius: 16px 16px 0 0;
  --backdrop-opacity: 0.4;
}

.sheet-body {
  padding: 16px max(16px, var(--ion-safe-area-right, 0px)) max(24px, var(--ion-safe-area-bottom, 0px))
    max(16px, var(--ion-safe-area-left, 0px));
}
</style>
