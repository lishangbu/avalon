package io.github.lishangbu.avalon.inventory.application.model;

import java.util.Map;

/**
 * 背包单项读模型，id 为实例编号，code 为内容编号，两者不能互换。 slot 为 null 的任务物品没有穿戴操作；stats 为解码后的实例属性，quantity
 * 是当前正库存。 equipped、bound 与 quality 仅展示服务器状态，客户端不得通过修改响应对象改变物品。
 */
public record InventoryItemView(String id, String name, String code, int quantity, String slot, int requiredLevel,
		Map<String, Integer> stats, boolean equipped, boolean bound, String quality, int sellPrice,
		int replacementPrice, String source, String requiredProfession, int enhancementLevel) {
}
