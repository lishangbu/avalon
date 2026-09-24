import {queryOptions, useQuery} from '@tanstack/vue-query'
import type {Ref} from 'vue'
import {api} from '@/shared/http'
import type {Battle, Lobby} from './types'

/** 战斗读取不触发行动；保留断线时最后一个已提交回合，重新获取后才允许继续。 */
export const battleKey = ['battle'] as const
export const lobbyKey = ['battle-lobby'] as const
export const battleOptions = () =>
    queryOptions({
        queryKey: battleKey,
        queryFn: async () => (await api<{battle: Battle | null}>('/api/battle')).battle,
        retry: false,
    })

export function useBattleLobby(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: lobbyKey,
        queryFn: () => api<Lobby>('/api/battle/lobby'),
        enabled,
        retry: false,
    })
}
