package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;

/**
 * CreatureCatalog 使用的类型化配置，声明id、name、element、level等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param element 元素属性
 * @param level 等级
 * @param abilitySlot 特性槽位
 * @param ability 特性代码
 * @param skills 技能声明列表
 * @param image 资源图片引用
 * @param description 玩家可读说明
 */
public record CreaturePet(String id, String name, String element, int level, String abilitySlot, String ability,
		List<String> skills, String image, String description) {
	public CreaturePet(String id, String name, String element, int level, String abilitySlot, String ability,
			List<String> skills, String image) {
		this(id, name, element, level, abilitySlot, ability, skills, image, null);
	}

	public CreaturePet {
		skills = skills == null ? List.of() : List.copyOf(skills);
	}
}
