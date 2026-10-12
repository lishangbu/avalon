package io.github.lishangbu.avalon.chat.application.model;

import java.time.Instant;

/**
 * 一条鉴权后的消息投影。正文保持纯文本，发送者公开名为发送时快照，不查询对方账号资料。
 *
 * @param id 消息雪花编号；仅作稳定引用和分页，不能当作数据库提交水位。
 * @param channel WORLD为全服玩家频道，PRIVATE为本人参与的私聊，SYSTEM为可信游戏通知。
 * @param sender 发件角色编号，系统为空；不能接受客户端伪造此值。
 * @param name 玩家公开角色名或“系统”，不暴露登录用户名。
 * @param recipient 私聊/个人通知的收件角色编号；世界和全服公告为空。
 * @param text 经格式校验的原文，浏览器仅通过转义插值展示。
 * @param createdAt 持久化时间；以UTC瞬间传输，由浏览器转换显示时区。
 * @param read 私聊表示真正收件人已读，系统表示当前查看者已读，不代表连接收到了提示。
 */
public record ChatMessage(String id, String channel, String sender, String name, String recipient, String text,
		Instant createdAt, boolean read) {
}
