package io.github.lishangbu.avalon.content.battle.model;

import java.util.Map;

/**
 * WildCatalog 使用的类型化配置，声明profiles等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 */
public record WildDefinition(Map<String, WildProfile> profiles) {
}
