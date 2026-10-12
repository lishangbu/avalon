package io.github.lishangbu.avalon.chat.application.model;

/**
 * 无正文的事务内更新事件，实时监听器仅在AFTER_COMMIT发送常量唤醒信号。
 *
 * @param sender 玩家消息的发件角色，系统为空；用于刷新发件人的已读显示或其他标签页。
 * @param recipient 收件角色，为空表示公开更新；个人通知只唤醒本人，私聊唤醒双方。
 */
public record ChatChanged(Long sender, Long recipient) {
}
