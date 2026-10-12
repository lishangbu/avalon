package io.github.lishangbu.avalon.inventory.domain;

/**
 * BagCapacityRules 使用的类型化规则数据，包含code、bound、quantity、equipment等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param code 内容目录中的稳定代码
 * @param bound 资产是否绑定
 * @param quantity 物品或资源数量
 * @param equipment 装备声明或装备标志
 * @param task 是否属于任务物品
 */
public record BagCapacityRulesEntry(String code, boolean bound, int quantity, boolean equipment, boolean task) {
}
