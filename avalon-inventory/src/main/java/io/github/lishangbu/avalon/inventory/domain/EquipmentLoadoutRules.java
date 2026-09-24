package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 汇总已穿戴实例与套装，供战斗、角色面板、换装前后资源上限使用同一套计算。 */
@Component
@RequiredArgsConstructor
public class EquipmentLoadoutRules {

	private final EquipmentSetCatalog catalog;

	public Loadout from(List<Piece> items, int level) {
		var worn = items.stream().filter(item -> item.equipped() && item.quantity() > 0).toList();
		Map<String, Integer> stats = new TreeMap<>();
		worn.forEach(item -> item.stats().forEach((key, value) -> stats.merge(key, value, Math::addExact)));
		var codes = worn.stream().map(Piece::code).distinct().toList();
		var sets = catalog.sets().stream().map(set -> {
			int count = (int) codes.stream().filter(set.pieces()::contains).count();
			boolean complete = count == set.pieces().size() && codes.stream().anyMatch(set.weapons()::contains);
			return new SetView(set.id(), set.name(), count, set.pieces().size(), set.healthPieces(), set.health(),
					set.shieldPieces(), Math.multiplyExact(Math.min(level, set.shieldLevelCap()), set.shieldPerLevel()),
					!set.weapons().isEmpty(), complete, set.weaponHealth(), set.weaponShield());
		}).filter(set -> set.count() > 0).toList();
		sets.stream()
			.filter(set -> set.count() >= set.healthPieces())
			.forEach(set -> stats.merge("health", set.health(), Math::addExact));
		sets.stream()
			.filter(SetView::weaponActive)
			.forEach(set -> stats.merge("health", set.weaponHealth(), Math::addExact));
		// 同名防御护盾不叠加；若以后有其他套装，当前机制取最高容量，不偷偷相加。
		int shield = sets.stream()
			.filter(set -> set.count() >= set.shieldPieces())
			.mapToInt(set -> Math.addExact(set.shield(), set.weaponActive() ? set.weaponShield() : 0))
			.max()
			.orElse(0);
		return new Loadout(stats, shield, sets);
	}

	/** 领域输入仅包含计算所需值，不依赖库存接口投影。 */
	public record Piece(String code, boolean equipped, int quantity, Map<String, Integer> stats) {
	}

	public record Loadout(Map<String, Integer> stats, int defendShield, List<SetView> sets) {
		public Loadout {
			stats = Map.copyOf(stats);
			sets = List.copyOf(sets);
		}
	}

	public record SetView(String id, String name, int count, int total, int healthPieces, int health, int shieldPieces,
			int shield, boolean hasWeaponBonus, boolean weaponActive, int weaponHealth, int weaponShield) {
	}

}
