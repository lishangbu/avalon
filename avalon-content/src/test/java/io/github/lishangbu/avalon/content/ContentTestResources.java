package io.github.lishangbu.avalon.content;

/** Test fixtures resolve real domain resources, never retain old resource aliases. */
public final class ContentTestResources {

	private ContentTestResources() {
	}

	public static String path(String name) {
		return "/content/" + switch (name) {
			case "opening" -> "world/opening.json";
			case "scene-actions" -> "world/scene-actions.json";
			case "creatures" -> "creatures/creatures.json";
			case "species-stats" -> "creatures/species-stats.json";
			case "ability-slots" -> "creatures/ability-slots.json";
			case "evolution" -> "creatures/evolution.json";
			case "evolution-drops" -> "creatures/evolution-drops.json";
			case "evolution-research" -> "creatures/evolution-research.json";
			case "learning" -> "creatures/learning.json";
			case "collection" -> "creatures/collection.json";
			case "creature-naming" -> "creatures/creature-naming.json";
			case "items" -> "items/items.json";
			case "equipment" -> "items/equipment.json";
			case "equipment-sets" -> "items/equipment-sets.json";
			case "enhancement" -> "items/enhancement.json";
			case "crafting" -> "items/crafting.json";
			case "book-recycling" -> "items/book-recycling.json";
			case "gathering" -> "professions/gathering.json";
			case "life-production" -> "professions/life-production.json";
			case "farming" -> "professions/farming.json";
			case "fishing" -> "professions/fishing.json";
			case "experience" -> "rules/experience.json";
			case "market" -> "rules/market.json";
			case "forbidden-words" -> "rules/forbidden-words.json";
			case "battle" -> "battle/battle.json";
			case "wilds" -> "battle/wilds.json";
			case "training" -> "battle/training.json";
			case "academy-training" -> "battle/academy-training.json";
			case "cultivation-rewards" -> "battle/cultivation-rewards.json";
			default -> throw new IllegalArgumentException("Unknown test content: " + name);
		};
	}

}
