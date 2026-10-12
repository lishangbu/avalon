package io.github.lishangbu.avalon.chat.application.model;

/** 拉黑意图仅携带对端精确公开角色名；黑名单归属始终由服务端身份决定。 */
public record ChatBlock(String name) {
}
