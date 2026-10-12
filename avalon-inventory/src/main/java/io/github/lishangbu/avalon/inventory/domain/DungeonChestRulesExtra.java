package io.github.lishangbu.avalon.inventory.domain;

/**
 * DungeonChestRules 使用的类型化规则数据，包含code、amount、bound等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param code 内容目录中的稳定代码
 * @param amount 本次规则使用的数量
 * @param bound 资产是否绑定
 */
public record DungeonChestRulesExtra(String code, int amount, boolean bound) {
}
