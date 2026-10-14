import {useQueryClient} from '@tanstack/vue-query'
import {useRouter} from 'vue-router'
import {useAction} from '@/shared/useAction'
import {api} from '@/shared/http'
import {battleKey} from './queries'
import type {Battle, Order} from './types'

/** 写命令保留请求号直到成功，网络响应丢失后重试同一请求，不另开遭遇或重复购买。 */
export function useBattleActions() {
    const client = useQueryClient(),
        router = useRouter(),
        action = useAction(true)
    let pendingStart: {encounter: string; requestId: string} | undefined
    let pendingTurn:
        | {
              key: string
              body: {
                  requestId: string
                  revision: number
                  round: number
                  orders: Order[]
                  surrender: boolean
                  replacements: Record<string, number>
              }
          }
        | undefined
    let pendingPurchase: {code: string; requestId: string} | undefined

    async function refresh() {
        await client.invalidateQueries()
    }

    async function start(encounter: string) {
        return action.run(async () => {
            if (!pendingStart || pendingStart.encounter !== encounter)
                pendingStart = {encounter, requestId: crypto.randomUUID()}
            const battle = await api<Battle>('/api/battle', 'POST', pendingStart)
            pendingStart = undefined
            client.setQueryData(battleKey, battle)
            await refresh()
            await router.push('/battle')
        })
    }

    async function turn(battle: Battle, orders: Order[], surrender = false, replacements: Record<string, number> = {}) {
        return action.run(async () => {
            // 网络失败后完全相同的指令复用UUID，改变选招则创建新请求；服务器仍拒绝已完成轮的异参覆盖。
            const key = JSON.stringify([battle.id, battle.revision, battle.round, orders, surrender, replacements])
            if (!pendingTurn || pendingTurn.key !== key)
                pendingTurn = {
                    key,
                    body: {
                        requestId: crypto.randomUUID(),
                        revision: battle.revision,
                        round: battle.round,
                        orders,
                        surrender,
                        replacements,
                    },
                }
            const result = await api<Battle>(`/api/battle/${battle.id}/turn`, 'POST', pendingTurn.body)
            pendingTurn = undefined
            client.setQueryData(battleKey, result)
            await refresh()
        })
    }

    async function recover() {
        return action.run(async () => {
            await api('/api/battle/recover', 'POST')
            await refresh()
        })
    }

    /** 查看与确认训练用球不等于投球；实际捕捉仍通过正式整轮指令提交。 */
    async function captureInfo(id: string, ball: string) {
        return action.run(async () => {
            const result = await api<Battle>(`/api/battle/academy/${encodeURIComponent(id)}/capture-info`, 'POST', {
                ball,
            })
            client.setQueryData(battleKey, result)
        })
    }

    async function purchase(code: string) {
        return action.run(async () => {
            if (!pendingPurchase || pendingPurchase.code !== code)
                pendingPurchase = {code, requestId: crypto.randomUUID()}
            await api('/api/battle/purchase', 'POST', pendingPurchase)
            pendingPurchase = undefined
            await refresh()
        })
    }

    return {...action, start, turn, recover, purchase, captureInfo}
}
