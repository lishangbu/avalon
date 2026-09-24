import {type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {type CraftResult, type ProductionView} from './production'

export interface FarmView {
    atFarm: boolean
    learned: boolean
    level: number
    maximumPlots: number
    expansionGold: number
    plots: {
        index: number
        crop: string | null
        name: string | null
        bound: boolean
        watered: boolean
        mature: boolean
        remainingSeconds: number
    }[]
    crops: {id: string; name: string; seed: string; level: number; growthSeconds: number; quantity: number}[]
    inventory: ProductionView['inventory']
    tools: ProductionView['tools']
}
export function useFarm(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['life-farm'],
        enabled: active,
        queryFn: () => api<FarmView>('/api/life/farm'),
        refetchInterval: 10000,
    })
    const action = useAction(true),
        client = useQueryClient()
    let pending: {key: string; requestId: string} | undefined
    async function perform(
        operation: string,
        plots: number[],
        planting?: {crop: string; seed: string; tool: string; tools: number},
    ): Promise<CraftResult | true | null> {
        if (action.busy.value) return null
        const managed = operation === 'WATER' || operation === 'EXPAND'
        const body = {
            action: operation,
            plots: [...plots].sort((a, b) => a - b),
            ...(managed ? {} : (planting ?? {crop: null, seed: null, tool: null, tools: 0})),
        }
        const key = JSON.stringify(body)
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        let result: CraftResult | true | null = null
        await action.run(async () => {
            const response = await api<CraftResult>(`/api/life/farm/${managed ? 'manage' : 'produce'}`, 'POST', {
                ...body,
                requestId: pending!.requestId,
            })
            result = managed ? true : response
            pending = undefined
            await client.invalidateQueries()
        })
        return result
    }
    return {...action, query, perform}
}
