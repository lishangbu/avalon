/** 聊天协议编号始终保持字符串，频道只表示显示选择，服务端负责身份和战斗权限。 */
export type Channel = 'WORLD' | 'PRIVATE' | 'SYSTEM'
/** read在私聊中表示收件人已读；接收更新提示与阅读消息是两种不同动作。 */
export interface ChatMessage {
    id: string
    channel: Channel
    sender: string | null
    name: string
    recipient: string | null
    text: string
    createdAt: string
    read: boolean
}
/** 最新页按新到旧返回；面板按时间方向倒序展示，next用于获取更早历史。 */
export interface ChatPage {
    entries: ChatMessage[]
    next: string | null
}
/** 摘要不携带正文；私聊会话只包含公开角色资料，黑名单只由本人管理。 */
export interface ChatSummary {
    character: string
    rejectStrangers: boolean
    conversations: {character: string; name: string; unread: number; latest: string}[]
    systemUnread: number
    blocked: {character: string; name: string}[]
}
