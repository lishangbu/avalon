import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {DungeonSubmission} from './types'

/** 命令失败也刷新权威状态，处理其他成员结算或截止时间刚过的情况。 */
export function useDungeonActions() {
    const client = useQueryClient()
    const action = useAction(true)
    let pendingSubmit: {key: string; requestId: string} | undefined

    async function command(run: string, name: string, body?: unknown) {
        const success = await action.run(() => api(`/api/dungeon/runs/${run}/${name}`, 'POST', body))
        await client.invalidateQueries({queryKey: ['dungeon']})
        await client.invalidateQueries({queryKey: ['world']})
        return success
    }

    async function submit(run: string, battle: string, body: DungeonSubmission) {
        // 已锁定提交的重试复用同一UUID，不能在响应丢失后创建不同请求覆盖本轮行动。
        const key = JSON.stringify([run, battle, body])
        if (!pendingSubmit || pendingSubmit.key !== key) pendingSubmit = {key, requestId: crypto.randomUUID()}
        const success = await command(run, `battles/${battle}/submit`, {...body, requestId: pendingSubmit.requestId})
        if (success) pendingSubmit = undefined
        return success
    }

    return {...action, command, submit}
}
