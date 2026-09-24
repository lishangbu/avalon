/** 启动只装配应用；身份失效清除服务器缓存，并由应用壳销毁缓存页面。 */
import {createApp, watch} from 'vue'
import {IonicVue} from '@ionic/vue'
import {VueQueryPlugin} from '@tanstack/vue-query'
import App from './App.vue'
import {router} from './router'
import {queryClient} from '@/app/query-client'
import {installTheme} from '@/app/theme'
import {configureAuthentication} from '@/shared/http'
import {
    authenticated,
    clearSession,
    currentAccessToken,
    restoreSession,
    sessionGeneration,
} from '@/features/auth/session'
import '@ionic/vue/css/core.css'
import '@ionic/vue/css/normalize.css'
import '@ionic/vue/css/structure.css'
import '@ionic/vue/css/typography.css'
import '@ionic/vue/css/palettes/dark.class.css'
import '@/styles/base.css'

installTheme()
configureAuthentication({
    token: currentAccessToken,
    restore: restoreSession,
    clear: clearSession,
    generation: sessionGeneration,
})
watch(
    authenticated,
    (signedIn) => {
        if (!signedIn) {
            queryClient.clear()
            if (router.currentRoute.value.matched.length) void router.replace('/login')
        }
    },
    {flush: 'sync'},
)
const app = createApp(App).use(IonicVue, {mode: 'ios'}).use(VueQueryPlugin, {queryClient}).use(router)
void router.isReady().then(() => app.mount('#app'))
