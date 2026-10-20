<script lang="ts" setup>
/** 页面组合地图、任务与地点授权操作；进入战斗后由路由守卫锁定战场。 */
import {computed, nextTick, ref, watch} from 'vue'
import {IonButton, IonIcon} from '@ionic/vue'
import {contractOutline, expandOutline} from 'ionicons/icons'
import {useRouter} from 'vue-router'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import DungeonLobbyNotice from '@/features/dungeon/DungeonLobbyNotice.vue'
import SoloDungeonEntrance from '@/features/dungeon/SoloDungeonEntrance.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import WorldMap from '@/features/world/WorldMap.vue'
import SceneActions from '@/features/world/SceneActions.vue'
import AcademyCourses from '@/features/battle/AcademyCourses.vue'
import PvpPlayers from '@/features/battle/PvpPlayers.vue'
import WorkshopCourses from '@/features/academy/WorkshopCourses.vue'
import {useWorld} from '@/features/world/queries'
import {useMovement} from '@/features/world/actions'
import type {Place} from '@/features/world/types'
import {useQuestConversations, useQuestInteraction, useQuests, useQuestTracking} from '@/features/quest/queries'
import QuestActions from '@/features/quest/QuestActions.vue'
import {npcQuestMarker} from '@/features/world/npc'
import QuestConversations from '@/features/quest/QuestConversations.vue'
import {useBattleLobby} from '@/features/battle/queries'
import {useBattleActions} from '@/features/battle/actions'
import {usePageActive} from '@/shared/usePageActive'
import {usePlacement} from '@/features/creature/actions'
import {useEvolutionResearch} from '@/features/creature/evolution'
import {useMarketAccess} from '@/features/market/market'
import {useEnhancementAccess} from '@/features/inventory/enhancement'
import {api} from '@/shared/http'
import type {OwnedCreature} from '@/features/creature/types'

const router = useRouter(),
  active = usePageActive()
const worldQuery = useWorld(),
  world = worldQuery.data
const quests = useQuests(active),
  lobbyQuery = useBattleLobby(active),
  lobby = lobbyQuery.data
const tracking = useQuestTracking(active)
const conversations = useQuestConversations(active)
const creatureActions = usePlacement()
const research = useEvolutionResearch(active)
const market = useMarketAccess(active)
const enhancement = useEnhancementAccess(active)
const movement = useMovement(),
  interaction = useQuestInteraction(),
  battle = useBattleActions()
const expanded = ref(false),
  selected = ref<Place>(),
  locationOpen = ref(false),
  dialogueOpen = ref(false)
const current = computed(() =>
  world.value?.content.places.find((place) => place.id === world.value?.character?.location),
)
/** 远处只突出每项追踪任务的下一步；当前地点的其他委托仍可接取或交付。 */
const displayedQuests = computed(() => {
  const available = quests.data.value?.available ?? []
  const selected = [tracking.data.value?.main, tracking.data.value?.side]
    .filter((id): id is string => Boolean(id))
    .map((id) => available.find((action) => action.quest === id)?.id)
  return available.filter((action) => action.location === current.value?.id || selected.includes(action.id))
})
const npcOpen = ref(false)
const courseBusy = ref(false)
const npcSceneBusy = ref(false)
const fieldSceneBusy = ref(false)
const selectedNpcId = ref<string>()
const localNpcs = computed(() =>
  (world.value?.content.npcs ?? []).filter((npc) => current.value?.npcs?.some((binding) => binding.npcId === npc.id)),
)
const selectedNpc = computed(() => localNpcs.value.find((npc) => npc.id === selectedNpcId.value))
const localNpcQuests = computed(() =>
  (quests.data.value?.available ?? []).filter(
    (action) => action.location === current.value?.id && action.npcId === selectedNpc.value?.id,
  ),
)
const outsideQuests = computed(() =>
  displayedQuests.value.filter((action) => action.location !== current.value?.id || !action.npcId),
)
const npcConversations = computed(() =>
  (conversations.data.value ?? []).filter((line) => line.npcId === selectedNpc.value?.id),
)
const hasService = (code: string) => selectedNpc.value?.services.includes(code) ?? false
const marker = (id: string) => npcQuestMarker(quests.data.value?.available ?? [], id, current.value?.id ?? '')
function openNpc(id: string) {
  selectedNpcId.value = id
  npcOpen.value = true
}
watch(
  () => current.value?.id,
  () => {
    npcOpen.value = false
    selectedNpcId.value = undefined
  },
)
watch(active, (value) => {
  if (!value) npcOpen.value = false
})
const busy = computed(
  () =>
    courseBusy.value ||
    npcSceneBusy.value ||
    fieldSceneBusy.value ||
    movement.busy.value ||
    interaction.busy.value ||
    battle.busy.value ||
    creatureActions.busy.value,
)

