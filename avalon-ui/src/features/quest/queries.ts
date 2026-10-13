/**
 * 任务读缓存只包含进度和可用交互。任务命令成功意味着后端奖励事务已提交，随后同时刷新世界、任务、库存和伙伴；客户端不推算经验或凭空追加奖励实例。
 */
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {ref, type Ref} from 'vue'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {QuestConversation, QuestResult, QuestState, QuestTracking, QuestInstance, QuestInstanceView} from './types'

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

/** 只读实例查询用于目标与分支展示，截止资格由服务器判定，不由浏览器时钟决定。 */
export function useQuestInstances(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: [...questKey, 'instances'],
        queryFn: () => api<QuestInstanceView[]>('/api/quests/instances'),
        enabled,
        retry: false,
        refetchInterval: 15000,
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

    /**
     * 实例命令失败且结果未知时保留原请求，下一次点击仍重放它，不能生成新请求号误交付下一轮。
     * 明确的4xx表示服务器拒绝了操作，可以清除请求并刷新页面。
     */
    const pendingInstance = ref<{
        requestId: string
        instance: string
        round: number
        operation: 'DELIVER' | 'CHOOSE' | 'REFRESH'
        step?: string
        group?: string
        option?: string
    }>()
    async function instanceCommand(body: Omit<NonNullable<typeof pendingInstance.value>, 'requestId'>) {
        if (action.busy.value) return false
        if (!pendingInstance.value) pendingInstance.value = {...body, requestId: crypto.randomUUID()}
        return retryInstance()
    }

    /** 即使响应丢失后步骤已经消失，也提供原请求回放入口以取回首次结果。 */
    async function retryInstance() {
        if (!pendingInstance.value || action.busy.value) return false
        return action.run(async () => {
            try {
                const result = await api<{instance: QuestInstance; rewards: string[]; dialogue: string}>(
                    '/api/quests/instances/commands',
                    'POST',
                    pendingInstance.value,
                )
                dialogue.value = {
                    step: pendingInstance.value?.step ?? '',
                    dialogue: result.dialogue,
                    rewards: result.rewards,
                }
                pendingInstance.value = undefined
            } catch (cause) {
                if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500)
                    pendingInstance.value = undefined
                throw cause
            } finally {
                await client.invalidateQueries()
            }
        })
    }

    async function interact(step: string) {
        const current = client.getQueryData<QuestState>(questKey)?.available.find((value) => value.id === step)
        if (!current?.instance || current.round == null) {
            await client.invalidateQueries({queryKey: questKey})
            return false
        }
        return instanceCommand({instance: current.instance, round: current.round, operation: 'DELIVER', step})
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

    return {
        ...action,
        dialogue,
        interact,
        accept,
        abandon,
        track,
        retryAcceptance,
        pendingAcceptance,
        instanceCommand,
        pendingInstance,
        retryInstance,
    }
}
