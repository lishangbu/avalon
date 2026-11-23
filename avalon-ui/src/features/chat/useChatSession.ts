/**
 * 全局角色级聊天面板：登录换号时由应用壳重建，流连接与消息状态不会跨账号复用。
 * 面板展开、频道和对端只保存到角色专属localStorage键；正文、草稿、黑名单不写浏览器持久化。
 * 战斗状态来自服务器world投影，战斗期间整个面板不可见，连接继续接收无正文更新提示。
 */
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref} from 'vue'
import {useRoute} from 'vue-router'
import {useQueryClient} from '@tanstack/vue-query'
import {useWorld, worldOptions} from '@/features/world/queries'
import {sessionGeneration} from '@/features/auth/session'
import {api, HttpError} from '@/shared/http'
import {connectChat} from './realtime'
import type {Channel, ChatMessage, ChatPage, ChatSummary} from './types'

/**
 * 每次面板挂载独占会话、草稿及连接，卸载时清理，不能在模块顶层缓存或跨账号复用。
 * 视图提供实际滚动容器与输入框：分页补偿、已读确认和好友私聊聚焦使用同一份 DOM 引用。
 */
export function useChatSession(scroller: Ref<HTMLElement | undefined>, composer: Ref<HTMLTextAreaElement | undefined>) {
    const world = useWorld(),
        client = useQueryClient(),
        route = useRoute()
    const self = computed(() => world.data.value?.character?.id)
    const battleLocked = ref(false)
    const canView = computed(
        () => Boolean(self.value) && !world.data.value?.character?.activeBattleId && !battleLocked.value,
    )
    const expanded = ref(false),
        channel = ref<Channel>('WORLD'),
        peer = ref('')
    const recipientDraft = ref(''),
        draft = ref(''),
        error = ref(''),
        connected = ref(false)
    const summary = ref<ChatSummary>(),
        messages = ref<ChatMessage[]>([]),
        next = ref<string | null>(null)
    const busy = ref(false),
        loading = ref(false),
        settingsOpen = ref(false),
        sound = ref(false)
    const privateList = ref(false)

    const menu = ref<ChatMessage>()
    const confirmingBlock = ref(false)
    const newMessages = ref(0)
    // 草稿与幂等请求按频道/对端隔离，只在当前组件生命周期存在；换号卸载即丢弃。
    const drafts = new Map<string, string>()
    const requests = new Map<string, {id: string; channel: Channel; peer: string; text: string}>()
    const contextKey = (value: Channel, name: string) => `${value}:${value === 'PRIVATE' ? name : ''}`
    const privateUnread = computed(() => summary.value?.conversations.reduce((sum, item) => sum + item.unread, 0) ?? 0)
    const showingHistory = computed(() => !settingsOpen.value && !(channel.value === 'PRIVATE' && privateList.value))
    const nearBottom = () =>
        !scroller.value || scroller.value.scrollHeight - scroller.value.scrollTop - scroller.value.clientHeight < 64

    async function jumpToLatest() {
        await nextTick()
        scroller.value?.scrollTo({top: scroller.value.scrollHeight})
        newMessages.value = 0
        await load()
    }
    function onScroll() {
        if (nearBottom() && newMessages.value) {
            newMessages.value = 0
            void load()
        }
    }

    async function blockSelected() {
        if (!menu.value) return
        await mutate('/api/chat/blocks', 'POST', {name: menu.value.name})
        if (!error.value) menu.value = undefined
    }
    function showConversations() {
        privateList.value = true
        select('PRIVATE', peer.value)
    }
    function openConversation(name: string) {
        const unchanged = channel.value === 'PRIVATE' && peer.value === name
        privateList.value = false
        select('PRIVATE', name)
        // 换对端交给频道监听器加载；同一对端从列表返回时没有状态变化，才主动刷新。
        if (unchanged) void nextTick(() => load())
    }
    function closeSettings() {
        settingsOpen.value = false
        void nextTick(() => load())
    }
    function retryConnection() {
        // 只由明确重试清除旧错误，后台 SSE 刷新不能悄悄抹掉发送失败提示。
        error.value = ''
        void refresh(true)
    }
    const unread = computed(
        () =>
            (summary.value?.conversations.reduce((sum, item) => sum + item.unread, 0) ?? 0) +
            (summary.value?.systemUnread ?? 0),
    )
    const title = computed(() =>
        channel.value === 'PRIVATE'
            ? `私聊 · ${peer.value || '选择玩家'}`
            : channel.value === 'WORLD'
              ? '世界聊天'
              : '系统通知',
    )
    const length = computed(() => Array.from(draft.value.trim()).length)
    let alive = true,
        version = 0,
        stop: (() => void) | undefined
    let pendingWorld = false
    let refreshing = false,
        pending = false,
        previousPrivateUnread: number | undefined
    const identityGeneration = sessionGeneration()
    const valid = () => alive && identityGeneration === sessionGeneration()

    /** 角色偏好使用容错读取；无痕模式、存储禁用或旧版本坏数据不会阻止聊天。 */
    function restorePreference() {
        try {
            const raw = localStorage.getItem(`avalon.chat.${self.value}`)
            const value = raw ? JSON.parse(raw) : null
            if (value && ['WORLD', 'PRIVATE', 'SYSTEM'].includes(value.channel)) {
                channel.value = value.channel
                expanded.value = value.expanded === true
                peer.value = typeof value.peer === 'string' ? value.peer.slice(0, 32) : ''
                sound.value = value.sound === true
                privateList.value = channel.value === 'PRIVATE' && !peer.value
            }
        } catch {
            /* 存储不可用时保留默认世界频道与关闭声音。 */
        }
    }
    function remember() {
        if (!self.value) return
        try {
            localStorage.setItem(
                `avalon.chat.${self.value}`,
                JSON.stringify({
                    expanded: expanded.value,
                    channel: channel.value,
                    peer: peer.value,
                    sound: sound.value,
                }),
            )
        } catch {
            /* 偏好保存失败不影响消息发送与服务器已读。 */
        }
    }

    /** 可选声音只用于新增私聊未读，首次加载离线历史不响；后台/战斗中不泄漏通知提示。 */
    function notifySound(count: number) {
        if (sound.value && canView.value && previousPrivateUnread !== undefined && count > previousPrivateUnread) {
            try {
                const audio = new AudioContext(),
                    oscillator = audio.createOscillator(),
                    gain = audio.createGain()
                oscillator.connect(gain)
                gain.connect(audio.destination)
                oscillator.frequency.value = 660
                gain.gain.value = 0.03
                oscillator.start()
                oscillator.stop(audio.currentTime + 0.12)
                oscillator.onended = () => {
                    void audio.close()
                }
            } catch {
                /* 浏览器没有授权播放时静默保留未读标记。 */
            }
        }
        previousPrivateUnread = count
    }

    /**
     * 多个提交提示合并读取，正在读取时再到达的更新在本轮结束后补一轮。
     * 每次先读取world确认战斗状态，再读聊天；服务端409立即清空可见状态，迟到响应不能重新打开面板。
     */
    async function refresh(checkWorld = false) {
        if (!valid()) return
        pendingWorld ||= checkWorld
        if (refreshing) {
            pending = true
            return
        }
        refreshing = true
        try {
            do {
                pending = false
                if (pendingWorld) {
                    pendingWorld = false
                    await client.fetchQuery({...worldOptions(), staleTime: 0})
                    if (!valid()) return
                    battleLocked.value = Boolean(world.data.value?.character?.activeBattleId)
                }
                if (!canView.value) return
                const data = await api<ChatSummary>('/api/chat')
                if (!valid() || !canView.value) return
                summary.value = data
                notifySound(data.conversations.reduce((sum, item) => sum + item.unread, 0))
                if (expanded.value && showingHistory.value) await load()
            } while (pending && valid())
        } catch (cause) {
            fail(cause)
        } finally {
            refreshing = false
        }
    }
    function fail(cause: unknown) {
        if (!valid()) return
        if (cause instanceof HttpError && cause.status === 409) {
            battleLocked.value = true
            messages.value = []
            summary.value = undefined
            void client.invalidateQueries({queryKey: ['world']})
        } else error.value = cause instanceof HttpError ? cause.message : '聊天暂时不可用，请稍后重试'
    }

    /** 频道切换递增version，旧频道迟到的历史不能覆盖当前频道。SSE刷新只更新最新页并合并已加载历史。 */
    async function load(older = false) {
        if (
            !valid() ||
            !canView.value ||
            !expanded.value ||
            !showingHistory.value ||
            (channel.value === 'PRIVATE' && !peer.value)
        )
            return
        const current = version,
            selected = channel.value,
            target = peer.value
        loading.value = true
        try {
            const params = new URLSearchParams({channel: selected})
            if (selected === 'PRIVATE') params.set('peer', target)
            if (older && next.value) params.set('before', next.value)
            const previousNewest = messages.value.at(-1)?.id
            const page = await api<ChatPage>(`/api/chat/messages?${params}`)
            if (!older && previousNewest) {
                // 断线期间可能积压超过50条；向前分页直到遇到原最新消息，再合并缓存，不跳过中间页。
                let cursor = page.next
                while (cursor && page.entries.at(-1) && BigInt(page.entries.at(-1)!.id) > BigInt(previousNewest)) {
                    if (!valid() || !canView.value || current !== version) return
                    params.set('before', cursor)
                    const gap = await api<ChatPage>(`/api/chat/messages?${params}`)
                    page.entries.push(...gap.entries)
                    cursor = gap.next
                    if (gap.entries.length === 0) break
                }
            }
            if (!valid() || !canView.value || current !== version) return
            // 网络等待期间用户可能已经向上滚动，因此在合并前读取位置，不能沿用请求发出时的位置。
            // 所有并发响应都与此刻缓存比较，防止同一条 SSE 消息被重复计入新消息提示。
            const oldHeight = scroller.value?.scrollHeight ?? 0
            const oldTop = scroller.value?.scrollTop ?? 0
            const follow = nearBottom() || messages.value.length === 0
            const known = new Set(messages.value.map((item) => item.id))
            const newest = messages.value.at(-1)?.id
            const combined = new Map(messages.value.map((item) => [item.id, item]))
            // 新页中的已读投影覆盖旧缓存；公开历史只保留七天，重连时同步丢弃过期缓存。
            for (const item of page.entries) combined.set(item.id, item)
            const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000
            messages.value = [...combined.values()]
                .filter(
                    (item) =>
                        !summary.value?.blocked.some((block) => block.character === item.sender) &&
                        (item.channel === 'PRIVATE' || item.recipient !== null || Date.parse(item.createdAt) >= cutoff),
                )
                .sort((a, b) => (BigInt(a.id) < BigInt(b.id) ? -1 : BigInt(a.id) > BigInt(b.id) ? 1 : 0))
            if (older || messages.value.length <= 50) next.value = page.next
            await nextTick()
            if (!valid() || current !== version || !showingHistory.value) return
            if (older) {
                if (scroller.value) scroller.value.scrollTop = oldTop + scroller.value.scrollHeight - oldHeight
            } else if (follow) {
                scroller.value?.scrollTo({top: scroller.value.scrollHeight})
                newMessages.value = 0
            } else if (newest) {
                newMessages.value += messages.value.filter(
                    (item) => !known.has(item.id) && BigInt(item.id) > BigInt(newest),
                ).length
            }
            // 只有面板展示且页面在前台才确认已读；战斗隐藏、折叠和后台标签页都不自动消除未读。
            const latest = page.entries[0]
            if (
                latest &&
                selected !== 'WORLD' &&
                document.visibilityState === 'visible' &&
                expanded.value &&
                canView.value &&
                current === version &&
                showingHistory.value &&
                nearBottom()
            ) {
                await api('/api/chat/read', 'POST', {channel: selected, peer: target || null, through: latest.id})
                if (valid() && canView.value) {
                    const updated = await api<ChatSummary>('/api/chat')
                    if (valid() && canView.value && current === version) summary.value = updated
                }
            }
        } catch (cause) {
            if (current === version) fail(cause)
        } finally {
            if (current === version) loading.value = false
        }
    }

    /** 写失败保留原草稿和请求号；响应丢失后重试同一内容不会重复发消息，改变内容则创建新请求号。 */
    async function send() {
        if (busy.value || !canView.value || channel.value === 'SYSTEM' || length.value < 1 || length.value > 500) return
        busy.value = true
        error.value = ''
        const text = draft.value.trim(),
            selected = channel.value,
            target = peer.value,
            current = version
        const key = contextKey(selected, target)
        let request = requests.get(key)
        if (!request || request.text !== text || request.channel !== selected || request.peer !== target)
            request = {id: crypto.randomUUID(), channel: selected, peer: target, text}
        requests.set(key, request)
        const sending = request
        try {
            await api('/api/chat/messages', 'POST', {
                requestId: sending.id,
                channel: selected,
                peer: target || null,
                text,
            })
            if (!valid()) return
            requests.delete(key)
            if (drafts.get(key)?.trim() === text) drafts.delete(key)
            if (current === version && draft.value.trim() === text) draft.value = ''
            await refresh()
        } catch (cause) {
            fail(cause)
        } finally {
            busy.value = false
        }
    }
    function select(value: Channel, name = '') {
        channel.value = value
        if (value === 'PRIVATE') peer.value = name
        if (value === 'PRIVATE' && !name) privateList.value = true
        menu.value = undefined
    }
    /**
     * 好友列表共用现有私聊逻辑，不创建第二份会话状态或绕过战斗门禁。
     * 已选中同一好友时保留未发送草稿；切换好友由频道监听器保存旧草稿、恢复对端草稿并清理旧历史。
     * 关闭设置并展开面板后才聚焦输入框，手机端也能直接开始输入。
     */
    async function openPrivate(name: string) {
        const target = name.trim()
        if (!valid() || !canView.value || !target) return
        openConversation(target)
        settingsOpen.value = false
        recipientDraft.value = ''
        expanded.value = true
        await nextTick()
        if (valid() && canView.value && expanded.value && channel.value === 'PRIVATE' && peer.value === target)
            composer.value?.focus()
    }
    function contact() {
        const name = recipientDraft.value.trim()
        if (name) {
            openConversation(name)
            recipientDraft.value = ''
            settingsOpen.value = false
        }
    }
    async function mutate(path: string, method: string, body?: unknown) {
        if (busy.value || !canView.value) return
        busy.value = true
        error.value = ''
        try {
            await api(path, method, body)
            // 拉黑后必须丢弃旧正文缓存，不能只让新查询过滤而继续显示旧发言。
            messages.value = []
            next.value = null
            await refresh()
        } catch (cause) {
            fail(cause)
        } finally {
            busy.value = false
        }
    }
    watch([channel, peer], ([selected, target], [oldChannel, oldPeer]) => {
        drafts.set(contextKey(oldChannel, oldPeer), draft.value)
        version++
        messages.value = []
        next.value = null
        newMessages.value = 0
        loading.value = false
        draft.value = drafts.get(contextKey(selected, target)) ?? ''
        error.value = ''
        remember()
        void load()
    })
    watch([expanded, sound], () => {
        remember()
        if (expanded.value) void refresh()
    })
    watch(canView, (allowed) => {
        version++
        if (!allowed) {
            messages.value = []
            summary.value = undefined
            settingsOpen.value = false
            menu.value = undefined
        } else void refresh()
    })
    // 路由变化使上次门禁失败重新核对服务器；不会用URL假定战斗已经结束。
    watch(
        () => route.path,
        () => void refresh(true),
    )
    function visibility() {
        if (document.visibilityState === 'visible') void refresh(true)
    }
    onMounted(() => {
        restorePreference()
        stop = connectChat(
            () => void refresh(true),
            (value) => {
                connected.value = value
            },
        )
        document.addEventListener('visibilitychange', visibility)
        void refresh(true)
    })
    onBeforeUnmount(() => {
        alive = false
        version++
        stop?.()
        messages.value = []
        summary.value = undefined
        draft.value = ''
        drafts.clear()
        requests.clear()
        document.removeEventListener('visibilitychange', visibility)
    })

    return {
        self,
        canView,
        expanded,
        channel,
        peer,
        recipientDraft,
        draft,
        error,
        connected,
        summary,
        messages,
        next,
        busy,
        loading,
        settingsOpen,
        sound,
        privateList,
        menu,
        confirmingBlock,
        newMessages,
        privateUnread,
        unread,
        title,
        length,
        jumpToLatest,
        onScroll,
        blockSelected,
        showConversations,
        openConversation,
        closeSettings,
        retryConnection,
        load,
        send,
        select,
        openPrivate,
        contact,
        mutate,
    }
}
