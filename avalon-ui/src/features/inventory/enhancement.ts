import {ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {OwnedItem} from './types'

type Mode = 'UNBOUND_ONLY' | 'BOUND_FIRST'
export interface EnhancementPreview {
    before: OwnedItem
    cost: {
        current: number
        target: number
        failureLevel: number
        successPercent: number
        stone: string
        quantity: number
        gold: string
    }
    mode: Mode
    materials: {quantities: Record<string, number>; bindsTarget: boolean}
    resultBound: boolean
    successStats: Record<string, number>
    failureStats: Record<string, number>
}
export function useEnhancementAccess(active: Ref<boolean>) {
    return useQuery({
        queryKey: ['enhancement', 'access'],
        enabled: active,
        queryFn: () => api<{available: boolean; requiredLevel: number}>('/api/enhancement/access'),
    })
}

/** 确认资料及请求号固定到本次尝试；失败保留原请求，已提交结果重试由服务端凭证返回。 */
export function useEnhancementActions() {
    const action = useAction(true),
        client = useQueryClient()
    const preview = ref<EnhancementPreview>(),
        result = ref<{success: boolean; item: OwnedItem}>()
    let pending: {requestId: string; expected: EnhancementPreview} | undefined
    async function inspect(id: string, mode: Mode | '') {
        preview.value = undefined
        result.value = undefined
        pending = undefined
        return action.run(async () => {
            const value = await api<EnhancementPreview>(`/api/enhancement/${id}${mode ? '?mode=' + mode : ''}`)
            preview.value = value
            pending = {requestId: crypto.randomUUID(), expected: value}
        })
    }
    async function submit() {
        if (!pending) return false
        const request = pending
        return action.run(async () => {
            result.value = await api(`/api/enhancement/${request.expected.before.id}`, 'POST', request)
            pending = undefined
            preview.value = undefined
            await client.invalidateQueries()
        })
    }
    return {...action, preview, result, inspect, submit}
}
