<script lang="ts" setup>
/** 职业目录完全来自后端，创角成功后重新读取角色，避免客户端伪造初始状态。 */
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {useWorld} from '@/features/world/queries'
import {useCharacterCreation} from '@/features/character/actions'
import {signOut} from '@/features/auth/api'

const router = useRouter()
const {data: world} = useWorld()
const name = ref(''),
  profession = ref('WARRIOR')
const {busy, error, create, run} = useCharacterCreation()

async function submit() {
  // 将玩家原文提交给服务端统一检查，不在客户端复制词表或自动替换字符。
  // 只有创建成功才离开表单；违禁词拒绝时保留名称、职业和可重试入口。
  if (await create(name.value, profession.value)) await router.replace('/world')
}
</script>
<template>
  <AppPage locked title="创建角色">
    <h1>启程</h1>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <form v-if="world" @submit.prevent="submit">
      <label class="form-field" for="character-name"
        >角色名<input id="character-name" v-model="name" maxlength="16" minlength="2" required
      /></label>
      <label class="form-field" for="profession"
        >职业<select id="profession" v-model="profession">
          <option v-for="job in world.content.professions" :key="job.id" :value="job.id">{{ job.name }}</option>
        </select></label
      >
      <p>{{ world.content.professions.find((job) => job.id === profession)?.description }}</p>
      <AppNotice>职业确定后不能更换。</AppNotice>
      <IonButton :disabled="busy" expand="block" type="submit">踏上旅途</IonButton>
      <IonButton :disabled="busy" expand="block" fill="clear" @click="run(signOut)">退出登录</IonButton>
    </form>
  </AppPage>
</template>
