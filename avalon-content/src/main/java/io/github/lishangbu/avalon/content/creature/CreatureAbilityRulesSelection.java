package io.github.lishangbu.avalon.content.creature;

/**
 * CreatureAbilityRules 使用的类型化配置，声明slot、ability等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param slot 资产槽位
 * @param ability 特性代码
 */
public record CreatureAbilityRulesSelection(String slot, String ability) {
}
