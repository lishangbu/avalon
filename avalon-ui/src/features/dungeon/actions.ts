import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import type {DungeonSubmission} from './types'

/** 命令失败也刷新权威状态，处理其他成员结算或截止时间刚过的情况。 */
export function useDungeonActions() {
    const client = useQueryClient()
    const action = useAction(true)

    async function command(run: string, name: string, body?: unknown) {
        const success = await action.run(() => api(`/api/dungeon/runs/${run}/${name}`, 'POST', body))
        await client.invalidateQueries({queryKey: ['dungeon']})
        await client.invalidateQueries({queryKey: ['world']})
        return success
    }

    async function submit(run: string, battle: string, body: DungeonSubmission) {
        return command(run, `battles/${battle}/submit`, body)
    }

    return {...action, command, submit}
}
