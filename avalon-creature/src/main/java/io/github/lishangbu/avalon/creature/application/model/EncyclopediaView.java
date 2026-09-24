package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/** 永久图鉴只揭示实际获得过的特性，放生不抹掉记录；持有数量不计已放生个体。 */
public record EncyclopediaView(String species, String name, String element, boolean collected, long owned,
		List<Ability> abilities, boolean rewarded, String image, String description) {
	public record Ability(String slot, String name) {
	}
}
