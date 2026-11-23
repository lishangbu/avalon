<script setup lang="ts">
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import type {ChatMessage} from './types'
import {useChatSession} from './useChatSession'

const scroller = ref<HTMLElement>()
const composer = ref<HTMLTextAreaElement>()
const {
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
} = useChatSession(scroller, composer)
const viewportHeight = ref<number>()
const viewportTop = ref(0)
const menuElement = ref<HTMLElement>()
let menuTrigger: HTMLElement | undefined

/** 视图只维护键盘、焦点、消息时间显示与软键盘视口；会话状态由 useChatSession 管理。 */
function resizeViewport() {
  viewportHeight.value = window.visualViewport?.height
  viewportTop.value = window.visualViewport?.offsetTop ?? 0
}

/** 日期按中国区域显示；五分钟时间分隔由 showTime 决定。 */
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

/** 桌面 Enter 发送，触屏保留换行；输入法确认和组合按键不触发发送。 */
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

/** 菜单内循环焦点，关闭后返回触发按钮。 */
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

onMounted(() => {
  resizeViewport()
  window.visualViewport?.addEventListener('resize', resizeViewport)
  window.visualViewport?.addEventListener('scroll', resizeViewport)
})
onBeforeUnmount(() => {
  window.visualViewport?.removeEventListener('resize', resizeViewport)
  window.visualViewport?.removeEventListener('scroll', resizeViewport)
})
defineExpose({openPrivate})
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
                    <button v-if="item.sender && item.sender !== self" @click="openMenu(item)">
                      {{ item.name }}</button><strong v-else>{{ item.name }}</strong>
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

<style scoped src="./chat-panel.css"></style>
