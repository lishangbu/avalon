/** 学院非战斗课程的数据契约；所有物品和个体都属于服务器隔离会话，不复用真实背包缓存。 */
import {ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api, HttpError} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export type WorkshopView = {
    id: string
    course: string
    status: 'ACTIVE' | 'PASSED' | 'EXITED'
    revision: number
    phase: string
    message: string
    level: number
    hp: number
    maxHp: number
    mp: number
    maxMp: number
    skills: string[]
    skillPool: string[]
    books: {code: string; skill: string; quantity: number; available: boolean; reason: string}[]
    items: {
        id: string
        name: string
        code: string
        quantity: number
        pending: number
        equipment: boolean
        equipped: boolean
        requiredLevel: number
        health: number
        mana: number
    }[]
    pets: {id: string; name: string; position: number; locked: boolean; hp: number; mp: number}[]
    actions: string[]
    progress: string[]
    learningResult: {learned: string; forgotten: string[]; remaining: string[]} | null
    bagSlots: number
    bagCapacity: number
    storageCapacity: number
    forgetProbabilities: number[][]
}
export type Workshop = {
    active: WorkshopView | null
    courses: {id: string; title: string; available: boolean; completed: boolean; reason: string}[]
}
export const workshopKey = ['academy-workshop'] as const
export function useWorkshop(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: workshopKey,
        queryFn: () => api<Workshop>('/api/academy/workshop'),
        enabled,
        retry: false,
    })
}

export function useWorkshopActions() {
    const client = useQueryClient()
    const action = useAction(true)
    const result = ref<WorkshopView>()
    const pending = ref<{path: string; body: object}>()

    async function send() {
        const request = pending.value
        if (!request) return false
        return action.run(async () => {
            try {
                result.value = await api<WorkshopView>(request.path, 'POST', request.body)
                pending.value = undefined
            } catch (cause) {
                if (cause instanceof HttpError && cause.status >= 400 && cause.status < 500) pending.value = undefined
                throw cause
            } finally {
                await client.invalidateQueries()
            }
        })
    }

    async function start(course: string) {
        if (pending.value || action.busy.value) return false
        pending.value = {path: '/api/academy/workshop', body: {course, requestId: crypto.randomUUID()}}
        return send()
    }

    async function operate(view: WorkshopView, type: string, target?: string, position?: number) {
        if (pending.value || action.busy.value) return false
        pending.value = {
            path: `/api/academy/workshop/${encodeURIComponent(view.id)}/actions`,
            body: {requestId: crypto.randomUUID(), revision: view.revision, type, target, position},
        }
        return send()
    }
    return {...action, result, pending, send, start, operate}
}
