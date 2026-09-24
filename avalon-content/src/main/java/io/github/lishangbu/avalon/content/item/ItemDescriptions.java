package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import org.springframework.stereotype.Component;

/** 物品展示说明的统一入口；原始定义不混入派生文案。 */
@Component
public final class ItemDescriptions {

	private final ItemCatalog items;

	public ItemDescriptions(ItemCatalog items) {
		this.items = items;
	}

	public String description(String code) {
		var item = items.item(code);
		String text = item.description();
		if (item.recovery() != null)
			text += recovery(item.recovery().health(), item.recovery().mana());
		var tool = item.tool();
		if (tool != null)
			text += "覆盖生活技能" + LifeSkillRules.firstToolLevel(tool.tier()) + "至"
					+ LifeSkillRules.lastToolLevel(tool.tier()) + "级。";
		return text;
	}

	private static String recovery(int health, int mana) {
		return "恢复" + (health > 0 ? health + "生命" : "") + (health > 0 && mana > 0 ? "、" : "")
				+ (mana > 0 ? mana + "魔力" : "") + "。";
	}

}
