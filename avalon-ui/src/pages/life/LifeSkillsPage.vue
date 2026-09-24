<script setup lang="ts">
import {computed, ref} from 'vue'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {usePageActive} from '@/shared/usePageActive'
import {useLifeSkills, type LifeSkill} from '@/features/life/skills'

const skills = useLifeSkills(usePageActive())
const forgetting = ref<LifeSkill | null>(null)
const used = computed(() => skills.query.data.value?.filter((skill) => skill.learned && skill.profession).length ?? 0)
const introductions: Record<string, string> = {
  MINING: '使用镐采集矿石，供制造工具与装备。初级资源位于山洞入口。',
  LOGGING: '使用斧采集木材，供制造工具与武器。初级资源位于东侧林口。',
  HERBALISM: '使用钳采集药草，供制药与生产辅料。初级资源位于河边小路。',
  HUNTING: '使用夹狩猎，获得皮革或肉类，供皮匠与烹饪。初级资源位于平原路标。',
  ARTISAN: '在诺姆锻造坊使用锤制造四职业武器，以及镐、斧、夹、锅和工匠部件。',
  LEATHERWORK: '在诺姆皮革坊使用针制造防具，以及钳、棒、锤和皮革包覆。',
  ALCHEMY: '在诺姆制药室使用棒制造药剂、鱼油与处理剂，以及针、鱼竿和锄头。',
  COOKING: '在诺姆厨房使用锅制作恢复生命或魔法的料理，以及高阶制造辅料。',
  FARMING: '在村庄广场或诺姆城种植，使用锄头播种、浇水后开始生长，离线也会成熟。',
  FISHING: '使用鱼竿与鱼饵，按住上升、松开下降。初级水域在河边小路，鱼获可做料理或提炼鱼油。',
}
async function forget() {
  if (forgetting.value && (await skills.change(forgetting.value.code, true))) forgetting.value = null
}
</script>
<template>
  <AppPage title="生活技能" back="/more">
    <IonButton router-link="/life/gathering" fill="outline">野外采集</IonButton>
    <IonButton router-link="/life/farm" fill="outline">我的农田</IonButton>
    <IonButton router-link="/life/food" fill="outline">食用料理</IonButton>
    <IonButton router-link="/life/fishing" fill="outline">钓鱼</IonButton>
    <IonButton router-link="/life/production" fill="outline">生活制造</IonButton>
    <p>人物6级开放。专业名额 {{ used }} / 3；种植与钓鱼不占名额。</p>
    <details>
      <summary>工具、熟练度与专业合作</summary>
      <p>首次学习赠送对应木工具；遗忘重学不重复赠送。木工具可在村庄补给店或诺姆城商店补购。</p>
      <p>木、铜、铁、银、金工具分别覆盖技能1～2、3～4、5～6、7～8、9～10级。成功操作后工具可能永久报废，不能修理。</p>
      <p>适龄内容获得完整熟练度，低一级减半，低两级不再增长。技能等级独立于人物等级；高级资源仍需要到对应地图。</p>
      <p>铁工具起需要不同专业制作的辅料。人物11级可到诺姆城商店的交易所完成教学，与其他玩家交易未绑定材料和成品。</p>
      <p>野外采集需派遣一只随身伙伴，消耗其固定生命和魔法；保持在线前台，结束派遣后才能离开或战斗。</p>
    </details>
    <AppNotice v-if="skills.error.value || skills.query.error.value" tone="danger">
      {{ skills.error.value || skills.query.error.value?.message }}
    </AppNotice>
    <p v-if="skills.query.isPending.value">正在读取生活技能…</p>
    <section v-for="skill in skills.query.data.value" :key="skill.code" class="life-skill">
      <h2>
        {{ skill.name }} <small>{{ skill.profession ? '专业' : '日常技能' }}</small>
      </h2>
      <p>{{ introductions[skill.code] }}</p>
      <template v-if="skill.learned">
        <p>
          等级 {{ skill.level }} / 10 · 累计熟练度 {{ skill.proficiency
          }}<span v-if="skill.nextThreshold !== null"> / {{ skill.nextThreshold }}</span
          ><span v-else> · 已满级</span>
        </p>
        <IonButton fill="outline" color="medium" :disabled="skills.busy.value" @click="forgetting = skill"
          >遗忘</IonButton
        >
      </template>
      <IonButton
        v-else
        :disabled="skills.busy.value || (skill.profession && used >= 3)"
        @click="skills.change(skill.code, false)"
        >学习{{ skill.name }}</IonButton
      >
    </section>
    <section
      v-if="forgetting"
      role="alertdialog"
      aria-labelledby="forget-title"
      aria-describedby="forget-description"
      class="forget-confirmation"
    >
      <h2 id="forget-title">遗忘{{ forgetting.name }}？</h2>
      <p id="forget-description">熟练度和已解锁配方将清空，物品保留。重新学习需要从1级开始。</p>
      <IonButton color="danger" :disabled="skills.busy.value" @click="forget">确认遗忘</IonButton>
      <IonButton fill="outline" :disabled="skills.busy.value" @click="forgetting = null">取消</IonButton>
    </section>
  </AppPage>
</template>
<style scoped>
.life-skill {
  padding-block: 0.75rem;
  border-bottom: 1px solid var(--ion-color-step-200, #ccc);
}
small {
  font-size: 0.75em;
  font-weight: normal;
}
.forget-confirmation {
  position: sticky;
  bottom: 0;
  padding: 1rem;
  background: var(--ion-background-color, white);
  border: 2px solid var(--ion-color-danger);
  border-radius: 0.5rem;
}
</style>
