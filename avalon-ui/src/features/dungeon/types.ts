import type {BattleUnit, Order} from '@/features/battle/types'

/** 多人协议编号保持字符串，角色编号与账号编号不能混用。 */
export interface DungeonRun {
    id: string
    name: string
    leader: string
    self: string
    cleared: number
    completed: boolean
    battle: string | null
    previousBattle: string | null
    canBegin: boolean
    canReady: boolean
    canRest: boolean
    recruiting: boolean
    canRecruit: boolean
    destinations: number[]
    route: {position: number; name: string; cleared: boolean; rest: boolean}[]
    members: {id: string; name: string; profession: string; level: number; location: number; ready: boolean}[]
    supplies: Record<string, number>
}

export interface DungeonCurrent {
    run: DungeonRun | null
    remainingStarts: number
}

export interface DungeonCombat {
    loot: {code: string; name: string; quantity: number}[]
    id: string
    run: string
    round: number
    status: string
    name: string
    deadline: string
    self: string
    canSubmit: boolean
    members: {id: string; submitted: boolean; removed: boolean; leaving: boolean; missedRounds: number}[]
    units: {owner: string | null; controllable: boolean; detail: BattleUnit}[]
    log: string[]
    supplies: Record<string, number>
    telegraph: string | null
}

export interface DungeonSubmission {
    round: number
    orders: Order[]
    replacements: Record<string, number>
}
