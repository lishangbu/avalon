package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 战斗和生活补给共用药品恢复量，不让同一药品因玩法而改变效果。 */
@Component
public final class PotionRecoveryRules {

	private final Map<String, ItemItem> potions;

	private final List<String> codes;

	/** 使用已完成校验的物品定义，不独立读取资源或共享全局药效状态。 */
	public PotionRecoveryRules(ItemCatalog items) {
		potions = ContentChecks.index("/content/items/items.json", "items.id",
				items.allItems().stream().filter(item -> item.type() == ItemConsumableType.POTION).toList(),
				ItemItem::id);
		codes = items.consumables(ItemConsumableType.POTION, ItemScope.GENERAL).stream().map(ItemItem::id).toList();
	}

	public List<String> usablePotions() {
		return codes;
	}

	public PotionRecoveryRulesRecovery recovery(String code) {
		return recovery(code, false);
	}

	public PotionRecoveryRulesRecovery recovery(String code, boolean academy) {
		var potion = potions.get(code);
		if (potion == null || potion.scope() != (academy ? ItemScope.ACADEMY : ItemScope.GENERAL))
			return null;
		return new PotionRecoveryRulesRecovery(potion.recovery().health(), potion.recovery().mana());
	}

	public List<String> academyPotions() {
		return potions.values().stream().filter(i -> i.scope() == ItemScope.ACADEMY).map(ItemItem::id).toList();
	}

}
