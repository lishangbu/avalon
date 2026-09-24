import {computed, ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {OwnedItem} from '@/features/inventory/types'

export interface MarketAccess {
    requiredLevel: number
    available: boolean
    taught: boolean
    activeListings: number
    pendingReturns: boolean
}
export interface Listing {
    id: string
    assetId: string
    kind: string
    state: string
    unitPrice: string
    originalQuantity: number
    remaining: number
    expiresAt: string
    name: string
    category: string
    enhancementLevel: number | null
}
export interface MarketPage {
    page: number
    size: number
    hasMore: boolean
    listings: Listing[]
}
export interface Detail {
    listing: Listing
    item: OwnedItem | null
    creature: CreatureDetail | null
    own: boolean
    sets: {
        name: string
        healthPieces: number
        weapons: string[]
        weaponHealth: number
        weaponShield: number
        health: number
        shieldPieces: number
        shieldPerLevel: number
        shieldLevelCap: number
    }[]
}

export interface CreatureDetail {
    id: string
    species: string
    speciesName: string
    name: string
    customName: boolean
    element: string
    level: number
    experience: string
    abilitySlot: string
    ability: string
    skills: string[]
    bound: boolean
    captorId: string | null
    captorName: string | null
    resources: {
        hp: number
        mp: number
        health: number
        mana: number
        physicalAttack: number
        magicAttack: number
        physicalDefense: number
        magicDefense: number
        speed: number
    }
}

export function useMarketAccess(active: Ref<boolean>) {
    return useQuery({
        queryKey: ['market', 'access'],
        enabled: active,
        queryFn: () => api<MarketAccess>('/api/market/access'),
    })
}
export function useMarketPage(path: Ref<string>, active: Ref<boolean>) {
    return useQuery({
        queryKey: computed(() => ['market', path.value]),
        enabled: active,
        queryFn: () => api<MarketPage>('/api/market/' + path.value),
    })
}

/** 每次明确确认创建请求号；网络失败保留原请求供重试，成功后再刷新列表。 */
export function useMarketAction() {
    const action = useAction(true),
        client = useQueryClient(),
        message = ref('')
    let pending: {key: string; requestId: string} | undefined
    async function submit(endpoint: string, body: Record<string, unknown>) {
        if (action.busy.value) return false
        const key = JSON.stringify([endpoint, body])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        const requestId = pending.requestId
        return action.run(async () => {
            message.value = ''
            try {
                await api('/api/market/' + endpoint, 'POST', {...body, requestId})
                pending = undefined
                message.value = '操作已完成。'
            } finally {
                await client.invalidateQueries()
            }
        })
    }
    return {...action, submit, message}
}

/** 大整数价格使用 BigInt 计算展示，不将金币或雪花编号转为 Number。 */
export function quote(price: string, quantity: number) {
    if (!/^\d+$/.test(price) || !Number.isSafeInteger(quantity) || quantity <= 0) return null
    const unit = BigInt(price),
        gross = unit * BigInt(quantity)
    if (unit < 2n || gross > 9223372036854775807n) return null
    const tax = (gross + 19n) / 20n
    return {gross: gross.toString(), tax: tax.toString(), net: (gross - tax).toString()}
}
