import {queryOptions} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import type {DungeonCombat, DungeonCurrent, DungeonRun} from './types'

/** 路由与页面共用查询；轮询只读取，不触发自动行动或默认准备。 */
export const dungeonCurrentOptions = () =>
    queryOptions({
        queryKey: ['dungeon', 'current'],
        queryFn: () => api<DungeonCurrent>('/api/dungeon/runs/current'),
        retry: false,
    })
export const dungeonRunOptions = (id: string) =>
    queryOptions({
        queryKey: ['dungeon', 'run', id],
        queryFn: () => api<DungeonRun>(`/api/dungeon/runs/${id}`),
        retry: false,
    })
export const dungeonCombatOptions = (run: string, battle: string) =>
    queryOptions({
        queryKey: ['dungeon', 'combat', run, battle],
        queryFn: () => api<DungeonCombat>(`/api/dungeon/runs/${run}/battles/${battle}`),
        retry: false,
    })
