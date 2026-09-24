package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;

import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;

/** 套装只引用明确的装备编号，不靠名称前缀识别；数值与触发件数在启动时校验。 */
@Component
public class EquipmentSetCatalog {

	private final List<SetDefinition> sets;

	public EquipmentSetCatalog(ItemCatalog items) {
		sets = ContentLoader.read("/content/equipment-sets.json", Definition.class).sets();
		var ids = new HashSet<String>();
		var pieces = new HashSet<String>();
		for (var set : sets) {
			ContentChecks.checkId("/content/equipment-sets.json", "sets.id", ids, set.id());
			if (set.name() == null || set.name().isBlank() || set.healthPieces() < 1
					|| set.shieldPieces() < set.healthPieces() || set.shieldPieces() > set.pieces().size()
					|| set.health() < 1 || set.shieldPerLevel() < 1 || set.shieldLevelCap() < 1
					|| set.shieldLevelCap() > GrowthRules.LEVEL_CAP)
				throw new IllegalArgumentException("套装效果配置无效：" + set.id());
			var slots = new HashSet<String>();
			if (set.weaponHealth() < 0 || set.weaponShield() < 0
					|| set.weapons().stream().distinct().count() != set.weapons().size())
				throw new IllegalArgumentException("配套武器效果无效：" + set.id());
			for (String weapon : set.weapons())
				if (!items.hasEquipment(weapon) || !items.equipment(weapon).slot().equals("WEAPON")
						|| !pieces.add(weapon))
					throw new IllegalArgumentException("配套武器引用无效：" + weapon);
			for (var code : set.pieces())
				if (!items.hasEquipment(code) || !pieces.add(code) || !slots.add(items.equipment(code).slot()))
					throw new IllegalArgumentException("套装装备引用、归属或部位重复：" + code);
		}
	}

	public List<SetDefinition> sets() {
		return sets;
	}

	public record Definition(List<SetDefinition> sets) {
		public Definition {
			sets = List.copyOf(sets);
		}
	}

	public record SetDefinition(String id, String name, List<String> pieces, int healthPieces, int health,
			int shieldPieces, int shieldPerLevel, int shieldLevelCap, List<String> weapons, int weaponHealth,
			int weaponShield) {
		public SetDefinition {
			pieces = List.copyOf(pieces);
			weapons = weapons == null ? List.of() : List.copyOf(weapons);
		}
	}

}