/** 主动选择服务先关闭人物层，避免路由守卫把这次跳转当作返回关闭弹层。 */
async function visitService(path: string) {
  npcOpen.value = false
  await nextTick()
  await router.push(path)
}

/** 仅显示后端投影中的在场人物，姓名从稳定人物名录取得。 */
function receptionNames(place?: Place): string {
  if (!place) return ''
  if (place.npcs?.length) {
    return place.npcs
      .map((binding) => world.value?.content.npcs?.find((npc) => npc.id === binding.npcId)?.name)
      .filter((name): name is string => Boolean(name))
      .join('、')
  }
  return ''
}
const error = computed(
  () =>
    movement.error.value ||
    interaction.error.value ||
    battle.error.value ||
    creatureActions.error.value ||
    worldQuery.error.value?.message ||
    quests.error.value?.message ||
    tracking.error.value?.message ||
    conversations.error.value?.message ||
    lobbyQuery.error.value?.message,
)

function inspect(place: Place) {
  selected.value = place
  locationOpen.value = true
}

// 地图缩放只改变展示范围；相邻地点直接移动，远处地点预览，当前位置不重复打开详情。
async function selectPlace(place: Place) {
  if (busy.value || place.id === current.value?.id) return
  if (current.value?.exits.includes(place.id)) await move(place)
  else inspect(place)
}

async function move(destination = selected.value) {
  if (busy.value) return
  if (destination && world.value?.character && (await movement.move(destination.id, world.value.character.revision)))
    locationOpen.value = false
}

const petDelivery = ref<{step: string; amount: number; candidates: OwnedCreature[]}>()
const selectedPetIds = ref<string[]>([])
const petDeliveryOpen = ref(false)
const petDeliveryError = ref('')
async function interact(step: string) {
  petDeliveryError.value = ''
  try {
    if (!quests.data.value?.available.find((action) => action.id === step)?.petDelivery) {
      if (await interaction.interact(step)) dialogueOpen.value = true
      return
    }
    const preview = await api<{amount: number; candidates: OwnedCreature[]}>(
      `/api/quests/${encodeURIComponent(step)}/pet-delivery`,
    )
    if (preview.amount > 0) {
      petDelivery.value = {step, ...preview}
      selectedPetIds.value = []
      petDeliveryOpen.value = true
    } else if (await interaction.interact(step)) dialogueOpen.value = true
  } catch (cause) {
    petDeliveryError.value = cause instanceof Error ? cause.message : '无法读取交付名单'
  }
}
async function confirmPetDelivery() {
  const value = petDelivery.value
  if (!value || selectedPetIds.value.length !== value.amount) return
  const pets = value.candidates.filter((pet) => selectedPetIds.value.includes(pet.id))
  if (await interaction.interact(value.step, pets)) {
    petDeliveryOpen.value = false
    dialogueOpen.value = true
  }
}

async function accept(step: string) {
  if (world.value?.character && (await interaction.accept(step, world.value.character.revision)))
    dialogueOpen.value = true
}

/** 交付响应丢失后仍能回放同一请求，并打开服务器保存的首次对话。 */
async function retryQuestInstance() {
  if (await interaction.retryInstance()) dialogueOpen.value = true
}
async function retryAcceptance() {
  if (await interaction.retryAcceptance()) dialogueOpen.value = true
}

