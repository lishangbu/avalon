package io.github.lishangbu.avalon.chat.application.model;

/** 仅返回双方公开角色资料与未读数量；最后消息全文仍通过历史接口鉴权读取。 */
public record ChatConversation(String character, String name, long unread, String latest) {
}
