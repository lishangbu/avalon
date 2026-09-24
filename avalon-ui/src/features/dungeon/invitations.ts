/** 邀请只表达入队意愿；占位、确认、准备仍由服务端独立校验。 */
import {queryOptions, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

interface Invitation {
    id: string
    lobby: string
    sender: string
    dungeon: string
    members: number
    capacity: number
    expiresAt: string
}

export const invitationOptions = (after: string) =>
    queryOptions({
        queryKey: ['dungeon', 'invitations', after],
        queryFn: () => api<{invitations: Invitation[]; next: string | null}>(`/api/dungeon/invitations?after=${after}`),
        retry: false,
    })

export function useInvitationActions() {
    const client = useQueryClient()
    const action = useAction(true)
    async function command(path: string, body?: unknown) {
        const success = await action.run(() => api(`/api/dungeon/invitations${path}`, 'POST', body))
        // 网络失败也可能已经入队，重新读取权威状态，不在客户端推测占位结果。
        await client.invalidateQueries({queryKey: ['dungeon']})
        await client.invalidateQueries({queryKey: ['world']})
        return success
    }
    return {...action, command}
}
