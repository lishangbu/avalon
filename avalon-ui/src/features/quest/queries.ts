/**
 * 任务读缓存只包含进度和可用交互。任务命令成功意味着后端奖励事务已提交，随后同时刷新世界、任务、库存和伙伴；客户端不推算经验或凭空追加奖励实例。
 */
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {ref, type Ref} from 'vue'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {QuestConversation, QuestResult, QuestState, QuestTracking} from './types'

// 任务仅保存进度；跨领域奖励提交后再刷新各领域状态。
export const questKey = ['quests'] as const

export function useQuestConversations(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...questKey, 'dialogues'],
        queryFn: () => api<QuestConversation[]>('/api/quests/dialogues'),
        enabled,
        retry: false,
    })
}

export function useQuestTracking(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...questKey, 'tracking'],
        queryFn: () => api<QuestTracking>('/api/quests/tracking'),
        enabled,
        retry: false,
    })
}

export function useQuests(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: questKey,
        queryFn: () => api<QuestState>('/api/quests'),
        enabled,
        retry: false,
    })
}

export function useQuestInteraction() {
    const client = useQueryClient()
    const action = useAction(true)
    const dialogue = ref<QuestResult | null>(null)
    const pendingAcceptance = ref<{step: string; requestId: string; expectedRevision: number}>()

    /** 接取失败结果未知时保留整份请求，不随移动或重新点击生成第二份补给。 */
    async function accept(step: string, expectedRevision: number) {
        if (action.busy.value || pendingAcceptance.value) return false
        pendingAcceptance.value = {step, requestId: crypto.randomUUID(), expectedRevision}
        return retryAcceptance()
    }

    async function retryAcceptance() {
        const pending = pendingAcceptance.value
        if (!pending) return false
        return action.run(async () => {
            try {
                const {step, ...body} = pending
                dialogue.value = await api<QuestResult>(`/api/quests/${encodeURIComponent(step)}/accept`, 'POST', body)
                pendingAcceptance.value = undefined
            } catch (cause) {
                if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500)
                    pendingAcceptance.value = undefined
                throw cause
            } finally {
                await client.invalidateQueries()
            }
        })
    }

    async function interact(step: string) {
        return action.run(async () => {
            dialogue.value = await api<QuestResult>(`/api/quests/${step}`, 'POST')
            // 任务事实还会开放课程、商店和其他设施，不能只刷新奖励库存或依赖角色版本变化。
            // 刷新全部已挂载查询；未挂载查询标记为过期，进入对应页面时再读取。
            await client.invalidateQueries()
        })
    }

    async function abandon(quest: string, expectedRevision: number) {
        return action.run(async () => {
            try {
                await api<void>(`/api/quests/${encodeURIComponent(quest)}/abandon`, 'POST', {expectedRevision})
            } finally {
                await client.invalidateQueries()
            }
        })
    }

    async function track(type: 'MAIN' | 'SIDE', quest: string | null, expectedRevision: number) {
        return action.run(async () => {
            try {
                await api<void>('/api/quests/tracking', 'PUT', {type, quest, expectedRevision})
            } finally {
                await client.invalidateQueries({queryKey: questKey})
            }
        })
    }

    return {...action, dialogue, interact, accept, abandon, track, retryAcceptance, pendingAcceptance}
}
