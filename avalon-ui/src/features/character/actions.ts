/**
 * 创角请求只提交名称和职业意图，不提交等级、位置或ID。成功后失效世界缓存，页面再根据成功标记进入世界；失败保留输入和错误，不能变相修改已有职业。
 */
import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {worldKey} from '@/features/world/queries'

export function useCharacterCreation() {
    const client = useQueryClient()
    const action = useAction(true)

    async function create(name: string, profession: string) {
        return action.run(async () => {
            await api('/api/world/character', 'POST', {name, profession})
            await client.invalidateQueries({queryKey: worldKey})
        })
    }

    return {...action, create}
}
