package io.github.lishangbu.avalon.inventory.application.model;

/** 待领数量独立于可用库存；来源用于玩家辨认奖励，不作为客户端重新发奖参数。 */
public record PendingItemView(String id, String name, int quantity, boolean bound, String source, String code) {
}
