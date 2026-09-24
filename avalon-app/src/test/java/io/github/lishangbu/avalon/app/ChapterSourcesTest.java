package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.creature.CollectionCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.CraftingCatalog;
import io.github.lishangbu.avalon.content.item.EquipmentCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核对章节内容的可重复来源，而非只检查模板存在。尤其检查旧村关闭后， 从诺姆城沿真实道路仍可到达的区域；一次性赠品不能替代遗忘或销毁后的补充来源。
 * 本测试只证明配置连通与来源覆盖，不代替真实升级、战斗胜率和经济验收。
 */
class ChapterSourcesTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final CreatureCatalog creatures = new CreatureCatalog();

	private final CollectionCatalog collection = new CollectionCatalog(items, creatures);

	private final QuestCatalog quests = new QuestCatalog(world, items, creatures);

	private final WildCatalog wilds = new WildCatalog(creatures, items);

	private final BattleCatalog battles = new BattleCatalog(creatures, items, world, quests, wilds,
			new SpeciesStatsCatalog());

	private final LearningCatalog learning = new LearningCatalog(items, creatures, battles);

	private final CraftingCatalog crafting = new CraftingCatalog(items, learning);

	private final EquipmentCatalog equipment = new EquipmentCatalog(items, world, quests);

	private final DungeonCatalog dungeon = new DungeonCatalog(creatures, quests, items, world);

	private final DungeonChestCatalog chests = new DungeonChestCatalog(items, dungeon);

	/** 按章节终点已完成任务的事实检查道路，关闭的建筑不能充当穿越节点。 */
	private Set<String> finalPlaces() {
		var facts = quests.definition().steps().stream().map(step -> "QUEST:" + step.id()).collect(Collectors.toSet());
		quests.definition().steps().forEach(step -> facts.addAll(step.facts()));
		var reached = new HashSet<String>();
		var pending = new ArrayDeque<String>();
		pending.add("CITY_OUTSKIRTS");
		while (!pending.isEmpty()) {
			var id = pending.remove();
			var place = world.place(id);
			if (world.accessible(place, facts::contains) && reached.add(id))
				pending.addAll(place.exits());
		}
		return reached;
	}

	/** 仅统计普通可捕获遭遇，精英不作为八物种收集的必要条件。 */
	@Test
	void everySpeciesRemainsCatchableAfterLeavingTheVillage() {
		var places = finalPlaces();
		var available = new HashSet<String>();
		for (var encounter : battles.definition().encounters()) {
			if (!places.contains(encounter.location()) || encounter.boss() != null || encounter.tutorial())
				continue;
			if (encounter.wild() == null)
				available.addAll(encounter.species().keySet());
			else if (!wilds.profile(encounter.wild()).elite())
				available.addAll(wilds.profile(encounter.wild()).species().keySet());
		}
		// 章节验收使用明确的原八物种清单；第二阶段进化形态没有野外来源，不能要求它们可捕捉。
		assertEquals(8, collection.definition().milestoneSpecies().size());
		var missing = creatures.all()
			.stream()
			.map(CreatureCatalog.Pet::id)
			.filter(creatures::obtainableDirectly)
			.filter(id -> !available.contains(id))
			.toList();
		assertTrue(missing.isEmpty(), "章节结束后无法捕获的物种：" + missing);
	}

	/** 真实权重池与物种掉落引用一起展开，未挂到遭遇入口的野外模板不算来源。 */
	private Set<String> renewableItems() {
		var places = finalPlaces();
		var result = new HashSet<String>();
		battles.definition()
			.products()
			.stream()
			.filter(product -> product.locations().stream().anyMatch(places::contains))
			.forEach(product -> result.add(product.code()));
		for (var encounter : battles.definition().encounters()) {
			if (!places.contains(encounter.location()) || encounter.boss() != null || encounter.tutorial())
				continue;
			var profile = encounter.wild() == null ? null : wilds.profile(encounter.wild());
			if (profile != null && profile.elite()) {
				profile.bookDrops().forEach((code, weight) -> {
					if (weight > 0)
						result.add(code);
				});
			}
			else {
				var species = profile == null ? encounter.species().keySet() : profile.species().keySet();
				for (var id : species) {
					if (battles.definition().bookDrops().getOrDefault("BOOK", 0) > 0)
						result.add(battles.growth(id).book());
					if (battles.definition().bookDrops().getOrDefault("FRAGMENT", 0) > 0)
						result.add(battles.growth(id).fragment());
				}
			}
		}
		return result;
	}

	@Test
	void everyLearnableBookAndRecipeHasARepeatableSource() {
		var sources = renewableItems();
		for (var pet : creatures.all().stream().filter(pet -> creatures.obtainableDirectly(pet.id())).toList()) {
			assertFalse(learning.pool(pet.id()).isEmpty(), "缺少物种技能池：" + pet.id());
			for (var book : learning.pool(pet.id()))
				assertTrue(sources.contains(book.code()), "技能书不可再次获取：" + book.code());
		}
		for (var recipe : crafting.recipes())
			assertTrue(sources.contains(recipe.fragment()), "配方碎片无来源：" + recipe.id());
	}

	/** 销毁后可补购、重新制造或从实际发起者宝箱池获得；制造设施必须仍可到达。 */
	@Test
	void everyEquipmentTemplateHasAReplacementSource() {
		var places = finalPlaces();
		var sources = equipment.definition()
			.offers()
			.stream()
			.filter(offer -> offer.locations().stream().anyMatch(places::contains))
			.map(EquipmentCatalog.Offer::code)
			.collect(Collectors.toSet());
		chests.definition()
			.equipment()
			.stream()
			.filter(entry -> entry.weight() > 0)
			.forEach(entry -> sources.add(entry.code()));
		new LifeProductionCatalog(items, world).recipes()
			.stream()
			.filter(recipe -> recipe.locations().stream().anyMatch(places::contains))
			.forEach(recipe -> sources.add(recipe.product()));
		var missing = new HashSet<>(items.equipmentCodes());
		missing.removeAll(sources);
		assertTrue(missing.isEmpty(), "装备没有重复来源：" + missing);
	}

}
