<script setup lang="ts">
/**
 * 全局角色级聊天面板：登录换号时由应用壳重建，流连接与消息状态不会跨账号复用。
 * 面板展开、频道和对端只保存到角色专属localStorage键；正文、草稿、黑名单不写浏览器持久化。
 * 战斗状态来自服务器world投影，战斗期间整个面板不可见，连接继续接收无正文更新提示。
 */
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {useQueryClient} from '@tanstack/vue-query'
import {useWorld, worldOptions} from '@/features/world/queries'
import {sessionGeneration} from '@/features/auth/session'
import {api, HttpError} from '@/shared/http'
import {connectChat} from './realtime'
import type {Channel, ChatMessage, ChatPage, ChatSummary} from './types'

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
const scroller = ref<HTMLElement>()
const composer = ref<HTMLTextAreaElement>()
const privateList = ref(false)
const viewportHeight = ref<number>()
const viewportTop = ref(0)
/** visualViewport 随软键盘缩小，避免输入框落在键盘下方；仅改变手机展开面板的尺寸。 */
function resizeViewport() {
  viewportHeight.value = window.visualViewport?.height
  viewportTop.value = window.visualViewport?.offsetTop ?? 0
}
const menu = ref<ChatMessage>()
const menuElement = ref<HTMLElement>()
let menuTrigger: HTMLElement | undefined
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
/** 日期和时间统一使用中国区域显示；按五分钟间隔插入时间，避免每条消息重复占位。 */
function dateLabel(value: string) {
  return new Date(value).toLocaleDateString('zh-CN', {year: 'numeric', month: 'long', day: 'numeric'})
}
function timeLabel(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})
}
function showTime(index: number) {
  return (
    index === 0 ||
    Date.parse(messages.value[index]!.createdAt) - Date.parse(messages.value[index - 1]!.createdAt) >= 300_000
  )
}
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
/** 触屏保留原生换行；桌面仅处理无修饰 Enter，组合输入和候选确认绝不能触发发送。 */
function onComposerKey(event: KeyboardEvent) {
  if (
    event.key !== 'Enter' ||
    event.shiftKey ||
    event.ctrlKey ||
    event.altKey ||
    event.metaKey ||
    event.isComposing ||
    event.keyCode === 229
  )
    return
  if (!matchMedia('(hover: hover) and (pointer: fine)').matches) return
  event.preventDefault()
  void send()
}
function openMenu(item: ChatMessage) {
  menuTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : undefined
  menu.value = item
  confirmingBlock.value = false
}
/** 操作菜单打开后限制 Tab 在菜单内循环，关闭时返回原姓名按钮，保持键盘操作连续。 */
function onMenuKey(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    menu.value = undefined
  } else if (event.key === 'Tab') {
    const buttons = [...(menuElement.value?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [])]
    const current = buttons.indexOf(document.activeElement as HTMLButtonElement)
    event.preventDefault()
    buttons[(current + (event.shiftKey ? -1 : 1) + buttons.length) % buttons.length]?.focus()
  }
}
watch([menu, confirmingBlock], async ([value]) => {
  await nextTick()
  if (value) menuElement.value?.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus()
  else if (menuTrigger?.isConnected) menuTrigger.focus()
})
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
      JSON.stringify({expanded: expanded.value, channel: channel.value, peer: peer.value, sound: sound.value}),
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
    await api('/api/chat/messages', 'POST', {requestId: sending.id, channel: selected, peer: target || null, text})
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
defineExpose({openPrivate})
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
  resizeViewport()
  window.visualViewport?.addEventListener('resize', resizeViewport)
  window.visualViewport?.addEventListener('scroll', resizeViewport)
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
  window.visualViewport?.removeEventListener('resize', resizeViewport)
  window.visualViewport?.removeEventListener('scroll', resizeViewport)
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
</script>

