/** 好友编号保持字符串；读写复用认证客户端，页面缓存不承担关系或权限判断。 */
import {queryOptions, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface FriendEntry {
    id: string
    character: string
    name: string
    profession: string
    status: 'ACCEPTED' | 'INCOMING' | 'OUTGOING'
}

export const friendsOptions = (after: string) =>
    queryOptions({
        queryKey: ['friends', after],
        queryFn: () => api<{entries: FriendEntry[]; next: string | null}>(`/api/friends?after=${after}`),
        retry: false,
    })

export function useFriendActions() {
    const client = useQueryClient()
    const action = useAction(true)

    async function command(path: string, method = 'POST', body?: unknown) {
        const success = await action.run(() => api(`/api/friends${path}`, method, body))
        // 超时不能证明写入失败；重新读取服务端，避免继续展示已删除或已接受的旧关系。
        await client.invalidateQueries({queryKey: ['friends']})
        return success
    }

    return {...action, command}
}
