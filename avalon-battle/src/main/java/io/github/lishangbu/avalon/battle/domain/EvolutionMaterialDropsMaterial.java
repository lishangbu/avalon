package io.github.lishangbu.avalon.battle.domain;

/**
 * EvolutionMaterialDrops 使用的类型化规则数据，包含code、quantity等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param code 内容目录中的稳定代码
 * @param quantity 物品或资源数量
 */
public record EvolutionMaterialDropsMaterial(String code, int quantity) {
}
