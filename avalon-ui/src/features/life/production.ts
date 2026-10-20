import {lifeActivities} from './activities'
import {type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface ProductionView {
    sellPrices: Record<string, number>
    canSell: boolean
    offers: {item: string; name: string; price: number; locations: string[]; available: boolean}[]
    recipes: {
        id: string
        skill: string
        level: number
        minimumToolTier: number
        product: string
        name: string
        quantity: number
        points: number
        skillLevel: number
        proficiencyGain: number | null
        ingredients: {code: string; name: string; quantity: number}[]
        locations: string[]
        available: boolean
        blueprintName: string | null
    }[]
    inventory: {
        id: string
        code: string
        name: string
        quantity: number
        bound: boolean
        equipped: boolean
        enhancementLevel: number
    }[]
    tools: {item: string; kind: string; tier: number; breakDenominator: number}[]
}
export interface CraftResult {
    completed: number
    produced: number
    broken: number
    stop: string
    proficiency: number
}
export function useProduction(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: lifeActivities.production.queryKey,
        enabled: active,
        queryFn: () => api<ProductionView>(lifeActivities.production.endpoint),
    })
    const action = useAction(true),
        client = useQueryClient()
    let pending: {key: string; requestId: string} | undefined
    async function buy(item: string, quantity: number) {
        if (action.busy.value) return false
        const key = JSON.stringify(['BUY', item, quantity])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api(`${lifeActivities.production.endpoint}/buy`, 'POST', {
                requestId: pending!.requestId,
                item,
                quantity,
            })
            pending = undefined
            await client.invalidateQueries()
        })
    }
    async function sell(item: string, quantity: number) {
        if (action.busy.value) return false
        const key = JSON.stringify(['SELL', item, quantity])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api(`${lifeActivities.production.endpoint}/sell`, 'POST', {
                requestId: pending!.requestId,
                item,
                quantity,
            })
            pending = undefined
            await client.invalidateQueries()
        })
    }
    async function craft(
        recipe: string,
        quantity: number,
        tool: string,
        tools: number,
        materials: Record<string, number>,
    ) {
        if (action.busy.value) return null
        const key = JSON.stringify([recipe, quantity, tool, tools, Object.entries(materials).sort()])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        let result: CraftResult | null = null
        await action.run(async () => {
            result = await api<CraftResult>(lifeActivities.production.endpoint, 'POST', {
                requestId: pending!.requestId,
                recipe,
                quantity,
                tool,
                tools,
                materials,
            })
            pending = undefined
            await client.invalidateQueries()
        })
        return result
    }
    return {...action, query, craft, buy, sell}
}
