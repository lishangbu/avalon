/**
 * 移动命令提交目的地与预期 revision。无论成功或冲突都使世界缓存失效，以便下一次操作使用真实位置；失败返回 false，不应自动关闭地点弹窗。
 */
import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export function useMovement() {
    const client = useQueryClient()
    const action = useAction(true)

    async function move(destination: string, expectedRevision: number) {
        return action.run(async () => {
            // 包括版本冲突在内，移动失败也重新读取真实位置，不能让旧地图继续提交旧 revision。
            try {
                await api('/api/world/move', 'POST', {destination, expectedRevision})
            } finally {
                // 抵达事件可能同时更新任务、经验和奖励，不能只刷新地图位置。
                await client.invalidateQueries()
            }
        })
    }

    return {...action, move}
}
