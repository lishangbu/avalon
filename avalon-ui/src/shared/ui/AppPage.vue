<script lang="ts" setup>
/** 导航只改变界面，不能代替服务器移动命令。所有子页面共用安全区与尺寸约束。 */
import {IonButton, IonContent, IonFooter, IonHeader, IonIcon, IonPage, IonTitle, IonToolbar} from '@ionic/vue'
import {chevronBackOutline, compassOutline, gridOutline} from 'ionicons/icons'
import {useRoute, useRouter} from 'vue-router'
import {computed, ref} from 'vue'
import {defaultNavigation, navigationEntries, useNavigation} from '@/features/navigation/queries'
import {useScrollMemory} from '@/shared/useScrollMemory'

const props = withDefaults(
  defineProps<{
    title: string
    tab?: string
    back?: string
    locked?: boolean
    scrollKey?: string
  }>(),
  {back: '/world', locked: false},
)
const content = ref<{$el: HTMLIonContentElement}>()
useScrollMemory(content, () => props.scrollKey)
const router = useRouter(),
  route = useRoute()
const navigation = useNavigation()
const tabs = computed(() => [
  {path: '/world', name: '冒险', icon: compassOutline},
  ...(navigation.data.value ?? defaultNavigation).flatMap((id) => navigationEntries.filter((entry) => entry.id === id)),
  {path: '/more', name: '更多', icon: gridOutline},
])

function back() {
  if (router.options.history.state.back) router.back()
  else void router.replace(props.back)
}
</script>
<template>
  <IonPage>
    <IonHeader class="ion-no-border">
      <IonToolbar>
        <IonButton
          v-if="!tab && !locked && route.path !== '/login'"
          slot="start"
          aria-label="返回"
          fill="clear"
          @click="back"
        >
          <IonIcon slot="icon-only" :icon="chevronBackOutline" />
        </IonButton>
        <IonTitle>{{ title }}</IonTitle>
        <slot name="header" />
      </IonToolbar>
    </IonHeader>
    <IonContent ref="content">
      <div class="page-body">
        <slot />
      </div>
    </IonContent>
    <IonFooter v-if="tab" class="ion-no-border">
      <nav aria-label="主导航" class="main-tabs">
        <button
          v-for="item in tabs"
          :key="item.path"
          :aria-current="tab === item.path ? 'page' : undefined"
          :class="{selected: tab === item.path}"
          @click="router.replace(item.path)"
        >
          <IonIcon :icon="item.icon" aria-hidden="true" />
          <span>{{ item.name }}</span>
        </button>
      </nav>
    </IonFooter>
    <IonFooter v-else-if="$slots.footer" class="ion-no-border">
      <div class="command-dock">
        <slot name="footer" />
      </div>
    </IonFooter>
  </IonPage>
</template>

<style scoped>
/* 与 Ionic 标题栏共用安全区来源，保证横屏刘海与底部手势区不遮挡内容。 */
.page-body {
  padding: 16px max(16px, var(--ion-safe-area-right, 0px)) max(28px, var(--ion-safe-area-bottom, 0px))
    max(16px, var(--ion-safe-area-left, 0px));
  max-width: 480px;
  margin: auto;
}

.main-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  padding: 5px max(8px, var(--ion-safe-area-right, 0px)) max(8px, var(--ion-safe-area-bottom, 0px))
    max(8px, var(--ion-safe-area-left, 0px));
  background: var(--ion-toolbar-background);
  border-top: 1px solid var(--game-border);
}

.main-tabs button {
  background: none;
  border: 0;
  display: grid;
  gap: 3px;
  justify-items: center;
  padding: 5px;
  min-height: 48px;
  font-size: 11px;
  color: var(--game-muted);
}

.main-tabs ion-icon {
  font-size: 23px;
}

.main-tabs .selected {
  color: var(--ion-color-primary);
}
</style>
