import type {Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

/** 宝箱编号就是领取幂等键；失败重试不会重新抽奖，也不会生成第二份奖励。 */
export function useChests(enabled: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['inventory', 'chests'],
        queryFn: () => api<{id: string; name: string; createdAt: string}[]>('/api/inventory/chests'),
        enabled,
    })
    const action = useAction(true)
    const client = useQueryClient()
    async function claim(id: string) {
        return action.run(async () => {
            await api(`/api/inventory/chests/${id}/claim`, 'POST')
            await client.invalidateQueries()
        })
    }
    return {...action, query, claim}
}
