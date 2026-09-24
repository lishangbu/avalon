/**
 * world 领域的浏览器数据契约。类型只约束编译期，不代表运行时验证；对象来自服务端响应，不能作为授权凭据。雪花 ID 保持字符串，普通等级/数量为数字，禁止将 ID 转成 Number 后再传回。
 */
import type {Character} from '@/features/character/types'

export type Npc = {id: string; name: string; role: string; description: string; greeting: string; services: string[]}

export type Place = {
    id: string
    name: string
    description: string
    /** 服务端已按人物进度筛选的接待绑定，页面不得自行恢复被隐藏的人物。 */
    npcs?: {npcId: string}[]
    x: number
    y: number
    exits: string[]
    services: string[]
}
export type World = {
    character: Character | null
    content: {
        start: string
        professions: {id: string; name: string; description: string}[]
        npcs?: Npc[]
        places: Place[]
    }
}
