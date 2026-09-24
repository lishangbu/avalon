<script lang="ts" setup>
import ContentLink from '@/features/content/ContentLink.vue'
/** 角色页展示服务器档案；主题独立于会话保存，退出账号不会重置设备偏好。 */
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppSheet from '@/shared/ui/AppSheet.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {theme} from '@/app/theme'
import {useWorld} from '@/features/world/queries'
import {listDevices, revokeDevice, signOut} from '@/features/auth/api'
import {useAction} from '@/shared/useAction'
import {usePageActive} from '@/shared/usePageActive'
import {useCharacterCombatProfile} from '@/features/battle/profile'
import {statNames} from '@/features/inventory/labels'

const router = useRouter(),
  client = useQueryClient(),
  active = usePageActive()
const {data: world} = useWorld()
const devices = useQuery({queryKey: ['devices'], queryFn: listDevices, enabled: active})
const profile = useCharacterCombatProfile(active)
const {busy, error, run} = useAction()
const logoutOpen = ref(false)

async function revoke(device: Awaited<ReturnType<typeof listDevices>>[number]) {
  await run(async () => {
    await revokeDevice(device)
    await client.invalidateQueries({queryKey: ['devices']})
  })
}
</script>
<template>
  <AppPage tab="/character" title="角色">
    <AppNotice v-if="error || devices.error.value || profile.error.value" tone="danger">
      {{ error || devices.error.value?.message || profile.error.value?.message }}
    </AppNotice>
    <template v-if="world?.character">
      <h1>{{ world.character.name }}</h1>
      <p>
        {{ world.content.professions.find((job) => job.id === world?.character?.profession)?.name }} · Lv.{{
          world.character.level
        }}
      </p>
      <div class="stat-grid">
        <div>
          累计经验<strong>{{ world.character.experience }}</strong>
        </div>
        <div>
          金币<strong>{{ world.character.gold }}</strong>
        </div>
      </div>
      <IonButton class="equipment-link" expand="block" fill="outline" @click="router.push('/inventory?tab=equipment')">
        查看装备
      </IonButton>
    </template>
    <template v-if="profile.data.value">
      <section aria-label="常驻属性">
        <h2>常驻属性</h2>
        <dl class="profile-stats">
          <div v-for="(value, key) in profile.data.value.stats" :key="key">
            <dt>{{ statNames[key] }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </section>
      <section aria-label="职业技能">
        <h2>职业技能</h2>
        <article v-for="skill in profile.data.value.skills" :key="skill.name" class="row">
          <div class="row-title">
            <ContentLink kind="skill" :code="skill.name" /><span>{{ skill.element }}</span>
          </div>
          <p>魔力 {{ skill.mana }} · {{ skill.cooldown ? `冷却 ${skill.cooldown} 回合` : '无冷却' }}</p>
        </article>
      </section>
    </template>
    <h2>外观</h2>
    <label class="form-field" for="theme"
      >主题<select id="theme" v-model="theme">
        <option value="system">跟随系统</option>
        <option value="light">浅色</option>
        <option value="dark">深色</option>
      </select></label
    >
    <h2>登录设备</h2>
    <div v-for="device in devices.data.value" :key="device.id" class="row">
      <div class="row-title">
        <strong>{{ device.label }}</strong
        ><small v-if="device.current">当前设备</small>
      </div>
      <IonButton :disabled="busy" color="danger" fill="clear" @click="revoke(device)">
        {{ device.current ? '退出此设备' : '撤销登录' }}
      </IonButton>
    </div>
    <IonButton
      :disabled="busy"
      class="equipment-link"
      color="danger"
      expand="block"
      fill="outline"
      @click="logoutOpen = true"
      >退出登录
    </IonButton>
    <AppSheet v-model="logoutOpen" :busy="busy" title="退出登录"
      ><p>确定退出当前账号？</p>
      <template #footer>
        <IonButton :disabled="busy" fill="outline" @click="logoutOpen = false">取消</IonButton>
        <IonButton :disabled="busy" color="danger" @click="run(signOut)">退出登录</IonButton>
      </template>
    </AppSheet>
  </AppPage>
</template>
<style scoped>
.equipment-link {
  margin-top: 20px;
}

.profile-stats {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.profile-stats div {
  padding: 12px 0;
  border-bottom: 1px solid var(--game-border);
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
}

.profile-stats dt {
  color: var(--ion-color-medium);
}

.profile-stats dd {
  margin: 0;
  font-weight: 600;
}
</style>
