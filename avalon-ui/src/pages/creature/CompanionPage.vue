<script lang="ts" setup>
/** 路由编号只用于选择已授权列表中的个体，仓库存取和保护规则仍由服务端执行。 */
import {computed, ref, useId} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import ReleaseSheet from '@/features/creature/ReleaseSheet.vue'
import CreaturePortrait from '@/features/creature/CreaturePortrait.vue'
import RenameSheet from '@/features/creature/RenameSheet.vue'
import {useCreature, useCreatureProvenance, useEncyclopedia} from '@/features/creature/queries'
import {usePlacement} from '@/features/creature/actions'
import {useWorld} from '@/features/world/queries'
import {usePageActive} from '@/shared/usePageActive'
import type {OwnedCreature} from '@/features/creature/types'
import ContentLink from '@/features/content/ContentLink.vue'
import ContentDetails from '@/features/content/ContentDetails.vue'

const route = useRoute(),
  router = useRouter(),
  active = usePageActive()
// Ionic 会保留详情页面实例，创建时固定编号，不能随另一个详情路由改变旧页面身份。
const id = String(route.params.id)
const creatures = useCreature(active),
  world = useWorld(),
  placement = usePlacement()
const pet = computed(() => creatures.data.value?.find((item) => item.id === id))
const encyclopedia = useEncyclopedia(active)
const species = computed(() => encyclopedia.data.value?.find((item) => item.species === pet.value?.species))
const provenance = useCreatureProvenance(
  id,
  computed(() => active.value && !!pet.value),
)
const originNames: Record<string, string> = {CAPTURE: '野外捕获', GIFT: '任务赠送', UNKNOWN: '未知'}
const position = ref(0),
  releasing = ref<OwnedCreature[] | null>(null)
const positionId = useId()
const renaming = ref(false)
const skillDetailsOpen = ref(false)
const skillInspection = useAction(true)
const queryClient = useQueryClient()

/** 明确查看技能后提交行为凭据；普通详情GET保持只读，不因页面预取自动过教学。 */
async function inspectSkills() {
  if (
    await skillInspection.run(async () => {
      await api(`/api/creatures/${encodeURIComponent(id)}/skills/viewed`, 'POST')
      await queryClient.invalidateQueries({queryKey: ['quests']})
    })
  )
    skillDetailsOpen.value = true
}
const positionName = computed(() => {
  const current = pet.value?.partyPosition
  if (current === undefined) return ''
  return current >= 100
    ? '仓库'
    : (['第一出战位', '第二出战位', '第一替补位', '第二替补位'][current] ?? `携带位${current - 3}`)
})
const atCenter = computed(() =>
  world.data.value?.content.places
    .find((place) => place.id === world.data.value?.character?.location)
    ?.services.includes('STORAGE'),
)
const positions = computed(() =>
  Array.from({length: 5 + Math.floor((world.data.value?.character?.level ?? 1) / 10)}, (_, index) => index),
)

function release() {
  if (pet.value) releasing.value = [{...pet.value, skills: [...pet.value.skills]}]
}
</script>
<template>
  <AppPage :title="pet?.name ?? '伙伴详情'" back="/companions">
    <AppNotice v-if="placement.error.value || creatures.error.value || provenance.error.value" tone="danger">
      {{ placement.error.value || creatures.error.value?.message || provenance.error.value?.message }}
    </AppNotice>
    <template v-if="pet">
      <AppNotice v-if="skillInspection.error.value" tone="danger">{{ skillInspection.error.value }}</AppNotice>
      <CreaturePortrait :source="species?.image" :name="species?.name ?? pet.name" large />
      <h1>{{ pet.name }}</h1>
      <ContentDetails kind="creature" :code="pet.id" />
      <p>Lv.{{ pet.level }} · {{ pet.element }} · 累计经验 {{ pet.experience }}</p>
      <p>当前位置：{{ positionName }}</p>
      <template v-if="provenance.data.value">
        <p>{{ provenance.data.value.bound ? '已绑定，不能交易' : '未绑定；交易仍需满足编队与锁定等条件' }}</p>
        <p>初始来源：{{ originNames[provenance.data.value.originKind] ?? '未知' }}</p>
        <p>原获得者：{{ provenance.data.value.originName ?? '未知' }}</p>
        <p>原捕获者：{{ provenance.data.value.captorName ?? '无捕获记录' }}</p>
      </template>
      <h2>特性</h2>
      <p>{{ pet.ability }} · {{ pet.abilitySlot === 'H' ? '隐藏' : '普通' }}</p>
      <ContentLink kind="ability" :code="pet.ability" name="查看特性效果" />
      <h2>技能 {{ pet.skills.length }} / 7</h2>
      <div v-for="skill in pet.skills" :key="skill" class="row">
        <ContentLink kind="skill" :code="skill" />
      </div>
      <IonButton :disabled="skillInspection.busy.value" fill="outline" @click="inspectSkills">查看已学技能</IonButton>
      <p v-if="skillDetailsOpen">
        以上为这只伙伴当前已学技能，共{{
          pet.skills.length
        }}项。学习其他技能前，可在学习页面查看本物种技能池和遗忘规则。
      </p>
      <IonButton
        class="section-action"
        expand="block"
        fill="outline"
        @click="router.push('/companions/' + id + '/learning')"
        >学习技能
      </IonButton>
      <template v-if="pet.partyPosition < 100 || atCenter"
        ><h2>编队</h2>
        <label :for="positionId" class="form-field"
          >目标位置<select :id="positionId" v-model.number="position" :disabled="placement.busy.value">
            <option v-for="item in positions" :key="item" :value="item">
              {{ ['第一出战位', '第二出战位', '第一替补位', '第二替补位'][item] ?? `携带位${item - 3}` }}
            </option>
          </select></label
        >
        <IonButton :disabled="placement.busy.value" @click="placement.place(id, position)"
          >{{ pet.partyPosition >= 100 ? '取出并编队' : '调整位置' }}
        </IonButton>
      </template>
      <AppNotice v-else>前往伙伴中心后可取出伙伴。</AppNotice>
      <h2>管理</h2>
      <div class="stack">
        <IonButton :disabled="placement.busy.value" fill="outline" @click="renaming = true">改名</IonButton>
        <IonButton :disabled="placement.busy.value" fill="outline" @click="placement.manage(id, 'lock', !pet.locked)">
          {{ pet.locked ? '解锁' : '锁定' }}
        </IonButton>
        <IonButton
          v-if="atCenter && pet.partyPosition < 100"
          :disabled="placement.busy.value"
          fill="outline"
          @click="placement.manage(id, 'deposit')"
          >存入仓库
        </IonButton>
        <IonButton
          v-if="pet.partyPosition >= 4 && !pet.locked"
          :disabled="placement.busy.value"
          color="danger"
          fill="clear"
          @click="release"
          >放生
        </IonButton>
      </div>
    </template>
    <p v-else-if="!creatures.isPending.value" class="empty">未找到该伙伴</p>
    <ReleaseSheet v-model="releasing" @released="router.replace('/companions')" />
    <RenameSheet v-if="renaming" :id="id" @closed="renaming = false" />
  </AppPage>
</template>
<style scoped>
.section-action {
  margin-top: 16px;
}
</style>
