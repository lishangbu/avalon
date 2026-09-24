/**
 * quest 领域的浏览器数据契约。类型只约束编译期，不代表运行时验证；对象来自服务端响应，不能作为授权凭据。雪花 ID 保持字符串，普通等级/数量为数字，禁止将 ID 转成 Number 后再传回。
 */
/** 任务接口仅返回进度与交互提示，库存和伙伴由各自接口读取。 */
export type QuestAction = {
    id: string
    quest: string
    title: string
    location: string
    npc: string
    npcId?: string | null
    label: string
    interactable: boolean
    /** 接取与交付是不同事务；未接取不能跳过一次性准备阶段直接交付。 */
    acceptRequired?: boolean
    accepted?: boolean
    acceptAvailable?: boolean
    guide?: string
}
export type QuestResult = {step: string; dialogue: string; rewards: string[]}
/** 现场对白由服务端选择阶段；阅读不会触发任务命令。 */
export type QuestConversation = {
    npcId: string | null
    step: string
    title: string
    speaker: string
    stage: 'OFFER' | 'PROGRESS' | 'READY' | 'REVISIT' | 'RESUME'
    text: string
}
export type QuestState = {
    available: QuestAction[]
    completed: string[]
    journal: QuestJournalEntry[]
}

/** 服务端保存的两个指引槽位；取消追踪不会放弃任务。 */
export type QuestTracking = {main: string | null; side: string | null; revision: number}

/** 整项任务记录；分类与标签来自服务端，完成数量不由浏览器自行推算。 */
export type QuestJournalEntry = {
    id: string
    title: string
    type: 'MAIN' | 'SIDE'
    tags: {id: string; name: string}[]
    repeatPolicy: 'ONCE'
    completed: boolean
    completedSteps: number
    totalSteps: number
    status: 'AVAILABLE' | 'ACTIVE' | 'ABANDONED' | 'COMPLETED'
    abandonable: boolean
    revision: number
    steps: {id: string; title: string; location: string; completed: boolean}[]
}
