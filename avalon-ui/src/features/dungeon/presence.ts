/** 战间显式续期；离开页面或切到后台后停止，恢复在线不会替玩家点击准备。 */
import {onScopeDispose, ref, watch, type Ref} from 'vue'
import {api} from '@/shared/http'

export function useDungeonPresence(id: string, enabled: Ref<boolean>) {
    const error = ref('')
    let timer: ReturnType<typeof setTimeout> | undefined
    let generation = 0

    watch(
        enabled,
        (active) => {
            const current = ++generation
            clearTimeout(timer)
            error.value = ''
            if (!active) return
            async function renew() {
                try {
                    await api(`/api/dungeon/runs/${id}/presence`, 'POST')
                    if (current === generation) error.value = ''
                } catch {
                    if (current === generation) error.value = '连接中断，准备状态可能已失效'
                } finally {
                    // 请求结束后再安排下一次，防止慢网络下积压续期写入。
                    if (current === generation) timer = setTimeout(renew, 5000)
                }
            }
            void renew()
        },
        {immediate: true},
    )

    onScopeDispose(() => {
        generation++
        clearTimeout(timer)
    })
    return error
}
