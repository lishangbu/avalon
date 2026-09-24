/**
 * 路由与世界页面共享 world 查询键和读取函数。数据包括角色状态与启动内容，服务端是唯一权威；未认证时不启动页面查询，退出时由 QueryClient 清除旧账号缓存。
 */
import {queryOptions, useQuery} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {authenticated} from '@/features/auth/session'
import type {World} from './types'

/** 路由准入与页面复用同一份缓存，角色位置始终取自服务端。 */
export const worldKey = ['world'] as const
export const worldOptions = () =>
    queryOptions({
        queryKey: worldKey,
        queryFn: () => api<World>('/api/world'),
        retry: false,
    })

export function useWorld() {
    return useQuery({...worldOptions(), enabled: authenticated})
}
