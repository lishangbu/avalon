<script lang="ts" setup>
/** 身份变化重建路由出口，缓存页面中的旧账号状态不能留给新账号。 */
import {ref, watch} from 'vue'
import {IonApp, IonRouterOutlet} from '@ionic/vue'
import {authenticated} from '@/features/auth/session'
import {navigationError} from '@/router/state'
import AppNotice from '@/shared/ui/AppNotice.vue'

const generation = ref(0)
watch(authenticated, () => generation.value++, {flush: 'sync'})
</script>
<template>
  <IonApp>
    <AppNotice v-if="navigationError" class="navigation-error" tone="danger">{{ navigationError }}</AppNotice>
    <IonRouterOutlet :key="generation" />
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
