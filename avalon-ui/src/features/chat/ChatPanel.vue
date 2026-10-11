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
let request: {id: string; channel: Channel; peer: string; text: string} | undefined
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
      error.value = ''
      notifySound(data.conversations.reduce((sum, item) => sum + item.unread, 0))
      if (expanded.value) await load()
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
  if (!valid() || !canView.value || !expanded.value || (channel.value === 'PRIVATE' && !peer.value)) return
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
    if (!older) {
      await nextTick()
      scroller.value?.scrollTo({top: scroller.value.scrollHeight})
    }
    // 只有面板展示且页面在前台才确认已读；战斗隐藏、折叠和后台标签页都不自动消除未读。
    const latest = page.entries[0]
    if (
      latest &&
      selected !== 'WORLD' &&
      document.visibilityState === 'visible' &&
      expanded.value &&
      canView.value &&
      current === version
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
  if (!request || request.text !== text || request.channel !== selected || request.peer !== target)
    request = {id: crypto.randomUUID(), channel: selected, peer: target, text}
  const sending = request
  try {
    await api('/api/chat/messages', 'POST', {requestId: sending.id, channel: selected, peer: target || null, text})
    if (!valid()) return
    request = undefined
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
}
/**
 * 好友列表共用现有私聊逻辑，不创建第二份会话状态或绕过战斗门禁。
 * 已选中同一好友时保留未发送草稿；切换好友沿用频道监听器清理草稿与旧历史。
 * 关闭设置并展开面板后才聚焦输入框，手机端也能直接开始输入。
 */
async function openPrivate(name: string) {
  const target = name.trim()
  if (!valid() || !canView.value || !target) return
  select('PRIVATE', target)
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
    select('PRIVATE', name)
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
watch([channel, peer], () => {
  version++
  messages.value = []
  next.value = null
  draft.value = ''
  request = undefined
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
  document.removeEventListener('visibilitychange', visibility)
})
</script>

<template>
  <!-- 战斗期间连折叠按钮和未读预览也隐藏；后台SSE只记录唤醒，不渲染消息。 -->
  <aside v-if="canView" class="chat" aria-label="游戏聊天">
    <button class="chat-toggle" :aria-expanded="expanded" aria-controls="chat-content" @click="expanded = !expanded">
      {{ expanded ? '收起聊天' : '聊天' }} <span v-if="unread" :aria-label="`${unread}条未读消息`">{{ unread }}</span>
    </button>
    <section v-if="expanded" id="chat-content" class="chat-content">
      <header>
        <strong>{{ title }}</strong
        ><small>{{ connected ? '已连接' : '正在重连' }}</small
        ><button @click="settingsOpen = !settingsOpen">设置</button>
      </header>
      <nav aria-label="聊天频道">
        <button :aria-pressed="channel === 'WORLD'" @click="select('WORLD')">世界</button>
        <button :aria-pressed="channel === 'PRIVATE'" @click="select('PRIVATE', peer)">私聊</button>
        <button :aria-pressed="channel === 'SYSTEM'" @click="select('SYSTEM')">
          系统{{ summary?.systemUnread ? ` (${summary.systemUnread})` : '' }}
        </button>
      </nav>
      <p v-if="error" role="alert" class="chat-error">{{ error }}</p>
      <section v-if="settingsOpen" aria-label="聊天设置" class="chat-settings">
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
        <p v-if="!summary?.blocked.length">尚未拉黑玩家</p>
        <div v-for="item in summary?.blocked" :key="item.character">
          {{ item.name }}
          <button :disabled="busy" @click="mutate(`/api/chat/blocks/${item.character}`, 'DELETE')">解除拉黑</button>
        </div>
      </section>
      <template v-else>
        <form v-if="channel === 'PRIVATE'" class="contact" @submit.prevent="contact">
          <input v-model="recipientDraft" aria-label="私聊角色名" placeholder="输入角色名" maxlength="32" />
          <button type="submit" :disabled="!recipientDraft.trim()">联系</button>
        </form>
        <nav v-if="channel === 'PRIVATE' && summary?.conversations.length" aria-label="私聊会话" class="conversations">
          <button
            v-for="item in summary.conversations"
            :key="item.character"
            :aria-pressed="peer === item.name"
            @click="select('PRIVATE', item.name)"
          >
            {{ item.name }}{{ item.unread ? ` (${item.unread})` : '' }}
          </button>
        </nav>
        <div ref="scroller" class="chat-history" role="log" aria-label="聊天消息" aria-live="polite">
          <button v-if="next" :disabled="loading" @click="load(true)">加载更早消息</button>
          <p v-if="!messages.length">
            {{ loading ? '加载中…' : channel === 'PRIVATE' && !peer ? '选择会话或输入角色名开始私聊' : '暂无消息' }}
          </p>
          <article v-for="item in messages" :key="item.id" :class="{mine: item.sender === self}">
            <div class="message-meta">
              <button v-if="item.sender && item.sender !== self" @click="select('PRIVATE', item.name)">
                {{ item.name }}</button
              ><strong v-else>{{ item.name }}</strong>
              <time :datetime="item.createdAt">{{
                new Date(item.createdAt).toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})
              }}</time>
              <small v-if="item.channel === 'PRIVATE' && item.sender === self">{{ item.read ? '已读' : '未读' }}</small>
              <button
                v-if="item.sender && item.sender !== self"
                :disabled="busy"
                :aria-label="`拉黑${item.name}`"
                @click="mutate('/api/chat/blocks', 'POST', {name: item.name})"
              >
                拉黑
              </button>
            </div>
            <!-- 插值始终转义玩家输入，禁止v-html；换行用CSS呈现，不生成可执行链接或脚本。 -->
            <p>{{ item.text }}</p>
          </article>
        </div>
        <form v-if="channel !== 'SYSTEM'" class="compose" @submit.prevent="send">
          <textarea
            ref="composer"
            v-model="draft"
            aria-label="聊天内容"
            placeholder="输入消息（最多500字）"
            rows="2"
            :disabled="busy || (channel === 'PRIVATE' && !peer)"
          />
          <div>
            <small>{{ length }}/500</small
            ><button type="submit" :disabled="busy || !length || length > 500 || (channel === 'PRIVATE' && !peer)">
              {{ busy ? '发送中…' : '发送' }}
            </button>
          </div>
        </form>
        <p v-else class="system-hint">系统通知由游戏事件自动生成</p>
      </template>
    </section>
  </aside>
