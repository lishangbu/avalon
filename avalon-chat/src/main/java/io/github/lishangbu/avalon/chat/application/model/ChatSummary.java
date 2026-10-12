package io.github.lishangbu.avalon.chat.application.model;

import java.util.List;

/**
 * 本人会话摘要，不携带任何消息正文；战斗期间摘要也不可查询。
 *
 * @param character 当前认证账号对应的角色编号，作为面板与偏好隔离键。
 * @param rejectStrangers 是否只接收已接受好友的后续私聊；不删除之前的陌生人历史。
 * @param conversations 实际形成的私聊会话，按最近消息排序，不生成任意陌生人的预览。
 * @param systemUnread 本人的个人通知与尚未过期公告未读数，不包含其他人的私有通知。
 * @param blocked 本人主动建立的黑名单，公开名用于管理，不能查询被谁拉黑。
 */
public record ChatSummary(String character, boolean rejectStrangers, List<ChatConversation> conversations,
		long systemUnread, List<ChatBlocked> blocked) {
}
