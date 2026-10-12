package io.github.lishangbu.avalon.chat.application.model;

/**
 * 玩家发送意图，任何字段都不是授权凭据；发件人必须从当前认证身份解析。
 *
 * @param requestId 标准UUID；同一成功内容重试返回原消息，修改内容则返回冲突。
 * @param channel 只允许WORLD或PRIVATE，SYSTEM没有玩家写入口。
 * @param peer 私聊精确角色名；世界频道忽略此值，不通过名字授权读取私有资料。
 * @param text 玩家纯文本，服务端按码点限长、控制字符和共用禁词规则校验。
 */
public record ChatSend(String requestId, String channel, String peer, String text) {
}
