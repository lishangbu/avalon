package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.creature.EvolutionDropCatalog.Drop;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** 两池独立抽取；数量只由已生成敌人的等级决定，与角色等级和研究资格无关。 */
public final class EvolutionMaterialDrops {

	private EvolutionMaterialDrops() {
	}

	public record Material(String code, int quantity) {
	}

	public static List<Material> roll(Drop drop, int level, RandomGenerator random) {
		if (drop == null)
			return List.of();
		int quantity = Math.min(3, 1 + Math.max(0, level - drop.sourceLevel()) / 10);
		var result = new ArrayList<Material>();
		if (random.nextInt(100) < drop.commonPercent())
			result.add(new Material(drop.common(), quantity));
		if (random.nextInt(100) < drop.familyPercent())
			result.add(new Material(drop.family(), quantity));
		return List.copyOf(result);
	}

}
