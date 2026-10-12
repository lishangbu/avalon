package io.github.lishangbu.avalon.chat.application.model;

/** 已读确认仅在消息实际展示后发送；through 必须属于当前频道与参与者，不能拿其他私聊的编号推进。 */
public record ChatRead(String channel, String peer, String through) {
}
