import {lifeActivities} from './activities'
import {type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {type ProductionView} from './production'

interface FoodView {
    inventory: ProductionView['inventory']
    foods: {item: string; health: number; mana: number}[]
    targets: {id: string; name: string; health: number; mana: number; maximumHealth: number; maximumMana: number}[]
}
export interface FoodResult {
    restoredHealth: number
    restoredMana: number
}
export function useFood(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: lifeActivities.food.queryKey,
        enabled: active,
        queryFn: () => api<FoodView>(lifeActivities.food.endpoint),
    })
    const action = useAction(true),
        client = useQueryClient()
    let pending: {key: string; requestId: string} | undefined
    async function eat(item: string, target: string): Promise<FoodResult | null> {
        if (action.busy.value) return null
        const key = JSON.stringify([item, target])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        let result: FoodResult | null = null
        await action.run(async () => {
            result = await api<FoodResult>(lifeActivities.food.endpoint, 'POST', {
                requestId: pending!.requestId,
                item,
                target,
            })
            pending = undefined
            await client.invalidateQueries()
        })
        return result
    }
    return {...action, query, eat}
}