async function start(id: string) {
  locationOpen.value = false
  await battle.start(id)
}
</script>
<template>
  <AppPage :title="current?.name ?? '冒险'" tab="/world">
    <DungeonLobbyNotice :active="active" />
    <template #header>
      <IonButton slot="end" :aria-label="expanded ? '收起全图' : '展开全图'" fill="clear" @click="expanded = !expanded">
        <IonIcon slot="icon-only" :icon="expanded ? contractOutline : expandOutline" />
      </IonButton>
    </template>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <AppNotice v-if="petDeliveryError" tone="danger">{{ petDeliveryError }}</AppNotice>
    <!-- 交付已提交但网络响应丢失时，任务按钮可能消失，因此提供独立的幂等重试入口。 -->
    <div v-if="interaction.pendingInstance.value">
      <p>任务操作结果尚未确认，请重试获取结果。</p>
      <IonButton :disabled="busy" @click="retryQuestInstance">重试任务操作</IonButton>
    </div>
    <div v-if="interaction.pendingAcceptance.value">
      <p>任务接取结果尚未确认，可重试原操作。</p>
      <IonButton :disabled="busy" @click="retryAcceptance">重试接取</IonButton>
    </div>
    <template v-if="world?.character && current">
      <div class="row-title">
        <strong
          >{{ world.character.name }} <small>Lv.{{ world.character.level }}</small></strong
        ><span class="gold">{{ world.character.gold }} 金币</span>
      </div>
      <div v-for="unit in lobby?.vitals" :key="unit.id" class="vital">
        <span>{{ unit.name }}</span
        ><small>生命 {{ unit.hp }}/{{ unit.maxHp }} · 魔力 {{ unit.mp }}/{{ unit.maxMp }}</small>
        <progress :max="unit.maxHp" :value="unit.hp" />
      </div>
      <WorldMap
        :current="current.id"
        :expanded="expanded"
        :places="world.content.places"
        :busy="busy"
        @select="selectPlace"
      />
      <p>{{ current.description }}</p>
      <SoloDungeonEntrance :active="active" :location="current.id" @busy="fieldSceneBusy = $event" />
      <SceneActions
        :active="active"
        :character-id="world.character.id"
        :location="current.id"
        :revision="world.character.revision"
        @busy="fieldSceneBusy = $event"
      />
      <section v-if="localNpcs.length" aria-label="在场人物">
        <h2>在场人物</h2>
        <IonButton
          v-for="npc in localNpcs"
          :key="npc.id"
          fill="clear"
          expand="block"
          :aria-label="npc.name + (marker(npc.id) ? '，' + marker(npc.id)?.label : '')"
          @click="openNpc(npc.id)"
        >
          <span class="npc-entry"
            ><strong>{{ npc.name }}</strong
            ><small>{{ npc.role }}</small></span
          >
          <span v-if="marker(npc.id)" class="quest-marker" :class="{ready: marker(npc.id)?.ready}" aria-hidden="true">{{
            marker(npc.id)?.symbol
          }}</span>
        </IonButton>
      </section>
      <QuestActions
        :actions="outsideQuests"
        :location="current.id"
        :busy="busy"
        :pending-acceptance="Boolean(interaction.pendingAcceptance.value)"
        @accept="accept"
        @interact="interact"
      />
      <QuestConversations :conversations="(conversations.data.value ?? []).filter((line) => !line.npcId)" />
      <PvpPlayers :active="active" :location="current.id" :allowed="current.services.includes('PVP')" />
      <div class="stack">
        <div v-for="encounter in lobby?.encounters" :key="encounter.id" class="encounter-entry">
          <IonButton :disabled="busy" expand="block" @click="start(encounter.id)">{{ encounter.name }}</IonButton>
          <p v-if="encounter.description" class="muted">{{ encounter.description }}</p>
        </div>
      </div>
    </template>
    <AppSheet v-model="npcOpen" :title="selectedNpc?.name ?? '交谈'" :busy="busy">
      <section v-if="selectedNpc && current && world?.character" data-npc-panel>
        <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
        <IonButton
          v-if="interaction.pendingAcceptance.value"
          :disabled="interaction.busy.value"
          @click="retryAcceptance"
          >重试接取</IonButton
        >
        <p class="muted">{{ selectedNpc.role }}</p>
        <p>{{ selectedNpc.description }}</p>
        <p class="npc-greeting">“{{ selectedNpc.greeting }}”</p>
        <QuestConversations :conversations="npcConversations" />
        <QuestActions
          :actions="localNpcQuests"
          :location="current.id"
          :busy="busy"
          :pending-acceptance="Boolean(interaction.pendingAcceptance.value)"
          @accept="accept"
          @interact="interact"
        />
        <SceneActions
          :key="selectedNpc.id"
          :active="active && npcOpen"
          :character-id="world.character.id"
          :location="current.id"
          :revision="world.character.revision"
          :npc-id="selectedNpc.id"
          @busy="npcSceneBusy = $event"
        />
        <AcademyCourses
          v-if="hasService('ACADEMY')"
          :active="active && npcOpen"
          :location="current.id"
          @enter="npcOpen = false"
          @busy="courseBusy = $event"
        />
        <WorkshopCourses
          v-if="hasService('WORKSHOP')"
          :active="active && npcOpen"
          @enter="npcOpen = false"
          @busy="courseBusy = $event"
        />
        <div class="stack">
          <IonButton v-if="hasService('RECOVERY') && lobby?.recovery" :disabled="busy" @click="battle.recover()"
            >全队免费恢复</IonButton
          >
          <IonButton v-if="hasService('STORAGE')" fill="outline" @click="visitService('/companions')"
            >伙伴仓库</IonButton
          >
          <IonButton v-if="hasService('SHOP')" fill="outline" @click="visitService('/shop')">商店</IonButton>
          <IonButton
            v-if="hasService('RESEARCH') || (hasService('RESEARCH_FOLLOWUP') && research.data.value?.available)"
            :disabled="!research.data.value?.available"
            fill="outline"
            @click="visitService('/evolution')"
            >进化研究所</IonButton
          >
          <p
            v-if="hasService('RESEARCH') && !research.data.value?.available && research.data.value?.requiredLevel"
            class="muted"
          >
            需要达到 {{ research.data.value.requiredLevel }} 级。
          </p>
          <IonButton
            v-if="hasService('ENHANCEMENT')"
            :disabled="!enhancement.data.value?.available"
            fill="outline"
            @click="visitService('/enhancement')"
            >装备强化{{ enhancement.data.value?.available ? '' : ' · 尚未开放' }}</IonButton
          >
          <p
            v-if="
              hasService('ENHANCEMENT') && !enhancement.data.value?.available && enhancement.data.value?.requiredLevel
            "
            class="muted"
          >
            需要达到 {{ enhancement.data.value.requiredLevel }} 级。
          </p>
          <IonButton
            v-if="hasService('MARKET')"
            :disabled="!market.data.value?.available"
            fill="outline"
            @click="visitService('/market')"
            >交易所</IonButton
          >
          <p
            v-if="hasService('MARKET') && !market.data.value?.available && market.data.value?.requiredLevel"
            class="muted"
          >
            需要达到 {{ market.data.value.requiredLevel }} 级。
          </p>
          <IonButton v-if="hasService('DUNGEON')" fill="outline" @click="visitService('/dungeon-lobby')"
            >副本招募</IonButton
          >
        </div>
      </section>
    </AppSheet>
    <AppSheet v-model="locationOpen" :busy="busy" :title="selected?.name ?? '地点'">
      <p>{{ selected?.description }}</p>
      <p v-if="receptionNames(selected)">{{ receptionNames(selected) }}</p>
      <template #footer>
        <IonButton v-if="selected && current?.exits.includes(selected.id)" :disabled="busy" @click="move()">
          前往{{ selected.name }}
        </IonButton>
        <span v-else class="muted">{{ selected?.id === current?.id ? '当前位置' : '道路尚未相连' }}</span></template
      >
    </AppSheet>
    <AppSheet v-model="petDeliveryOpen" :busy="busy" title="确认交付伙伴">
      <p>请选择{{ petDelivery?.amount }}只伙伴。确认后这些个体将离开你的队伍与仓库，无法取回。</p>
      <p>出战、替补或锁定伙伴不能交付；正在采集的伙伴需要先结束采集。</p>
      <label v-for="pet in petDelivery?.candidates" :key="pet.id" class="form-field">
        <input v-model="selectedPetIds" type="checkbox" :value="pet.id" :disabled="busy" />
        {{ pet.name }} · Lv.{{ pet.level }} · {{ pet.ability }} · {{ pet.skills.join('、') }} · 编号{{ pet.id }}
      </label>
      <p v-if="!petDelivery?.candidates.length">没有可交付的同种伙伴，请先调整编队或保护状态。</p>
      <AppNotice v-if="interaction.error.value" tone="danger">{{ interaction.error.value }}</AppNotice>
      <template #footer>
        <IonButton :disabled="busy || selectedPetIds.length !== petDelivery?.amount" @click="confirmPetDelivery"
          >确认交付选中伙伴</IonButton
        >
      </template>
    </AppSheet>
    <AppSheet v-model="dialogueOpen" title="对话"
      ><p>{{ interaction.dialogue.value?.dialogue }}</p>
      <p v-for="reward in interaction.dialogue.value?.rewards" :key="reward">{{ reward }}</p>
      <template #footer>
        <IonButton @click="dialogueOpen = false">继续</IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
<style scoped>
.npc-entry {
  display: flex;
  flex: 1;
  gap: 12px;
  align-items: center;
  text-align: left;
  white-space: normal;
}
.npc-entry small {
  color: var(--ion-color-medium);
}
.quest-marker {
  font-size: 28px;
  font-weight: 800;
  color: var(--ion-color-medium);
  margin-left: 12px;
}
.quest-marker.ready {
  color: var(--game-gold);
}
.gold {
  color: var(--game-gold);
  font-size: 13px;
}

.vital {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 5px 8px;
  margin-top: 12px;
  font-size: 12px;
}

.vital small {
  text-align: right;
}

.vital progress {
  grid-column: 1 / -1;
}

.encounter-entry p {
  margin: 6px 4px 0;
  font-size: 13px;
  line-height: 1.5;
}
</style>
