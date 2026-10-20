<script setup lang="ts">
/** 任务命令入口在人物与现场面板间复用；是否能提交始终由服务端状态决定。 */
import {IonButton} from '@ionic/vue'
import type {QuestAction} from './types'
import QuestRewardPreview from './QuestRewardPreview.vue'

defineProps<{actions: QuestAction[]; location: string; busy: boolean; pendingAcceptance: boolean}>()
defineEmits<{accept: [id: string]; interact: [id: string]}>()
</script>

<template>
  <section v-if="actions.length" aria-label="任务委托">
    <div v-for="action in actions" :key="action.id" class="row">
      <strong>{{ action.title }}</strong>
      <p>{{ action.guide }}</p>
      <p v-if="action.accepted">已接取，请完成现场要求后回报。</p>
      <QuestRewardPreview :step="action.id" />
      <template v-if="action.location === location">
        <IonButton
          v-if="action.acceptRequired && !action.accepted"
          :disabled="busy || !action.acceptAvailable || pendingAcceptance"
          @click="$emit('accept', action.id)"
          >接取任务</IonButton
        >
        <IonButton v-else :disabled="busy || !action.interactable" @click="$emit('interact', action.id)">
          {{ action.label }}
        </IonButton>
      </template>
    </div>
  </section>
</template>
