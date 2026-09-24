import {computed, ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface CraftingView {
    usedSlots: number
    capacity: number
    recipes: {
        id: string
        product: string
        quantity: number
        gold: number
        fragments: {id: string; name: string; quantity: number; bound: boolean}[]
    }[]
}

/** 预览只在弹窗可见时读取；消费失败重试保留同一负载的请求号。 */
export function useCrafting(visible: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['crafting'],
        enabled: computed(() => visible.value),
        queryFn: () => api<CraftingView>('/api/inventory/crafting'),
    })
    const action = useAction(true)
    const client = useQueryClient()
    const result = ref<{name: string; bound: boolean} | null>(null)
    let pending: {key: string; requestId: string} | undefined

    async function craft(recipe: string, fragments: Record<string, number>) {
        const key = JSON.stringify([recipe, Object.entries(fragments).sort()])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            result.value = await api('/api/inventory/crafting', 'POST', {
                recipe,
                fragments,
                requestId: pending!.requestId,
            })
            pending = undefined
            await client.invalidateQueries()
        })
    }

    return {...action, query, result, craft}
}
