/**
 * inventory 领域的浏览器数据契约。类型只约束编译期，不代表运行时验证；对象来自服务端响应，不能作为授权凭据。雪花 ID 保持字符串，普通等级/数量为数字，禁止将 ID 转成 Number 后再传回。
 */
export type OwnedItem = {
    id: string
    name: string
    code: string
    quantity: number
    slot: string | null
    requiredLevel: number
    stats: Record<string, number>
    equipped: boolean
    bound: boolean
    quality: string | null
    sellPrice: number
    replacementPrice: number
    requiredProfession: string | null
    source: string
    enhancementLevel: number
}

/** 服务端统一投影的背包格，stack.key 仅用于渲染，不能作为物品编号提交命令。 */
export interface BagView {
    usedSlots: number
    capacity: number
    equipment: OwnedItem[]
    tasks: OwnedItem[]
    stacks: {key: string; code: string; name: string; category: string; bound: boolean; quantity: number}[]
}
