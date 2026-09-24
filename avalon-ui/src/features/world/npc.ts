import type {QuestAction} from '@/features/quest/types'

/** 只聚合当前接待人的状态，不能把另一地点的可交付任务映射到同名人物。 */
export function npcQuestMarker(actions: QuestAction[], npcId: string, location: string) {
    const local = actions.filter((action) => action.npcId === npcId && action.location === location)
    if (local.some((action) => action.accepted && action.interactable))
        return {symbol: '?', label: '可交付任务', ready: true}
    if (local.some((action) => !action.accepted && action.acceptAvailable))
        return {symbol: '!', label: '可接取任务', ready: true}
    if (local.some((action) => action.accepted)) return {symbol: '?', label: '任务进行中', ready: false}
    return undefined
}
