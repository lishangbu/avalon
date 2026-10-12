package io.github.lishangbu.avalon.content.creature.model;

import java.util.Map;

/**
 * EvolutionDropCatalog 使用的类型化配置，声明species等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param species 幻兽物种代码
 */
public record EvolutionDropDefinition(Map<String, EvolutionDropDrop> species) {
}
