<script lang="ts" setup>
/** 地图只展示服务器给出的道路，页面统一按相邻关系决定查看与移动。 */
import {computed} from 'vue'
import {IonIcon} from '@ionic/vue'
import {homeOutline, leafOutline, locationOutline} from 'ionicons/icons'
import type {Place} from './types'

const props = defineProps<{places: Place[]; current: string; expanded: boolean; busy?: boolean}>()
defineEmits<{select: [place: Place]}>()
const visible = computed(() => {
  const current = props.places.find((place) => place.id === props.current)
  return props.expanded
    ? props.places
    : props.places.filter((place) => place.id === props.current || current?.exits.includes(place.id))
})
const bounds = computed(() => {
  const xs = visible.value.map((place) => place.x),
    ys = visible.value.map((place) => place.y)
  return {
    x: Math.min(...xs),
    y: Math.min(...ys),
    width: Math.max(...xs) - Math.min(...xs) || 1,
    height: Math.max(...ys) - Math.min(...ys) || 1,
  }
})
// 附近图表示相邻关系而非实际里程。按不同坐标的顺序均匀排布，避免远处学院压缩城内节点间距。
const localAxes = computed(() => ({
  x: [...new Set(visible.value.map((place) => place.x))].sort((a, b) => a - b),
  y: [...new Set(visible.value.map((place) => place.y))].sort((a, b) => a - b),
}))
// 全图按坐标跨度保留实际间距，避免后续区域挤进固定高度后遮挡名称和点击区域。
// 邻近地图仍完整展示在手机宽度内；全图允许双向滚动，不扩大页面本身。
const canvas = computed(() =>
  props.expanded
    ? {
        minWidth: `${bounds.value.width * 112 + 128}px`,
        height: `${bounds.value.height * 100 + 128}px`,
      }
    : undefined,
)

function point(place: Place) {
  const box = bounds.value
  if (props.expanded)
    return {
      x: ((64 + (place.x - box.x) * 112) / (box.width * 112 + 128)) * 100,
      y: ((64 + (place.y - box.y) * 100) / (box.height * 100 + 128)) * 100,
    }
  const axes = localAxes.value
  return {
    x: axes.x.length === 1 ? 50 : 16 + (axes.x.indexOf(place.x) / (axes.x.length - 1)) * 68,
    y: axes.y.length === 1 ? 50 : 18 + (axes.y.indexOf(place.y) / (axes.y.length - 1)) * 64,
  }
}

const roads = computed(() =>
  visible.value.flatMap((place) =>
    place.exits.flatMap((id) => {
      const destination = visible.value.find((other) => other.id === id)
      return destination && place.id < id ? [{id: `${place.id}-${id}`, from: point(place), to: point(destination)}] : []
    }),
  ),
)
</script>
<template>
  <div :class="{expanded}" :tabindex="expanded ? 0 : undefined" aria-label="冒险地图" class="world-map" role="region">
    <div :style="canvas" class="map-canvas">
      <svg aria-hidden="true" preserveAspectRatio="none" viewBox="0 0 100 100">
        <line
          v-for="road in roads"
          :key="road.id"
          :x1="road.from.x"
          :x2="road.to.x"
          :y1="road.from.y"
          :y2="road.to.y"
        />
      </svg>
      <button
        v-for="place in visible"
        :key="place.id"
        :disabled="busy"
        :aria-label="place.name + (place.id === current ? '，当前位置' : '')"
        :class="{current: place.id === current}"
        :style="{left: point(place).x + '%', top: point(place).y + '%'}"
        @click="$emit('select', place)"
      >
        <IonIcon :icon="place.id === current ? locationOutline : place.npcs?.length ? homeOutline : leafOutline" />
        <span>{{ place.name }}</span>
      </button>
    </div>
  </div>
</template>
<style scoped>
.world-map {
  position: relative;
  height: 300px;
  margin: 16px -16px;
  background: var(--game-tint);
  overflow: hidden;
}

.world-map.expanded {
  height: 480px;
  max-height: 65dvh;
  overflow: auto;
  overscroll-behavior: contain;
}

.map-canvas {
  position: relative;
  width: 100%;
  height: 100%;
}

svg {
  position: absolute;
  width: 100%;
  height: 100%;
}

line {
  stroke: var(--game-muted);
  stroke-width: 0.6;
  stroke-dasharray: 1.5 1.5;
  opacity: 0.45;
}

button {
  position: absolute;
  transform: translate(-50%, -50%);
  display: grid;
  justify-items: center;
  gap: 4px;
  width: 88px;
  min-height: 54px;
  border: 0;
  color: var(--ion-text-color);
  background: transparent;
  font-size: 12px;
}

ion-icon {
  font-size: 24px;
  padding: 7px;
  border-radius: 8px;
  background: var(--game-surface);
  border: 1px solid var(--game-border);
}

.current ion-icon {
  background: var(--ion-color-primary);
  color: var(--ion-color-primary-contrast);
}

span {
  background: var(--game-surface);
  padding: 2px 4px;
  border-radius: 3px;
}
</style>
