/** 倒计时仅作显示，截止后的结算由服务器执行；离开缓存页面立即停止本地时钟。 */
import {onScopeDispose, ref, type Ref, watch} from 'vue'

export function usePageClock(active: Ref<boolean>) {
    const now = ref(Date.now())
    let timer: ReturnType<typeof setInterval> | undefined

    function stop() {
        if (timer !== undefined) clearInterval(timer)
        timer = undefined
    }

    watch(
        active,
        (enabled) => {
            stop()
            if (enabled) {
                now.value = Date.now()
                timer = setInterval(() => {
                    now.value = Date.now()
                }, 1000)
            }
        },
        {immediate: true},
    )
    onScopeDispose(stop)
    return now
}
