<script lang="ts" setup>
import ContentLink from '@/features/content/ContentLink.vue'
/** 编队概览与收藏投影分开读取；批量放生必须逐只勾选，并在确认前复制名单。 */
import {computed, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {IonButton, IonIcon} from '@ionic/vue'
import {chevronForwardOutline, lockClosedOutline} from 'ionicons/icons'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import ReleaseSheet from '@/features/creature/ReleaseSheet.vue'
import CreaturePortrait from '@/features/creature/CreaturePortrait.vue'
import {useCreature, useEncyclopedia} from '@/features/creature/queries'
import {usePlacement} from '@/features/creature/actions'
import {useWorld} from '@/features/world/queries'
import type {OwnedCreature} from '@/features/creature/types'
import {usePageActive} from '@/shared/usePageActive'
import {usePageCategory} from '@/app/pageState'

const active = usePageActive(),
  router = useRouter()
const creatures = useCreature(active),
  world = useWorld(),
  encyclopedia = useEncyclopedia(active)
const placement = usePlacement()
const category = usePageCategory('companions', 'party')
const route = useRoute()
const standaloneEncyclopedia = computed(() => route.path === '/encyclopedia')
const tab = computed({
    get: () => (standaloneEncyclopedia.value ? 'encyclopedia' : category.value),
    set: (value) => {
      category.value = value
    },
  }),
  selected = ref<string[]>([]),
  releasing = ref<OwnedCreature[] | null>(null)
const shown = computed(
  () =>
    creatures.data.value?.filter((pet) =>
      tab.value === 'storage' ? pet.partyPosition >= 100 : pet.partyPosition < 100,
    ) ?? [],
)
const capacity = computed(() => 5 + Math.floor((world.data.value?.character?.level ?? 1) / 10))
const atCenter = computed(() =>
  world.data.value?.content.places
    .find((place) => place.id === world.data.value?.character?.location)
    ?.services.includes('STORAGE'),
)

function positionName(position: number) {
  return ['第一出战位', '第二出战位', '第一替补位', '第二替补位'][position] ?? (position >= 100 ? '仓库' : '携带')
}

function selectCategory(category: string) {
  tab.value = category
  selected.value = []
}

function confirmRelease() {
  releasing.value = (creatures.data.value ?? [])
    .filter((pet) => selected.value.includes(pet.id))
    .map((pet) => ({
      ...pet,
      skills: [...pet.skills],
    }))
}
</script>
<template>
  <AppPage
    :scroll-key="`companions:${tab}`"
    :tab="standaloneEncyclopedia ? '/encyclopedia' : '/companions'"
    :title="standaloneEncyclopedia ? '图鉴' : '伙伴'"
  >
    <div v-if="!standaloneEncyclopedia" aria-label="伙伴分类" class="segments">
      <button
        v-for="item in [
          {id: 'party', name: '随身'},
          {id: 'storage', name: '仓库'},
          {id: 'encyclopedia', name: '图鉴'},
        ]"
        :key="item.id"
        :aria-pressed="tab === item.id"
        @click="selectCategory(item.id)"
      >
        {{ item.name }}
      </button>
    </div>
    <AppNotice v-if="creatures.error.value || encyclopedia.error.value || placement.error.value" tone="danger">
      {{ creatures.error.value?.message || encyclopedia.error.value?.message || placement.error.value }}
    </AppNotice>
    <template v-if="tab === 'encyclopedia'">
      <IonButton
        v-if="encyclopedia.data.value?.some((entry) => entry.collected && !entry.rewarded)"
        :disabled="placement.busy.value"
        @click="placement.claimCollection()"
        >领取图鉴奖励
      </IonButton>
      <article v-for="entry in encyclopedia.data.value" :key="entry.species" class="row">
        <div class="row-title">
          <strong>{{ entry.name }}</strong
          ><small>{{ entry.element }}</small>
        </div>
        <p>{{ entry.collected ? `已收集 · 持有 ${entry.owned}` : '尚未收集' }}</p>
        <p v-if="entry.rewarded">首次收集奖励已领取</p>
        <details>
          <summary>图鉴介绍</summary>
          <p>{{ entry.description }}</p>
        </details>
        <p v-for="ability in entry.abilities" :key="ability.slot">
          {{ ability.slot === 'H' ? '隐藏' : '普通' }}：{{ ability.name }}
          <ContentLink kind="ability" :code="ability.name" name="查看特性效果" />
        </p>
      </article>
    </template>
    <template v-else>
      <template v-if="tab === 'party'"
        ><h2>出战编队</h2>
        <div class="formation">
          <button
            v-for="position in [0, 1, 2, 3]"
            :key="position"
            :disabled="!creatures.data.value?.find((pet) => pet.partyPosition === position)"
            @click="
              router.push('/companions/' + creatures.data.value?.find((pet) => pet.partyPosition === position)?.id)
            "
          >
            <small>{{ positionName(position) }}</small
            ><strong>{{ creatures.data.value?.find((pet) => pet.partyPosition === position)?.name ?? '空位' }}</strong>
          </button>
        </div>
        <h2>随身 {{ shown.length }} / {{ capacity }}</h2></template
      >
      <template v-else
        ><p>仓库 {{ shown.length }} / 30</p>
        <AppNotice v-if="!atCenter">前往伙伴中心后可存入或取出伙伴。</AppNotice>
      </template>
      <article v-for="pet in shown" :key="pet.id" class="row">
        <button class="pet-link" @click="router.push('/companions/' + pet.id)">
          <CreaturePortrait
            :source="encyclopedia.data.value?.find((entry) => entry.species === pet.species)?.image"
            :name="pet.name"
          />
          <span class="pet-description"
            ><strong>{{ pet.name }} <IonIcon v-if="pet.locked" :icon="lockClosedOutline" aria-label="已锁定" /></strong
            ><small>Lv.{{ pet.level }} · {{ pet.element }} · {{ positionName(pet.partyPosition) }}</small
            ><small>{{ pet.ability }} · {{ pet.abilitySlot === 'H' ? '隐藏' : '普通' }}</small></span
          >
          <IonIcon :icon="chevronForwardOutline" />
        </button>
        <label v-if="pet.partyPosition >= 4 && !pet.locked" class="release-choice"
          ><input
            v-model="selected"
            :aria-label="'选择放生' + pet.name"
            :value="pet.id"
            type="checkbox"
          />选择放生</label
        >
      </article>
      <p v-if="!shown.length" class="empty">暂无伙伴</p>
      <IonButton v-if="selected.length" color="danger" expand="block" fill="outline" @click="confirmRelease">
        放生所选（{{ selected.length }}）
      </IonButton>
    </template>
    <ReleaseSheet v-model="releasing" @released="selected = []" />
  </AppPage>
</template>
<style scoped>
.formation {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.formation button {
  display: grid;
  gap: 8px;
  padding: 12px;
  min-height: 80px;
  border: 1px solid var(--game-border);
  border-radius: 8px;
  background: var(--game-surface);
  color: var(--ion-text-color);
  text-align: left;
}

.pet-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: 0;
  padding: 0;
  min-height: 70px;
  background: transparent;
  color: var(--ion-text-color);
  text-align: left;
  gap: 12px;
}

.pet-link small {
  display: block;
  margin-top: 6px;
}

.pet-description {
  flex: 1;
  min-width: 0;
}

.release-choice {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  font-size: 13px;
  color: var(--game-muted);
}
</style>
