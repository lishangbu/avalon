package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;

/**
 * CreatureCatalog 使用的类型化配置，声明pets等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param pets 幻兽声明列表
 */
public record CreatureDefinition(List<CreaturePet> pets) {
	public CreatureDefinition {
		pets = List.copyOf(pets);
	}
}
