/**
 * 背包来自独立库存查询，不复用任务聚合响应。查询以认证账号隐式限定归属；任务发奖和穿戴成功都会使相同键失效，避免维护两份库存副本。
 */
import {useQuery} from '@tanstack/vue-query'
import type {Ref} from 'vue'
import {api} from '@/shared/http'
import type {BagView, OwnedItem} from './types'

/** 领域接口拥有独立缓存；奖励事务结束后由任务操作统一触发刷新。 */
export const inventoryKey = ['inventory'] as const

/** 套装计件与效果由服务端统一计算，页面不自行重复装备规则。 */
export function useLoadout(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...inventoryKey, 'loadout'],
        queryFn: () =>
            api<{
                sets: Array<{
                    id: string
                    name: string
                    count: number
                    total: number
                    healthPieces: number
                    health: number
                    shieldPieces: number
                    shield: number
                    hasWeaponBonus: boolean
                    weaponActive: boolean
                    weaponHealth: number
                    weaponShield: number
                }>
            }>('/api/inventory/equipment/loadout'),
        enabled,
        retry: false,
    })
}

export function useInventory(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: inventoryKey,
        queryFn: () => api<OwnedItem[]>('/api/inventory'),
        enabled,
        retry: false,
    })
}

export function useBag(enabled: Ref<boolean>) {
    return useQuery({
        // 背包投影属于库存查询；穿戴及任务奖励按库存前缀统一失效。
        queryKey: [...inventoryKey, 'bag'],
        queryFn: () => api<BagView>('/api/inventory/bag'),
        enabled,
        retry: false,
    })
}
