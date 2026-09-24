/** 缓存页面离开或浏览器切到后台时停用查询与倒计时，恢复后由查询重新确认服务端状态。 */
import {computed, onScopeDispose, ref} from 'vue'
import {onIonViewDidEnter, onIonViewWillLeave} from '@ionic/vue'

export function usePageActive() {
    const entered = ref(true)
    const visible = ref(document.visibilityState !== 'hidden')

    function visibilityChanged() {
        visible.value = document.visibilityState !== 'hidden'
    }

    document.addEventListener('visibilitychange', visibilityChanged)
    onScopeDispose(() => document.removeEventListener('visibilitychange', visibilityChanged))
    onIonViewDidEnter(() => {
        entered.value = true
    })
    onIonViewWillLeave(() => {
        entered.value = false
    })
    return computed(() => entered.value && visible.value)
}
