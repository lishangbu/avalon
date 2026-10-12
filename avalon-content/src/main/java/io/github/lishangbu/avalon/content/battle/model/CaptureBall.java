package io.github.lishangbu.avalon.content.battle.model;

/**
 * CaptureBallCatalog 使用的类型化配置，声明code、level、multiplier、reinforced等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param code 内容目录中的稳定代码
 * @param level 等级
 * @param multiplier 数值倍率
 */
public record CaptureBall(String code, int level, double multiplier, boolean reinforced) {
}
