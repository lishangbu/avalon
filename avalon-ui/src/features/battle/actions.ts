import {useQueryClient} from '@tanstack/vue-query'
import {useRouter} from 'vue-router'
import {shallowRef} from 'vue'
import {useAction} from '@/shared/useAction'
import {api, HttpError} from '@/shared/http'
import {
    readTurnSubmission,
    saveTurnSubmission,
    clearTurnSubmission,
    type TurnSubmission,
} from './turn-submission-storage'
import {battleKey} from './queries'
import type {Battle, Order, PvpView} from './types'

/** 写命令保留请求号直到成功，网络响应丢失后重试同一请求，不另开遭遇或重复购买。 */
export function useBattleActions() {
    const client = useQueryClient(),
        router = useRouter(),
        action = useAction(true)
    let pendingStart: {encounter: string; requestId: string} | undefined
    const pendingTurn = shallowRef<TurnSubmission>()
    /** 页面切换战局时重新读取该战局的请求；读取失败显示错误并由提交路径再次阻止写入。 */
    function restoreTurn(battleId: string) {
        try {
            pendingTurn.value = readTurnSubmission(localStorage, battleId)
        } catch {
            action.error.value = '无法读取待确认回合，请恢复浏览器存储后重试'
        }
    }
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
            // 未确认命令优先于当前面板草稿，不能因重启后的轮次变化或重新选招而另建请求。
            const body = readTurnSubmission(localStorage, battle.id) ?? {
                requestId: crypto.randomUUID(),
                revision: battle.revision,
                round: battle.round,
                // Vue 的响应式数组不能直接 structuredClone，按协议字段复制以冻结发送时的行动。
                orders: orders.map((order) => ({
                    actor: order.actor,
                    action: order.action,
                    target: order.target,
                    ...(order.targets === undefined ? {} : {targets: [...order.targets]}),
                })),
                surrender,
                replacements: {...replacements},
            }
            saveTurnSubmission(localStorage, battle.id, body)
            pendingTurn.value = body
            let result: Battle
            try {
                result = battle.pvp
                    ? (await api<PvpView>(`/api/battle/pvp/${battle.id}/turn`, 'POST', body)).battle
                    : await api<Battle>(`/api/battle/${battle.id}/turn`, 'POST', body)
            } catch (cause) {
                // 明确的业务拒绝可以结束该命令；网络、服务异常及认证失败均保留原负载。
                if (cause instanceof HttpError && [400, 404, 409, 422].includes(cause.status)) {
                    clearTurnSubmission(localStorage, battle.id, body.requestId)
                    restoreTurn(battle.id)
                }
                throw cause
            }
            clearTurnSubmission(localStorage, battle.id, body.requestId)
            restoreTurn(battle.id)
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

    return {...action, start, turn, recover, purchase, captureInfo, pendingTurn, restoreTurn}
}
