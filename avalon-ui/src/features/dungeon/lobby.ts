import {queryOptions, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface LobbyMember {
    id: string
    name: string
    profession: string
    level: number
    self: boolean
    confirmed: boolean
    ready: boolean
    inBattle: boolean
    deadline: string | null
    party: {
        id: string
        name: string
        level: number
        hero: boolean
        position: number
        health: number
        maxHealth: number
        mana: number
        maxMana: number
    }[]
    supplies: Record<string, number>
}

export interface DungeonLobby {
    id: string
    name: string
    leader: string | null
    run: string | null
    targetRun: string | null
    closed: boolean
    canLaunch: boolean
    members: LobbyMember[]
}

interface Recruitment {
    id: string
    name: string
    leader: string | null
    members: number
    capacity: number
    professions: string[]
}

export const lobbyCurrentOptions = () =>
    queryOptions({
        queryKey: ['dungeon', 'lobby', 'current'],
        queryFn: () => api<{lobby: DungeonLobby | null}>('/api/dungeon/lobbies/current'),
        retry: false,
    })
export const recruitmentOptions = (after: string) =>
    queryOptions({
        queryKey: ['dungeon', 'recruitment', after],
        queryFn: () => api<{offers: Recruitment[]; next: string | null}>(`/api/dungeon/lobbies?after=${after}`),
        retry: false,
    })

interface Replacement extends Omit<Recruitment, 'leader'> {
    cleared: number
    stages: number
}

export const replacementOptions = (after: string) =>
    queryOptions({
        queryKey: ['dungeon', 'replacements', after],
        queryFn: () =>
            api<{offers: Replacement[]; next: string | null}>(`/api/dungeon/lobbies/replacements?after=${after}`),
        retry: false,
    })

/** 创建请求的编号由组件保留到成功；网络重试不会重复创建队伍。 */
export function useLobbyActions() {
    const client = useQueryClient()
    const action = useAction(true)

    async function command(path: string, body?: unknown) {
        const success = await action.run(() => api(`/api/dungeon/lobbies${path}`, 'POST', body))
        await client.invalidateQueries({queryKey: ['dungeon']})
        await client.invalidateQueries({queryKey: ['world']})
        return success
    }

    return {...action, command}
}
