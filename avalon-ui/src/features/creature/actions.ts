import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {OwnedCreature} from './types'

/** 编队只提交个体编号和目标位置；成功后刷新跨领域缓存，任务条件与生命面板随之更新。 */
export function usePlacement() {
    const action = useAction(true)
    const client = useQueryClient()

    async function place(id: string, position: number) {
        return action.run(async () => {
            await api(`/api/creatures/${id}/position`, 'POST', {position})
            await client.invalidateQueries()
        })
    }

    async function manage(id: string, operation: 'deposit' | 'lock', locked?: boolean) {
        return action.run(async () => {
            await api(`/api/creatures/${id}/${operation}`, 'POST', operation === 'lock' ? {locked} : undefined)
            await client.invalidateQueries()
        })
    }

    /** 请求号由确认弹窗保留，网络失败后再次点击仍提交同一份确认。 */
    async function release(requestId: string, expected: OwnedCreature[]) {
        return action.run(async () => {
            await api('/api/creatures/release', 'POST', {requestId, expected})
            await client.invalidateQueries()
        })
    }

    async function claimCollection() {
        return action.run(async () => {
            await api('/api/creatures/encyclopedia/rewards', 'POST')
            await client.invalidateQueries()
        })
    }

    /** 在幻兽中心登记野外捕获结果；服务端核验捕获来源，任务奖励仍由后续交付结算。 */
    async function registerCapture() {
        return action.run(async () => {
            await api('/api/creatures/capture-registration', 'POST')
            await client.invalidateQueries()
        })
    }

    return {...action, place, manage, release, claimCollection, registerCapture}
}
