<script lang="ts" setup>
import {IonIcon} from '@ionic/vue'
import {alertCircleOutline, checkmarkCircleOutline, informationCircleOutline} from 'ionicons/icons'

withDefaults(defineProps<{tone?: 'danger' | 'warning' | 'success' | 'info'}>(), {tone: 'info'})
</script>
<template>
  <div :class="tone" :role="tone === 'danger' ? 'alert' : 'status'" class="notice">
    <IonIcon
      :icon="
        tone === 'danger' || tone === 'warning'
          ? alertCircleOutline
          : tone === 'success'
            ? checkmarkCircleOutline
            : informationCircleOutline
      "
      aria-hidden="true"
    />
    <div>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.notice {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  border-left: 3px solid var(--ion-color-primary);
  background: var(--game-tint);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 14px;
  overflow-wrap: anywhere;
}

.notice ion-icon {
  font-size: 19px;
  flex: 0 0 19px;
  margin-top: 1px;
}

.notice.danger {
  border-color: var(--game-enemy);
}

.notice.warning {
  border-color: var(--game-gold);
}

.notice.success {
  border-color: var(--game-health);
}
</style>
