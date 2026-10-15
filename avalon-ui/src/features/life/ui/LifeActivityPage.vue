<script setup lang="ts">
import {computed} from 'vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {lifeActivities, type LifeActivityId} from '../activities'

const props = defineProps<{
  activity: LifeActivityId
  pending: boolean
  errors: (string | null | undefined)[]
  result?: string
}>()
const definition = computed(() => lifeActivities[props.activity])
const error = computed(() => props.errors.find((value) => !!value))
</script>
<template>
  <AppPage :title="definition.title" back="/life">
    <p>{{ definition.description }}</p>
    <slot name="introduction" />
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <AppNotice v-if="result" tone="info">{{ result }}</AppNotice>
    <p v-if="pending" role="status">{{ definition.loading }}</p>
    <slot />
  </AppPage>
</template>
