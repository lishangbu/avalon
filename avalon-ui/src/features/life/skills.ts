import {type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'

export interface LifeSkill {
    code: string
    name: string
    profession: boolean
    learned: boolean
    level: number
    proficiency: number
    nextThreshold: number | null
}

export function useLifeSkills(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['life-skills'],
        enabled: active,
        queryFn: () => api<LifeSkill[]>('/api/life/skills'),
    })
    const client = useQueryClient()
    const action = useAction(true)
    let pending: {key: string; requestId: string} | undefined
    async function change(skill: string, forget: boolean) {
        if (action.busy.value) return false
        const key = `${skill}:${forget}`
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api(`/api/life/skills/${forget ? 'forget' : 'learn'}`, 'POST', {
                requestId: pending!.requestId,
                skill,
                confirmed: forget,
            })
            pending = undefined
            await client.invalidateQueries({queryKey: ['life-skills']})
        })
    }
    return {...action, query, change}
}
