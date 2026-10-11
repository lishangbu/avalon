import type {InjectionKey} from 'vue'

/**
 * 页面只提出打开指定玩家私聊的意图，面板负责频道切换、历史与战斗门禁。
 * 回调由应用壳提供并指向当前挂载的面板，不缓存请求或消息；换号销毁面板后，
 * 旧身份的打开意图不会排队交给新身份，也不会通过URL暴露或持久化角色名。
 */
export const openPrivateChatKey: InjectionKey<(name: string) => void> = Symbol('openPrivateChat')