<template>
  <!-- Teleport 绕开 ion-app 的固定定位包含块：宽屏才能利用游戏外侧空白，手机仍覆盖可用视口。 -->
  <Teleport to="body">
    <aside
      v-if="canView"
      class="chat"
      :style="{'--chat-height': viewportHeight ? `${viewportHeight}px` : undefined, '--chat-top': `${viewportTop}px`}"
      :class="{'is-expanded': expanded}"
      aria-label="游戏聊天"
    >
      <button class="chat-toggle" :aria-expanded="expanded" aria-controls="chat-content" @click="expanded = !expanded">
        {{ expanded ? '收起聊天' : '聊天' }} <span v-if="unread" :aria-label="`${unread}条未读消息`">{{ unread }}</span>
      </button>
      <section v-if="expanded" id="chat-content" class="chat-content" aria-label="聊天面板">
        <header class="chat-header">
          <div>
            <small class="eyebrow">AVALON · 冒险者通讯</small><strong>{{ settingsOpen ? '聊天设置' : title }}</strong>
          </div>
          <button v-if="settingsOpen" @click="closeSettings">返回聊天</button>
          <button v-else @click="settingsOpen = true">设置</button>
          <button aria-label="关闭聊天" @click="expanded = false">关闭</button>
        </header>
        <p v-if="!connected" class="connection-status" role="status">连接中断，正在重连…</p>
        <nav class="channel-tabs" aria-label="聊天频道">
          <button :aria-pressed="channel === 'WORLD'" @click="select('WORLD')">世界</button>
          <button aria-label="私聊" :aria-pressed="channel === 'PRIVATE'" @click="showConversations">
            私聊<span v-if="privateUnread" class="badge">{{ privateUnread }}</span>
          </button>
          <button aria-label="系统" :aria-pressed="channel === 'SYSTEM'" @click="select('SYSTEM')">
            系统<span v-if="summary?.systemUnread" class="badge">{{ summary.systemUnread }}</span>
          </button>
        </nav>
        <p v-if="error" role="alert" class="chat-error">
          {{ error }} <button @click="retryConnection">重试连接</button>
        </p>
        <!-- 设置单独呈现，但保留历史 DOM 与滚动位置；返回不重新创建滚动容器。 -->
        <section v-if="settingsOpen" aria-label="聊天设置" class="chat-settings">
          <h3>消息偏好</h3>
          <label
            ><input
              type="checkbox"
              :checked="summary?.rejectStrangers"
              :disabled="busy"
              @change="
                mutate('/api/chat/settings', 'PUT', {rejectStrangers: ($event.target as HTMLInputElement).checked})
              "
            />拒收陌生人私聊</label
          >
          <label><input v-model="sound" type="checkbox" />私聊提示音</label>
          <h3>黑名单</h3>
          <p v-if="!summary?.blocked.length" class="empty-state">尚未拉黑玩家</p>
          <div v-for="item in summary?.blocked" :key="item.character" class="blocked-row">
            {{ item.name
            }}<button :disabled="busy" @click="mutate(`/api/chat/blocks/${item.character}`, 'DELETE')">解除拉黑</button>
          </div>
        </section>
        <div v-show="!settingsOpen" class="chat-body">
          <section v-if="channel === 'PRIVATE' && privateList" class="conversation-screen">
            <form class="contact" @submit.prevent="contact">
              <input
                v-model="recipientDraft"
                aria-label="私聊角色名"
                placeholder="输入角色名，开始新私聊"
                maxlength="32"
              /><button type="submit" :disabled="!recipientDraft.trim()">联系</button>
            </form>
            <nav aria-label="私聊会话" class="conversations">
              <button
                v-for="item in summary?.conversations"
                :key="item.character"
                class="conversation"
                @click="openConversation(item.name)"
              >
                <span class="avatar" aria-hidden="true">{{ Array.from(item.name)[0] }}</span>
                <span class="conversation-info"
                  ><strong>{{ item.name }}</strong
                  ><small>{{ item.unread ? '有新消息，点击查看' : '点击继续聊天' }}</small></span
                >
                <span v-if="item.unread" class="badge">{{ item.unread }}</span>
              </button>
            </nav>
            <p v-if="!summary?.conversations.length" class="empty-state">
              还没有私聊会话<br />输入角色名，或从好友列表开始聊天
            </p>
          </section>
          <div v-show="!(channel === 'PRIVATE' && privateList)" class="thread">
            <div v-if="channel === 'PRIVATE'" class="thread-heading">
              <button @click="privateList = true">返回会话</button><strong>{{ peer }}</strong>
            </div>
            <div
              ref="scroller"
              class="chat-history"
              :class="channel.toLowerCase()"
              role="log"
              aria-label="聊天消息"
              aria-live="polite"
              @scroll.passive="onScroll"
            >
              <button v-if="next" class="load-older" :disabled="loading" @click="load(true)">加载更早消息</button>
              <p v-if="!messages.length" class="empty-state">{{ loading ? '加载中…' : '暂无消息' }}</p>
              <template v-for="(item, index) in messages" :key="item.id">
                <div
                  v-if="index === 0 || dateLabel(item.createdAt) !== dateLabel(messages[index - 1]!.createdAt)"
                  class="date-divider"
                >
                  {{ dateLabel(item.createdAt) }}
                </div>
                <time v-if="showTime(index)" class="time-divider" :datetime="item.createdAt">{{
                  timeLabel(item.createdAt)
                }}</time>
                <article :class="{mine: item.sender === self, notification: channel === 'SYSTEM'}">
                  <div class="message-meta">
                    <button v-if="item.sender && item.sender !== self" @click="openMenu(item)">{{ item.name }}</button
                    ><strong v-else>{{ item.name }}</strong>
                  </div>
                  <!-- 玩家文本始终经插值转义；仅用 CSS 保留换行，不能使用 v-html。 -->
                  <p class="message-text">{{ item.text }}</p>
                  <small v-if="item.channel === 'PRIVATE' && item.sender === self" class="read-status">{{
                    item.read ? '已读' : '未读'
                  }}</small>
                </article>
              </template>
            </div>
            <button v-if="newMessages" class="new-messages" @click="jumpToLatest">{{ newMessages }} 条新消息 ↓</button>
            <form v-if="channel !== 'SYSTEM'" class="compose" @submit.prevent="send">
              <textarea
                ref="composer"
                v-model="draft"
                aria-label="聊天内容"
                placeholder="输入消息（最多500字）"
                rows="2"
                :disabled="busy || (channel === 'PRIVATE' && !peer)"
                @keydown="onComposerKey"
              />
              <div>
                <small>{{ length }}/500</small
                ><button
                  type="submit"
                  class="send-button"
                  :disabled="busy || !length || length > 500 || (channel === 'PRIVATE' && !peer)"
                >
                  {{ busy ? '发送中…' : '发送' }}
                </button>
              </div>
            </form>
            <p v-else class="system-hint">系统通知由游戏事件自动生成</p>
          </div>
        </div>
        <section
          v-if="menu"
          ref="menuElement"
          class="player-menu"
          role="dialog"
          aria-modal="true"
          :aria-label="`${menu.name}的操作`"
          @keydown="onMenuKey"
        >
          <strong>{{ menu.name }}</strong>
          <p v-if="confirmingBlock">确认拉黑该玩家？将不再显示其消息。</p>
          <button v-if="!confirmingBlock" @click="openConversation(menu.name)">私聊</button>
          <button v-if="!confirmingBlock" :aria-label="`拉黑${menu.name}`" @click="confirmingBlock = true">拉黑</button>
          <button v-else :disabled="busy" @click="blockSelected">确认拉黑</button>
          <button @click="menu = undefined">取消</button>
        </section>
      </section>
    </aside>
  </Teleport>
