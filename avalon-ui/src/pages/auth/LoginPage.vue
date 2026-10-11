<script lang="ts" setup>
/** 身份令牌由认证模块接收；页面只保存输入，登录后的入口由服务器角色状态决定。 */
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {IonButton, IonIcon} from '@ionic/vue'
import {compassOutline} from 'ionicons/icons'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {signIn} from '@/features/auth/api'
import {useAction} from '@/shared/useAction'

const router = useRouter()
const username = ref(''),
  password = ref(''),
  register = ref(false)
const {busy, error, run} = useAction()

async function submit() {
  // 新账号的词表检查由服务端负责，登录旧账号不会复查账号名；密码不参与检测。
  // run捕获拒绝消息并返回false，失败时保留账号和密码供修改，仅成功后清空密码及跳转。
  if (await run(() => signIn(username.value, password.value, register.value))) {
    password.value = ''
    await router.replace('/world')
  }
}
</script>
<template>
  <AppPage locked title="阿瓦隆">
    <div class="arrival">
      <IonIcon :icon="compassOutline" />
      <h1>{{ register ? '创建账号' : '重返阿瓦隆' }}</h1>
    </div>
    <AppNotice v-if="error" tone="danger">{{ error }}</AppNotice>
    <form @submit.prevent="submit">
      <label class="form-field" for="username"
        >账号<input
          id="username"
          v-model="username"
          :spellcheck="false"
          autocapitalize="none"
          autocomplete="username"
          maxlength="32"
          minlength="3"
          required
      /></label>
      <label class="form-field" for="password"
        >密码<input
          id="password"
          v-model="password"
          :autocomplete="register ? 'new-password' : 'current-password'"
          minlength="10"
          required
          type="password"
      /></label>
      <p v-if="register">密码至少 10 位，最长 72 个 UTF-8 字节。</p>
      <IonButton :disabled="busy" expand="block" type="submit"
        >{{ busy ? '正在连接' : register ? '注册并进入' : '进入冒险' }}
      </IonButton>
      <IonButton :disabled="busy" expand="block" fill="clear" @click="register = !register">
        {{ register ? '返回登录' : '创建账号' }}
      </IonButton>
    </form>
  </AppPage>
</template>
<style scoped>
.arrival {
  padding: 36px 0 20px;
  text-align: center;
}

.arrival ion-icon {
  font-size: 56px;
  color: var(--ion-color-primary);
}
</style>
