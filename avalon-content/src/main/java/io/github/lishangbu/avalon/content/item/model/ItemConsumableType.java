package io.github.lishangbu.avalon.content.item.model;

/** ItemCatalog 的独立公开配置模型；保留原字段及约束，调用方直接引用此类型。 */
public enum ItemConsumableType {

	POTION("药品"), FOOD("料理"), CAPTURE_BALL("封印球"), SEED("种子"), BAIT("鱼饵"), ENHANCEMENT_STONE("强化石");

	public final String title;

	ItemConsumableType(String title) {
		this.title = title;
	}

}
