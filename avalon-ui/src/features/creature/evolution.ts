import {ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {OwnedCreature} from './types'

export type MaterialMode = 'UNBOUND_ONLY' | 'BOUND_FIRST'
export interface ResearchView {
    requiredLevel: number
    available: boolean
    taught: boolean
    selected: string | null
    tokens: number
    offers: {code: string; name: string; price: number}[]
    routes: {
        source: string
        target: string
        name: string
        level: number
        materials: {code: string; name: string; quantity: number}[]
    }[]
}
export interface EvolutionPreview {
    before: OwnedCreature
    bound: boolean
    customName: boolean
    target: string
    name: string
    mode: MaterialMode
    materials: {quantities: Record<string, number>; bindsTarget: boolean}
    resultBound: boolean
}
interface EvolutionResult {
    id: string
    species: string
    name: string
    bound: boolean
}

/** 入口资格完全来自服务端，地图页面不复制设施与等级配置。 */
export function useEvolutionResearch(active: Ref<boolean>) {
    return useQuery({
        queryKey: ['evolution-research'],
        enabled: active,
        queryFn: () => api<ResearchView>('/api/creatures/evolution/research'),
    })
}

/** 保留失败请求的完整预览与请求号，重试不能在后台悄悄换材料或换目标。 */
export function useEvolutionActions() {
    const action = useAction(true),
        client = useQueryClient()
    const preview = ref<EvolutionPreview | null>(null),
        result = ref<EvolutionResult | null>(null)
    let pending: {requestId: string; expected: EvolutionPreview} | undefined
    let exchangePending:
        {requestId: string; code: string; quantity: number; unitPrice: number; acceptBinding: true} | undefined
    const exchangeMessage = ref('')

    async function exchange(offer: ResearchView['offers'][number], quantity: number) {
        if (!Number.isSafeInteger(quantity) || quantity <= 0) return
        if (
            !exchangePending ||
            exchangePending.code !== offer.code ||
            exchangePending.quantity !== quantity ||
            exchangePending.unitPrice !== offer.price
        ) {
            exchangePending = {
                requestId: crypto.randomUUID(),
                code: offer.code,
                quantity,
                unitPrice: offer.price,
                acceptBinding: true,
            }
        }
        const submission = exchangePending
        exchangeMessage.value = ''
        return action.run(async () => {
            await api('/api/creatures/evolution/exchange', 'POST', submission)
            exchangePending = undefined
            exchangeMessage.value = `已兑换${offer.name} ×${quantity}，物品已绑定。`
            await client.invalidateQueries()
        })
    }

    async function teach(source: string) {
        return action.run(async () => {
            await api('/api/creatures/evolution/teaching', 'POST', {source, acceptBinding: true})
            await client.invalidateQueries()
        })
    }

    async function prepare(id: string, mode: MaterialMode | '') {
        preview.value = null
        pending = undefined
        result.value = null
        return action.run(async () => {
            preview.value = await api<EvolutionPreview>(`/api/creatures/${id}/evolution${mode ? '?mode=' + mode : ''}`)
        })
    }

    async function evolve() {
        if (!preview.value) return
        pending ??= {
            requestId: crypto.randomUUID(),
            expected: JSON.parse(JSON.stringify(preview.value)) as EvolutionPreview,
        }
        const submission = pending
        return action.run(async () => {
            result.value = await api<EvolutionResult>(
                `/api/creatures/${submission.expected.before.id}/evolution`,
                'POST',
                submission,
            )
            pending = undefined
            preview.value = null
            await client.invalidateQueries()
        })
    }

    return {...action, preview, result, exchangeMessage, teach, prepare, evolve, exchange}
}