</template>

<style scoped>
/* 面板位于游戏底栏上方，窄屏宽度与安全区共同约束；独立历史滚动不会把导航挤出屏幕。 */
.chat {
  position: fixed;
  z-index: 1000;
  bottom: calc(72px + var(--ion-safe-area-bottom, 0px));
  /* Ionic的ion-app建立固定定位包含块，右边距相对游戏容器计算，不能再次加桌面视口居中偏移。 */
  right: max(12px, var(--ion-safe-area-right, 0px));
  max-width: calc(100vw - 24px);
  color: var(--ion-text-color);
}
.chat-toggle {
  display: block;
  margin-left: auto;
  border: 1px solid var(--game-border);
  border-radius: 18px;
  padding: 10px 16px;
  background: var(--game-surface);
  color: var(--ion-color-primary);
  min-height: 44px;
}
.chat-toggle span {
  background: var(--ion-color-primary);
  color: var(--ion-color-primary-contrast);
  border-radius: 12px;
  padding: 2px 6px;
}
.chat-content {
  display: flex;
  flex-direction: column;
  width: 420px;
  max-width: calc(100vw - 24px);
  max-height: calc(100dvh - 180px);
  overflow-y: auto;
  margin-top: 6px;
  padding: 12px;
  background: var(--game-surface);
  border: 1px solid var(--game-border);
  border-radius: 12px;
  box-shadow: 0 8px 24px #0002;
}
/* 高度不足时只压缩可滚动历史，频道、输入框与发送按钮保持完整；极矮横屏允许整个面板滚动。 */
.chat-content > * {
  flex-shrink: 0;
}
.contact input {
  min-width: 0;
}
.contact button {
  flex-shrink: 0;
  white-space: nowrap;
}
header,
nav,
.contact,
.compose div,
.message-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}
header {
  justify-content: space-between;
}
nav {
  margin: 8px 0;
}
button {
  min-height: 36px;
  border: 1px solid var(--game-border);
  border-radius: 6px;
  background: var(--game-tint);
  color: var(--ion-text-color);
  padding: 6px 10px;
}
button[aria-pressed='true'] {
  border-color: var(--ion-color-primary);
  color: var(--ion-color-primary);
}
small,
time {
  color: var(--game-muted);
  font-size: 12px;
}
input:not([type='checkbox']),
textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--game-border);
  border-radius: 6px;
  background: var(--game-surface);
  color: var(--ion-text-color);
  font: inherit;
}
textarea {
  resize: vertical;
  min-height: 50px;
  max-height: 120px;
}
.conversations {
  overflow-x: auto;
  white-space: nowrap;
}
.chat-history {
  flex: 1 1 auto;
  height: min(32dvh, 280px);
  min-height: 80px;
  overflow-y: auto;
  overscroll-behavior: contain;
}
article {
  border-bottom: 1px solid var(--game-border);
  padding: 8px 0;
  overflow-wrap: anywhere;
}
article p {
  white-space: pre-wrap;
  margin: 4px 0;
  font-size: 14px;
}
.message-meta {
  flex-wrap: wrap;
  gap: 6px;
}
.message-meta button {
  padding: 2px 6px;
  min-height: 30px;
}
.mine strong {
  color: var(--ion-color-primary);
}
.compose {
  margin-top: 8px;
}
.compose div {
  justify-content: space-between;
  margin-top: 4px;
}
.chat-settings label {
  display: flex;
  gap: 8px;
  padding: 10px 0;
}
.chat-error {
  color: var(--game-enemy);
  font-size: 14px;
}
.system-hint {
  color: var(--game-muted);
  font-size: 12px;
  margin-bottom: 0;
}
</style>
