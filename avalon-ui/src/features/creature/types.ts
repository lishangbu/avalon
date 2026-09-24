/**
 * creature 领域的浏览器数据契约。类型只约束编译期，不代表运行时验证；对象来自服务端响应，不能作为授权凭据。雪花 ID 保持字符串，普通等级/数量为数字，禁止将 ID 转成 Number 后再传回。
 */
export type OwnedCreature = {
    species: string
    id: string
    name: string
    element: string
    level: number
    experience: number
    abilitySlot: string
    ability: string
    skills: string[]
    partyPosition: number
    locked: boolean
}

export type EncyclopediaEntry = {
    description: string
    /** 物种图片由后端内容目录维护，无图片时为空。 */
    image: string | null
    species: string
    name: string
    element: string
    collected: boolean
    rewarded: boolean
    owned: number
    abilities: {slot: string; name: string}[]
}
