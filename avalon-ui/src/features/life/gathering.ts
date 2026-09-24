import {onScopeDispose, ref, watch, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
export interface GatheringSession {
    id: string
    node: string
    creature: string
    tool: string
    toolsRemaining: number
    roundMillis: number
    progressMillis: number
    revision: number
    completed: number
    status: string
    supplies: Record<string, number>
}
interface GatheringView {
    supplies: {id: string; code: string; name: string; quantity: number; bound: boolean}[]
    session: GatheringSession | null
    nodes: {
        id: string
        name: string
        skillName: string
        toolKind: string
        level: number
        product: string
        quantity: number
        health: number
        mana: number
        available: boolean
    }[]
    creatures: {id: string; name: string; level: number}[]
    inventory: {id: string; code: string; name: string; quantity: number}[]
    tools: {item: string; kind: string; tier: number}[]
}
/** 心跳串行发出，不传客户端时长；后台或离页发送暂停，网络失败后先重新读取版本。 */
export function useGathering(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['gathering'],
        enabled: active,
        queryFn: () => api<GatheringView>('/api/life/gathering'),
    })
    const action = useAction(true),
        client = useQueryClient(),
        heartbeatError = ref('')
    const session = ref<GatheringSession | null>(null)
    let sending = false,
        disposed = false,
        pauseRevision = 0,
        confirmedPauseRevision = 0
    let pending: {key: string; requestId: string} | undefined
    watch(
        () => query.data.value,
        (value) => {
            if (
                value &&
                (!session.value ||
                    value.session?.id !== session.value.id ||
                    value.session.revision >= session.value.revision)
            )
                session.value = value.session
        },
    )
    async function pulse(foreground: boolean) {
        if (sending || !session.value || disposed) return
        // 返回前台不能跳过尚未确认的暂停，否则后台间隔可能获得一次心跳额度。
        foreground = foreground && pauseRevision === confirmedPauseRevision
        const pendingPause = pauseRevision
        sending = true
        try {
            const state = session.value
            const next = await api<GatheringSession>('/api/life/gathering/heartbeat', 'POST', {
                session: state.id,
                revision: state.revision,
                foreground,
            })
            if (session.value?.id === next.id) session.value = next
            if (!foreground) confirmedPauseRevision = pendingPause
            heartbeatError.value = ''
            if (next.completed !== state.completed) await client.invalidateQueries({queryKey: ['life-skills']})
        } catch (cause) {
            heartbeatError.value = cause instanceof Error ? cause.message : '连接中断，采集暂停'
            await query.refetch()
        } finally {
            sending = false
            // 离页可能发生在上一心跳进行中，补发暂停以清除服务端在线基点。
            if (foreground && pauseRevision !== confirmedPauseRevision && !disposed) void pulse(false)
        }
    }
    const timer = window.setInterval(() => {
        if (active.value && !action.busy.value) void pulse(true)
    }, 1000)
    watch(
        active,
        (value) => {
            if (!value) {
                pauseRevision++
                void pulse(false)
            } else void query.refetch()
        },
        {flush: 'sync'},
    )
    onScopeDispose(() => {
        window.clearInterval(timer)
        disposed = true
    })
    async function start(
        node: string,
        creature: string,
        tool: string,
        tools: number,
        supplies: Record<string, number>,
    ) {
        if (action.busy.value || !active.value) return false
        const key = JSON.stringify(['START', node, creature, tool, tools, Object.entries(supplies).sort()])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api('/api/life/gathering/start', 'POST', {
                requestId: pending!.requestId,
                node,
                creature,
                tool,
                tools,
                supplies,
            })
            pending = undefined
            await query.refetch()
            if (!active.value || pauseRevision !== confirmedPauseRevision) void pulse(false)
        })
    }
    async function stop() {
        if (action.busy.value || !session.value || sending) return false
        const id = session.value.id,
            key = `STOP:${id}`
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        return action.run(async () => {
            await api('/api/life/gathering/stop', 'POST', {requestId: pending!.requestId, session: id})
            pending = undefined
            session.value = null
            await client.invalidateQueries()
        })
    }
    return {...action, query, session, heartbeatError, start, stop}
}
