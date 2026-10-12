package io.github.lishangbu.avalon.inventory.domain;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * EnhancementRules 使用的类型化规则数据，包含current、target、failureLevel、successPercent等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param current 当前状态
 * @param target 目标编号或目标声明
 * @param stone 强化石引用
 * @param quantity 物品或资源数量
 * @param gold 金币数量
 */
public record EnhancementRulesQuote(int current, int target, int failureLevel, int successPercent, String stone,
		int quantity, @JsonSerialize(using = ToStringSerializer.class) long gold) {
}
