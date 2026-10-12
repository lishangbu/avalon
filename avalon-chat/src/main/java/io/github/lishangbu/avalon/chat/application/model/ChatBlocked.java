package io.github.lishangbu.avalon.chat.application.model;

/** 拒收陌生人仅影响后续私聊；黑名单有方向，不影响对方账号的其他游戏能力。 */
public record ChatBlocked(String character, String name) {
}
