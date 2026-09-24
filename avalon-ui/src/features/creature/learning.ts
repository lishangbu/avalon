import {computed, ref, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface LearningView {
    creatureId: string
    current: string[]
    probabilities: number[]
    options: {
        skill: string
        minimumLevel: number
        unavailable: string | null
        books: {id: string; name: string; quantity: number; bound: boolean}[]
    }[]
}

export interface LearningResult {
    learned: string
    forgotten: string[]
    remaining: string[]
}

/** 预览缓存按个体编号隔离；关闭弹窗不再读取，查询不改变技能或随机结果。 */
export function useLearning(id: Ref<string | null>) {
    return useQuery({
        queryKey: computed(() => ['creature-learning', id.value]),
        enabled: computed(() => !!id.value),
        queryFn: () => api<LearningView>(`/api/creatures/${id.value}/learning`),
    })
}

/** 同一确认在失败重试时保留请求号；只有收到结果后才清除，避免响应丢失造成重复扣书。 */
export function useLearningAction() {
    const action = useAction(true)
    const client = useQueryClient()
    const result = ref<LearningResult | null>(null)
    let pending: {key: string; requestId: string} | undefined

    async function learn(view: LearningView, bookId: string) {
        const key = JSON.stringify([view.creatureId, bookId, view.current])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            result.value = await api<LearningResult>(`/api/creatures/${view.creatureId}/learning`, 'POST', {
                requestId: pending!.requestId,
                bookId,
                expectedSkills: [...view.current],
            })
            pending = undefined
            await client.invalidateQueries()
        })
    }

    return {...action, result, learn}
}
