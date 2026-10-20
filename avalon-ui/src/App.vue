<script lang="ts" setup>
/** 身份变化重建路由出口，缓存页面中的旧账号状态不能留给新账号。 */
import {computed, provide, ref, watch} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {useRouter, useRoute} from 'vue-router'
import {api} from '@/shared/http'
import {IonApp, IonRouterOutlet} from '@ionic/vue'
import {authenticated} from '@/features/auth/session'
import {navigationError} from '@/router/state'
import AppNotice from '@/shared/ui/AppNotice.vue'
import ChatPanel from '@/features/chat/ChatPanel.vue'
import {openPrivateChatKey} from '@/features/chat/entry'
import {useWorld} from '@/features/world/queries'

// 已创角且认证通过才挂载聊天；与路由出口共用身份重建键，换号销毁流、草稿和正文。
const world = useWorld()
const router = useRouter(),
  route = useRoute(),
  client = useQueryClient()
// 受袭可能发生在地图、背包或自动采集页，统一发现后恢复正式战局。
const pvpPresence = useQuery({
  queryKey: ['pvp-presence'],
  enabled: computed(() => authenticated.value && !!world.data.value?.character && route.path !== '/battle'),
  queryFn: () => api<{id: string | null}>('/api/battle/pvp/current'),
  refetchInterval: 2000,
  retry: false,
})
watch(pvpPresence.data, async (value) => {
  if (value?.id && authenticated.value && route.path !== '/battle') {
    await client.invalidateQueries({queryKey: ['world']})
    await router.replace('/battle')
  }
})
// 页面入口总是调用当前角色的面板实例；未登录、尚未挂载或战斗中不保留待打开请求。
const chatPanel = ref<InstanceType<typeof ChatPanel>>()
provide(openPrivateChatKey, (name) => {
  if (authenticated.value && !world.data.value?.character?.activeBattleId) chatPanel.value?.openPrivate(name)
})

const generation = ref(0)
watch(authenticated, () => generation.value++, {flush: 'sync'})
</script>
<template>
  <IonApp>
    <AppNotice v-if="navigationError" class="navigation-error" tone="danger">{{ navigationError }}</AppNotice>
    <IonRouterOutlet :key="generation" />
    <ChatPanel
      ref="chatPanel"
      v-if="authenticated && world.data.value?.character"
      :key="`chat-${generation}-${world.data.value.character.id}`"
    />
  </IonApp>
</template>

<style scoped>
.navigation-error {
  position: absolute;
  top: 60px;
  z-index: 10000;
  inset-inline: 12px;
}
</style>
