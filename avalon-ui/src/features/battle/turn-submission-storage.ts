import type {Order} from './types'

/** 只保存已点击提交但尚未确认的命令，不保存会话、奖励或战局成果。 */
export interface TurnSubmission {
    requestId: string
    revision: number
    round: number
    orders: Order[]
    surrender: boolean
    replacements: Record<string, number>
}

/** 战局编号全局唯一；每个请求独立槽位，避免另一标签页覆盖尚未确认的命令。 */
const prefix = (battleId: string) => `avalon.battle-turn.v1:${battleId}:`
const record = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value)
const integer = (value: unknown): value is number => Number.isSafeInteger(value) && Number(value) >= 0

/** 本地存储是不可信输入。逐字段校验并重建协议对象，额外字段不能进入正式接口。 */
function parse(raw: string): TurnSubmission | undefined {
    let value: unknown
    try {
        value = JSON.parse(raw)
    } catch {
        return undefined
    }
    if (
        !record(value) ||
        typeof value.requestId !== 'string' ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.requestId) ||
        !integer(value.revision) ||
        !integer(value.round) ||
        typeof value.surrender !== 'boolean' ||
        !Array.isArray(value.orders) ||
        !record(value.replacements)
    )
        return undefined
    const orders: Order[] = []
    for (const order of value.orders) {
        if (
            !record(order) ||
            typeof order.actor !== 'string' ||
            typeof order.action !== 'string' ||
            typeof order.target !== 'string' ||
            (order.targets !== undefined &&
                (!Array.isArray(order.targets) || !order.targets.every((target) => typeof target === 'string')))
        )
            return undefined
        orders.push({
            actor: order.actor,
            action: order.action,
            target: order.target,
            ...(order.targets === undefined ? {} : {targets: [...order.targets]}),
        })
    }
    const positions: [string, number][] = []
    for (const [id, position] of Object.entries(value.replacements)) {
        if (!integer(position)) return undefined
        positions.push([id, position])
    }
    return {
        requestId: value.requestId,
        revision: value.revision,
        round: value.round,
        orders,
        surrender: value.surrender,
        replacements: Object.fromEntries(positions),
    }
}

/** 发送前必须成功落盘；配额或权限错误向上传播，不能发送无法恢复的写命令。 */
export function saveTurnSubmission(storage: Storage, battleId: string, body: TurnSubmission) {
    storage.setItem(`${prefix(battleId)}${body.requestId}`, JSON.stringify(body))
}

/** 只确认当前请求，旧响应不能清掉其他标签页的新请求。 */
export function clearTurnSubmission(storage: Storage, battleId: string, requestId: string) {
    storage.removeItem(`${prefix(battleId)}${requestId}`)
}

/** 先恢复最早修订的请求。读取不提交，服务端仍负责归属、版本及幂等检查。 */
export function readTurnSubmission(storage: Storage, battleId: string): TurnSubmission | undefined {
    const scope = prefix(battleId)
    const keys = Array.from({length: storage.length}, (_, index) => storage.key(index))
    const pending: TurnSubmission[] = []
    for (const key of keys) {
        if (!key?.startsWith(scope)) continue
        const body = parse(storage.getItem(key) ?? '')
        if (!body || key !== `${scope}${body.requestId}`) storage.removeItem(key)
        else pending.push(body)
    }
    return pending.sort((a, b) => a.revision - b.revision || a.requestId.localeCompare(b.requestId))[0]
}
