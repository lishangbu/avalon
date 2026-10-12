package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 汇总已穿戴实例与套装，供战斗、角色面板、换装前后资源上限使用同一套计算。 */
@Component
@RequiredArgsConstructor
public class EquipmentLoadoutRules {

	private final EquipmentSetCatalog catalog;

	public EquipmentLoadoutRulesLoadout from(List<EquipmentLoadoutRulesPiece> items, int level) {
		var worn = items.stream().filter(item -> item.equipped() && item.quantity() > 0).toList();
		Map<String, Integer> stats = new TreeMap<>();
		worn.forEach(item -> item.stats().forEach((key, value) -> stats.merge(key, value, Math::addExact)));
		var codes = worn.stream().map(EquipmentLoadoutRulesPiece::code).distinct().toList();
		var sets = catalog.sets().stream().map(set -> {
			int count = (int) codes.stream().filter(set.pieces()::contains).count();
			boolean complete = count == set.pieces().size() && codes.stream().anyMatch(set.weapons()::contains);
			return new EquipmentLoadoutRulesSetView(set.id(), set.name(), count, set.pieces().size(),
					set.healthPieces(), set.health(), set.shieldPieces(),
					Math.multiplyExact(Math.min(level, set.shieldLevelCap()), set.shieldPerLevel()),
					!set.weapons().isEmpty(), complete, set.weaponHealth(), set.weaponShield());
		}).filter(set -> set.count() > 0).toList();
		sets.stream()
			.filter(set -> set.count() >= set.healthPieces())
			.forEach(set -> stats.merge("health", set.health(), Math::addExact));
		sets.stream()
			.filter(EquipmentLoadoutRulesSetView::weaponActive)
			.forEach(set -> stats.merge("health", set.weaponHealth(), Math::addExact));
		// 同名防御护盾不叠加；若以后有其他套装，当前机制取最高容量，不偷偷相加。
		int shield = sets.stream()
			.filter(set -> set.count() >= set.shieldPieces())
			.mapToInt(set -> Math.addExact(set.shield(), set.weaponActive() ? set.weaponShield() : 0))
			.max()
			.orElse(0);
		return new EquipmentLoadoutRulesLoadout(stats, shield, sets);
	}

}
