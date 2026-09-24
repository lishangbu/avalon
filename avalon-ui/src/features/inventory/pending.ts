import {computed, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface PendingItem {
    code: string
    id: string
    name: string
    quantity: number
    bound: boolean
    source: string
}

/** 领取指定数量，重试同一选择保留请求号，成功后刷新背包与待领列表。 */
export function usePendingItems(visible: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['inventory-pending'],
        enabled: computed(() => visible.value),
        queryFn: () => api<PendingItem[]>('/api/inventory/pending'),
    })
    const action = useAction(true)
    const client = useQueryClient()
    let pending: {id: string; quantity: number; requestId: string} | undefined

    async function claim(id: string, quantity: number) {
        if (!pending || pending.id !== id || pending.quantity !== quantity)
            pending = {id, quantity, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api(`/api/inventory/pending/${id}/claim`, 'POST', pending)
            pending = undefined
            await client.invalidateQueries()
        })
    }

    return {...action, query, claim}
}
