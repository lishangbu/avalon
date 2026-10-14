/** 战局公开协议。雪花编号始终为字符串，敌方隐藏特性和随机种子不在协议中。 */
export interface BattleAction {
    id: string
    name: string
    target: string
    mana: number
    cooldown: number
    available: boolean
}

export interface BattleUnit {
    id: string
    name: string
    element: string
    level: number
    enemy: boolean
    hero: boolean
    position: number
    hp: number
    mp: number
    captured: boolean
    ability: string | null
    shield: number
    slowRounds: number
    effects: string[]
    captures: {code: string; name: string; level: number; chance: number}[]
    stats: {health: number; mana: number}
    captureStats: {
        health: number
        mana: number
        physicalAttack: number
        magicAttack: number
        physicalDefense: number
        magicDefense: number
        speed: number
    } | null
    actions: BattleAction[]
}

export interface Battle {
    loot: {code: string; name: string; quantity: number}[]
    id: string
    round: number
    /** 完整回合状态修订，成员提交期间保持同一窗口。 */
    revision: number
    status: string
    name: string
    units: BattleUnit[]
    log: string[]
    supplies: Record<string, number>
    captureSupplies: {code: string; name: string; level: number; quantity: number}[]
    telegraph: string | null
    training: boolean
    /** 训练目标生成后槽位保密，只有成功捕获才返回槽位与特性。 */
    academyCapture?: {
        viewed: boolean
        selectedBall: string | null
        targetId: string
        instruction: string
        balls: string[]
        capturedAbilitySlot: string | null
        capturedAbility: string | null
    } | null
}

export interface Order {
    actor: string
    action: string
    target: string
    targets?: string[]
}

export interface Lobby {
    encounters: {id: string; name: string; description: string | null}[]
    recovery: boolean
    products: {code: string; name: string; price: number}[]
    vitals: {
        id: string
        name: string
        hp: number
        maxHp: number
        mp: number
        maxMp: number
    }[]
}
