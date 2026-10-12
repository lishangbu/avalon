package io.github.lishangbu.avalon.content.item.model;

/** ItemCatalog 的独立公开配置模型；保留原字段及约束，调用方直接引用此类型。 */
public enum ItemCategory {

	MATERIAL("材料"), CONSUMABLE("消耗品"), TOOL("工具"), SKILL_BOOK("技能书"), EQUIPMENT("装备"), TOKEN("凭证");

	public final String title;

	ItemCategory(String title) {
		this.title = title;
	}

}
