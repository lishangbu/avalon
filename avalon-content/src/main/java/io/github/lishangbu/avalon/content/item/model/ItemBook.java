package io.github.lishangbu.avalon.content.item.model;

/**
 * ItemCatalog 使用的类型化配置，声明skill、level等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param skill 技能代码
 * @param level 等级
 */
public record ItemBook(String skill, int level) {
}
