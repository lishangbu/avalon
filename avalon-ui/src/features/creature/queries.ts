/**
 * 伙伴列表独立查询，读取等级、特性、技能与位置等持久化结果。页面不根据人物经验追赶伙伴，也不根据隐藏槽位重算特性；奖励命令完成后统一使缓存失效。
 */
import {useQuery} from '@tanstack/vue-query'
import type {Ref} from 'vue'
import {api} from '@/shared/http'
import type {EncyclopediaEntry, OwnedCreature} from './types'

/** 领域接口拥有独立缓存；奖励事务结束后由任务操作统一触发刷新。 */
export const creatureKey = ['creatures'] as const

/** 来源不参与可交易资格的客户端推断；只有服务端允许读取的当前个体才请求该详情。 */
export function useCreatureProvenance(id: string, enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...creatureKey, id, 'provenance'],
        queryFn: () =>
            api<{
                bound: boolean
                originKind: string
                originId: string | null
                originName: string | null
                captorId: string | null
                captorName: string | null
            }>(`/api/creatures/${id}/provenance`),
        enabled,
        retry: false,
    })
}

export function useEncyclopedia(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...creatureKey, 'encyclopedia'],
        queryFn: () => api<EncyclopediaEntry[]>('/api/creatures/encyclopedia'),
        enabled,
        retry: false,
    })
}

export function useCreature(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: creatureKey,
        queryFn: () => api<OwnedCreature[]>('/api/creatures'),
        enabled,
        retry: false,
    })
}
