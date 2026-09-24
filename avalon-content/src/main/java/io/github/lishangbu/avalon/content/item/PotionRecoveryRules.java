package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 战斗和生活补给共用药品恢复量，不让同一药品因玩法而改变效果。 */
@Component
public final class PotionRecoveryRules {

	private final Map<String, ItemCatalog.Item> potions;

	private final List<String> codes;

	/** 使用已完成校验的物品定义，不独立读取资源或共享全局药效状态。 */
	public PotionRecoveryRules(ItemCatalog items) {
		potions = ContentChecks.index("/content/items.json", "items.id",
				items.allItems().stream().filter(item -> item.type() == ItemCatalog.ConsumableType.POTION).toList(),
				ItemCatalog.Item::id);
		codes = items.consumables(ItemCatalog.ConsumableType.POTION, ItemCatalog.Scope.GENERAL)
			.stream()
			.map(ItemCatalog.Item::id)
			.toList();
	}

	public List<String> usablePotions() {
		return codes;
	}

	public Recovery recovery(String code) {
		return recovery(code, false);
	}

	public Recovery recovery(String code, boolean academy) {
		var potion = potions.get(code);
		if (potion == null || potion.scope() != (academy ? ItemCatalog.Scope.ACADEMY : ItemCatalog.Scope.GENERAL))
			return null;
		return new Recovery(potion.recovery().health(), potion.recovery().mana());
	}

	public List<String> academyPotions() {
		return potions.values()
			.stream()
			.filter(i -> i.scope() == ItemCatalog.Scope.ACADEMY)
			.map(ItemCatalog.Item::id)
			.toList();
	}

	public record Recovery(int health, int mana) {
	}

}
