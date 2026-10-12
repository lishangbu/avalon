package io.github.lishangbu.avalon.chat.application.model;

import java.util.List;

/** 历史按新到旧返回；next 是继续向更早消息翻页的排他游标，空值表示本页已到末尾。 */
public record ChatPage(List<ChatMessage> entries, String next) {
}
