/** 服务端保存账号导航；缓存随会话清除，重新聚焦时同步其他设备的修改。 */
import {useQuery} from '@tanstack/vue-query'
import {authenticated} from '@/features/auth/session'
import {api} from '@/shared/http'
import {bagHandleOutline, pawOutline, personOutline, bookOutline, listOutline} from 'ionicons/icons'

export const navigationKey = ['account-navigation'] as const
export const navigationEntries = [
    {id: 'COMPANIONS', path: '/companions', name: '伙伴', icon: pawOutline},
    {id: 'INVENTORY', path: '/inventory', name: '背包', icon: bagHandleOutline},
    {id: 'CHARACTER', path: '/character', name: '角色', icon: personOutline},
    {id: 'ENCYCLOPEDIA', path: '/encyclopedia', name: '图鉴', icon: bookOutline},
    {id: 'QUESTS', path: '/quests', name: '任务', icon: listOutline},
]
export const defaultNavigation = ['COMPANIONS', 'INVENTORY', 'CHARACTER']
export function useNavigation() {
    return useQuery({
        queryKey: navigationKey,
        queryFn: () => api<string[]>('/api/account/navigation'),
        enabled: authenticated,
    })
}
