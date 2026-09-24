import {onScopeDispose, ref, watch, type Ref} from 'vue'
import {useQuery, useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {type ProductionView} from './production'

export interface FishingSession {
    id: string
    fish: string
    revision: number
    state: {elapsedMillis: number; caughtMillis: number; position: number; held: boolean; status: string}
    broken: boolean
    fishPosition: number
    tolerance: number
}
interface FishingView {
    session: FishingSession | null
    level: number
    fish: {id: string; name: string; level: number; difficulty: number}[]
    inventory: ProductionView['inventory']
    tools: ProductionView['tools']
    bait: string
}
export function useFishing(active: Ref<boolean>) {
    const query = useQuery({
        queryKey: ['fishing'],
        enabled: active,
        queryFn: () => api<FishingView>('/api/life/fishing'),
    })
    const action = useAction(true),
        client = useQueryClient(),
        session = ref<FishingSession | null>(null),
        connectionError = ref(''),
        online = ref(navigator.onLine)
    let sending = false,
        disposed = false,
        held = false,
        cancelling = false
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
        {immediate: true},
    )
    async function pulse() {
        if (sending || disposed || !online.value || session.value?.state.status !== 'PLAYING') return
        sending = true
        const current = session.value,
            foreground = active.value && !cancelling
        try {
            const next = await api<FishingSession>('/api/life/fishing/input', 'POST', {
                session: current.id,
                revision: current.revision,
                held: foreground && held,
                foreground,
            })
            if (session.value?.id === next.id) session.value = next
            if (online.value) connectionError.value = ''
            if (next.state.status !== 'PLAYING') {
                held = false
                cancelling = false
                await client.invalidateQueries()
            }
        } catch (cause) {
            held = false
            if (online.value)
                connectionError.value = cause instanceof Error ? cause.message : '连接中断，请重新查看本局结果'
            await query.refetch()
        } finally {
            sending = false
            if (foreground && (!active.value || cancelling) && !disposed) void pulse()
        }
    }
    function press(value: boolean) {
        held = online.value && value
        void pulse()
    }
    function cancel() {
        cancelling = true
        held = false
        void pulse()
    }
    const timer = window.setInterval(() => {
        if (active.value && !action.busy.value) void pulse()
    }, 100)
    watch(
        active,
        (value) => {
            if (!value) {
                // 即使输入／抛竿请求尚未返回，快速回到前台也不能撤销本局结束。
                cancelling = true
                held = false
                void pulse()
            } else void query.refetch()
        },
        {flush: 'sync'},
    )
    function offline() {
        online.value = false
        held = false
        cancelling = true
        connectionError.value = '网络已断开，本局停止；联网后会确认结果，鱼饵不返还'
    }
    function reconnect() {
        online.value = true
        if (session.value?.state.status === 'PLAYING') void pulse()
        else void query.refetch()
    }
    window.addEventListener('offline', offline)
    window.addEventListener('online', reconnect)
    onScopeDispose(() => {
        window.removeEventListener('offline', offline)
        window.removeEventListener('online', reconnect)
        disposed = true
        held = false
        window.clearInterval(timer)
    })
    async function start(fish: string, tool: string, bait: string) {
        if (action.busy.value || sending || !active.value || !online.value) return false
        const key = JSON.stringify([fish, tool, bait])
        if (!pending || pending.key !== key) pending = {key, requestId: crypto.randomUUID()}
        held = false
        cancelling = false
        return action.run(async () => {
            await api('/api/life/fishing/start', 'POST', {requestId: pending!.requestId, fish, tool, bait})
            pending = undefined
            await query.refetch()
            if (cancelling || !active.value) void pulse()
        })
    }
    return {...action, query, session, connectionError, online, press, cancel, start}
}