</template>

<style scoped>
/* 固定游戏仍为 480px，宽屏在其右侧停靠；窄桌面覆盖，触屏则占满动态视口。 */
.chat {
  position: fixed;
  z-index: 1000;
  right: max(12px, calc((100vw - 480px) / 2 + 12px));
  bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  color: var(--ion-text-color);
  font-size: 14px;
}
button {
  min-height: 40px;
  padding: 8px 12px;
  border: 1px solid var(--game-border);
  border-radius: 10px;
  background: var(--game-surface);
  color: var(--ion-text-color);
  font: inherit;
  cursor: pointer;
}
button:disabled {
  opacity: 0.5;
  cursor: default;
}
button:focus-visible,
input:focus-visible,
textarea:focus-visible {
  outline: 2px solid var(--ion-color-primary);
  outline-offset: 2px;
}
.chat-toggle {
  display: block;
  margin-left: auto;
  border-radius: 24px;
  box-shadow: 0 4px 20px #0002;
  color: var(--ion-color-primary);
}
.chat-toggle span,
.badge {
  display: inline-grid;
  place-items: center;
  min-width: 20px;
  padding: 2px 5px;
  border-radius: 12px;
  background: var(--ion-color-primary);
  color: var(--ion-color-primary-contrast);
  font-size: 12px;
}
.chat-content {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 420px;
  max-width: calc(100vw - 24px);
  height: min(700px, calc(100dvh - 156px));
  margin-top: 8px;
  overflow: hidden;
  border: 1px solid var(--game-border);
  border-radius: 18px;
  background: var(--game-surface);
  box-shadow: 0 16px 48px #0003;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--game-border);
}
.chat-header > div {
  flex: 1;
  min-width: 0;
}
.chat-header strong {
  display: block;
  overflow-wrap: anywhere;
  font-size: 16px;
}
.eyebrow {
  display: block;
  font-size: 10px;
  letter-spacing: 0.1em;
  margin-bottom: 5px;
}
small,
time,
.empty-state,
.system-hint {
  color: var(--game-muted);
}
.channel-tabs {
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  background: var(--game-tint);
}
.channel-tabs button {
  flex: 1;
  background: transparent;
  border-color: transparent;
}
.channel-tabs button[aria-pressed='true'] {
  background: var(--game-surface);
  color: var(--ion-color-primary);
  border-color: var(--game-border);
}
.channel-tabs .badge {
  margin-left: 5px;
}
.connection-status,
.chat-error {
  margin: 0;
  padding: 10px 16px;
  font-size: 12px;
  background: var(--game-tint);
}
.chat-error {
  color: var(--game-enemy);
}
.chat-body,
.thread {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}
.thread {
  position: relative;
}
.thread-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--game-border);
}
.thread-heading strong {
  overflow-wrap: anywhere;
}
.chat-history {
  flex: 1;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  overflow-anchor: none;
  padding: 12px 16px;
}
.empty-state {
  text-align: center;
  line-height: 1.8;
  padding: 32px 16px;
}
.date-divider,
.time-divider {
  display: block;
  text-align: center;
  color: var(--game-muted);
  font-size: 11px;
  margin: 12px 0;
}
.date-divider {
  font-size: 12px;
}
article {
  margin: 12px 0;
  overflow-wrap: anywhere;
}
.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--game-muted);
}
.message-meta button {
  border: 0;
  min-height: 30px;
  padding: 2px 0;
  background: transparent;
  color: var(--ion-color-primary);
}
.message-text {
  white-space: pre-wrap;
  margin: 4px 0;
  line-height: 1.65;
}
.private article {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.private article.mine {
  align-items: flex-end;
}
.private .message-text {
  max-width: 88%;
  border: 1px solid var(--game-border);
  padding: 10px 13px;
  border-radius: 4px 14px 14px;
  background: var(--game-tint);
}
.private .mine .message-text {
  border-radius: 14px 4px 14px 14px;
  background: var(--ion-color-primary);
  color: var(--ion-color-primary-contrast);
  border-color: transparent;
}
.read-status {
  font-size: 11px;
}
.world article {
  border-bottom: 1px solid var(--game-border);
  padding-bottom: 10px;
}
.notification {
  padding: 14px;
  border: 1px solid var(--game-border);
  border-radius: 12px;
  background: var(--game-tint);
}
.load-older {
  display: block;
  margin: auto;
  font-size: 12px;
}
.new-messages {
  position: absolute;
  bottom: 135px;
  align-self: center;
  box-shadow: 0 4px 16px #0002;
  color: var(--ion-color-primary);
}
.compose {
  padding: 12px;
  border-top: 1px solid var(--game-border);
}
.compose > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
input:not([type='checkbox']),
textarea {
  box-sizing: border-box;
  min-width: 0;
  width: 100%;
  padding: 10px;
  border: 1px solid var(--game-border);
  border-radius: 10px;
  background: var(--game-tint);
  color: var(--ion-text-color);
  font: inherit;
}
textarea {
  resize: none;
  min-height: 56px;
  max-height: 120px;
}
.send-button {
  background: var(--ion-color-primary);
  color: var(--ion-color-primary-contrast);
  border: 0;
  min-width: 72px;
}
.system-hint {
  text-align: center;
  font-size: 12px;
  padding: 10px;
}
.conversation-screen,
.chat-settings {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}
.contact {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.contact button {
  flex-shrink: 0;
}
.conversations {
  display: flex;
  flex-direction: column;
}
.conversation {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  border: 0;
  border-bottom: 1px solid var(--game-border);
  border-radius: 0;
  padding: 14px 6px;
}
.avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 14px;
  background: var(--game-tint);
  color: var(--ion-color-primary);
  font-size: 18px;
}
.conversation-info {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}
.conversation-info small {
  display: block;
  margin-top: 5px;
  font-size: 12px;
}
.chat-settings label,
.blocked-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid var(--game-border);
}
.blocked-row {
  justify-content: space-between;
}
.player-menu {
  position: absolute;
  inset: auto 12px 12px;
  padding: 18px;
  background: var(--game-surface);
  border: 1px solid var(--game-border);
  box-shadow: 0 0 0 100vmax #0005;
  border-radius: 14px;
  z-index: 2;
}
.player-menu strong {
  display: block;
  margin-bottom: 12px;
}
.player-menu button {
  margin: 4px;
}
@media (min-width: 1380px) {
  .chat {
    left: calc(50% + 256px);
    right: auto;
    bottom: 32px;
  }
  .chat-content {
    height: min(760px, calc(100dvh - 120px));
  }
}
@media (max-width: 600px) {
  .chat.is-expanded {
    inset: var(--chat-top, 0px) 0 auto;
    height: var(--chat-height, 100dvh);
  }
  .is-expanded > .chat-toggle {
    display: none;
  }
  .chat-content {
    box-sizing: border-box;
    width: 100%;
    max-width: none;
    height: 100%;
    margin: 0;
    border: 0;
    border-radius: 0;
    padding-top: env(safe-area-inset-top, 0px);
    padding-bottom: env(safe-area-inset-bottom, 0px);
  }
  .chat-header {
    padding: 12px;
  }
  .chat-history {
    padding: 10px 12px;
  }
}
@media (prefers-reduced-motion: no-preference) {
  button {
    transition:
      background 0.15s ease,
      color 0.15s ease;
  }
}
</style>
