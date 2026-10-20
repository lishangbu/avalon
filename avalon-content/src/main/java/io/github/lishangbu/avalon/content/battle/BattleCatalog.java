package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.BattleDefinition;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.content.battle.model.BattleGrowth;
import io.github.lishangbu.avalon.content.battle.model.BattleProduct;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.battle.model.WildProfile;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 开篇战斗内容在启动时加载。技能、成长、遭遇与商店引用统一校验，不支持运行中热改。
 */
@Component
public class BattleCatalog {

	private final BattleDefinition definition;

	private final Map<String, BattleSkill> skills;

	@Autowired
	public BattleCatalog(CreatureCatalog creatures, ItemCatalog items, WorldCatalog world, QuestCatalog quests,
			WildCatalog wilds, SpeciesStatsCatalog species) {
		this(ContentLoader.read("/content/battle/battle.json", BattleDefinition.class), creatures, items, world, quests,
				wilds, species);
	}

	public BattleCatalog(BattleDefinition raw, CreatureCatalog creatures, ItemCatalog items, WorldCatalog world,
			QuestCatalog quests, WildCatalog wilds, SpeciesStatsCatalog species) {
		skills = ContentChecks.index("/content/battle/battle.json", "skills.id", raw.skills(), BattleSkill::id);
		for (var skill : raw.skills()) {
			if (skill.mana() < 0 || skill.cooldown() < 0
					|| !Set.of("ENEMY", "ENEMIES", "ALLY", "OTHER", "SELF").contains(skill.target()))
				throw ContentChecks.invalid("/content/battle/battle.json", skill.id(), "skills", skill, "战斗技能配置无效");
			if (skill.effects()
				.stream()
				.anyMatch(effect -> Set.of("DAMAGE", "SLOW", "POISON", "AREA").contains(effect.kind()))
					&& (!Set.of("PA", "MA").contains(skill.attack()) || !Set.of("PD", "MD").contains(skill.defense())
							|| skill.element() == null))
				throw ContentChecks.invalid("/content/battle/battle.json", skill.id(), "attack/defense/element", skill,
						"技能攻防通道无效");
			// 条件在每项直接攻击执行时生效，必须逐项比较；治疗、护盾等数值不属于攻击威力。
			if (skill.lowHealthPower() != null
					&& (!skill.effects().stream().anyMatch(effect -> Set.of("DAMAGE", "SLOW").contains(effect.kind()))
							|| skill.lowHealthPower().percent() < 1 || skill.lowHealthPower().percent() > 100
							|| skill.effects()
								.stream()
								.anyMatch(effect -> Set.of("DAMAGE", "SLOW", "POISON", "AREA").contains(effect.kind())
										&& skill.lowHealthPower().power() < effect.power())))
				throw ContentChecks.invalid("/content/battle/battle.json", skill.id(), "lowHealthPower",
						skill.lowHealthPower(), "低生命威力条件无效");
		}
		var growth = new LinkedHashMap<String, BattleGrowth>();
		raw.growth().forEach((id, g) -> {
			boolean pet = creatures.contains(id);
			if (!skills.keySet().containsAll(g.skills()) || (!pet && (g.coefficients() == null
					|| g.coefficients().size() != 6 || g.coefficients().stream().anyMatch(n -> n <= 0)
					|| g.manaBase() < 0 || g.manaPerLevel() < 0)))
				throw ContentChecks.invalid("/content/battle/battle.json", id, "growth", g, "战斗成长无效");
			if (pet) {
				// 幻兽只能使用逐形态七维目录，拒绝残留的第二套成长参数。
				if (g.coefficients() != null || g.manaBase() != 0 || g.manaPerLevel() != 0)
					throw ContentChecks.invalid("/content/battle/battle.json", id, "growth", g, "幻兽不得配置职业成长参数");
				species.base(id);
			}
			if (creatures.contains(id) && (g.captureRate() <= 0 || g.captureRate() > 1 || g.minLevel() < 1
					|| g.maxLevel() < g.minLevel() || g.maxLevel() > GrowthRules.LEVEL_CAP || !items.hasItem(g.book())
					|| !items.hasItem(g.fragment())))
				throw ContentChecks.invalid("/content/battle/battle.json", id, "growth", g, "野生物种配置无效");
			growth.put(id, new BattleGrowth(pet ? List.of() : g.coefficients(), g.manaBase(), g.manaPerLevel(),
					g.skills(), g.captureRate(), g.minLevel(), g.maxLevel(), g.book(), g.fragment()));
		});
		var encounters = new ArrayList<BattleEncounter>();
		var encounterIds = new HashSet<String>();
		for (var e : raw.encounters()) {
			ContentChecks.checkId("/content/battle/battle.json", "encounters.id", encounterIds, e.id());
			if ((e.species().isEmpty() && e.boss() == null && e.wild() == null) || e.requires() == null
					|| e.species()
						.entrySet()
						.stream()
						.anyMatch(x -> !creatures.obtainableDirectly(x.getKey()) || !growth.containsKey(x.getKey())
								|| x.getValue() <= 0))
				throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "encounters", e, "遭遇配置无效");
			world.referencedPlace("/content/battle/battle.json", e.id(), "location", e.location());
			// 固定战利品只用于普通胜利且实际击败敌人的遭遇；拒绝无效资产、零数量及教学/剧情首领混用。
			if (!e.victoryDrops().isEmpty() && (e.tutorial() || e.boss() != null
					|| e.victoryDrops()
						.entrySet()
						.stream()
						.anyMatch(drop -> !items.hasItem(drop.getKey()) || drop.getValue() <= 0)))
				throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "victoryDrops", e.victoryDrops(),
						"固定战利品配置无效");
			if (e.wild() != null) {
				WildProfile profile;
				try {
					profile = wilds.profile(e.wild());
				}
				catch (IllegalArgumentException error) {
					throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "wild", e.wild(), "野外模板引用不存在");
				}
				if (e.boss() != null || e.tutorial() || !e.species().isEmpty() || e.count() < 1 || e.count() > 2
						|| (profile.elite() && e.count() != 1)
						|| !growth.keySet().containsAll(profile.species().keySet()))
					throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "wild/count", e, "区域遭遇引用或人数无效");
			}
			if (quests.step(e.requires()) == null)
				throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "requires", e.requires(), "遭遇前置不存在");
			if (e.boss() == null && quests.step(e.captureRequires()) == null)
				throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "captureRequires",
						e.captureRequires(), "捕捉前置不存在");
			if (e.boss() != null && (!"CAVE".equals(e.boss().policy()) || e.boss().health() <= 0
					|| e.boss().defense() <= 0 || e.boss().level() < 1
					|| e.boss().rewardStep() != null && quests.step(e.boss().rewardStep()) == null))
				throw ContentChecks.invalid("/content/battle/battle.json", e.id(), "boss", e.boss(), "首领参数或奖励任务无效");
			encounters.add(e);
		}
		var productIds = new HashSet<String>();
		for (var p : raw.products()) {
			if (!items.hasItem(p.code()) || p.price() <= 0 || quests.step(p.requires()) == null
					|| p.locations().isEmpty())
				throw ContentChecks.invalid("/content/battle/battle.json", p.code(), "products", p, "商店商品无效");
			for (var location : p.locations()) {
				world.referencedPlace("/content/battle/battle.json", p.code(), "locations", location);
				// 同种补给在不同商店可有不同开放任务，但同一商店不能出现歧义报价。
				if (!productIds.add(location + ":" + p.code()) || !world.provides(location, "SUPPLIES"))
					throw ContentChecks.invalid("/content/battle/battle.json", p.code(), "locations", location,
							"补给商品必须关联补给商店且报价唯一");
			}
		}
		validatePool(raw.supplyDrops(), items, false);
		validatePool(raw.bookDrops(), items, true);
		definition = new BattleDefinition(growth, raw.skills(), encounters, raw.products(), raw.elements(),
				raw.supplyDrops(), raw.bookDrops());
	}

	private void validatePool(Map<String, Integer> pool, ItemCatalog items, boolean books) {
		String field = books ? "bookDrops" : "supplyDrops";
		if (pool == null || pool.isEmpty() || pool.values().stream().anyMatch(v -> v <= 0)
				|| pool.keySet()
					.stream()
					.anyMatch(k -> books ? !Set.of("NONE", "BOOK", "FRAGMENT").contains(k)
							: !k.equals("NONE") && !items.hasItem(k)))
			throw ContentChecks.invalid("/content/battle/battle.json", field, field, pool, "掉落权重或物品引用无效");
		int total = 0;
		for (var entry : pool.entrySet()) {
			if (entry.getValue() > Integer.MAX_VALUE - total)
				throw ContentChecks.invalid("/content/battle/battle.json", entry.getKey(), field + "." + entry.getKey(),
						entry.getValue(), "掉落总权重超出整数范围");
			total += entry.getValue();
		}
	}

	public BattleDefinition definition() {
		return definition;
	}

	public BattleSkill skill(String id) {
		return skills.get(id);
	}

	public BattleGrowth growth(String id) {
		return ContentChecks.required("/content/battle/battle.json", "growth", id, definition.growth().get(id));
	}

	public BattleEncounter encounter(String id) {
		return definition.encounters().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
	}

	public double element(String attack, String target) {
		return definition.elements().getOrDefault(attack, Map.of()).getOrDefault(target, 1.0);
	}

	/** 商品资格供购买命令和展示共用；任务进度不能把商品开放到未配置的商店。 */
	public List<BattleProduct> productsAt(String location, Set<String> facts) {
		return definition.products()
			.stream()
			.filter(product -> product.locations().contains(location) && facts.contains("QUEST:" + product.requires()))
			.toList();
	}

}
