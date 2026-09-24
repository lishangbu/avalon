<script setup lang="ts">
import type {QuestConversation} from './types'

defineProps<{conversations: QuestConversation[]}>()
</script>

<template>
  <section v-if="conversations.length" aria-label="现场交谈">
    <details v-for="conversation in conversations" :key="conversation.step + conversation.stage" class="conversation">
      <summary>{{ conversation.speaker }} · 关于《{{ conversation.title }}》</summary>
      <p>{{ conversation.text }}</p>
    </details>
  </section>
</template>

<style scoped>
.conversation {
  padding-block: 0.75rem;
  border-bottom: 1px solid var(--ion-color-step-150, #ddd);
  overflow-wrap: anywhere;
}
summary {
  min-height: 2rem;
  cursor: pointer;
  line-height: 1.6;
}
p {
  line-height: 1.8;
  white-space: pre-line;
}
</style>
