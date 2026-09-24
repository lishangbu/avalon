package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.ContentChecks;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 开篇战斗内容在启动时加载。技能、成长、遭遇与商店引用统一校验，不支持运行中热改。
 */
@Component
public class BattleCatalog {

	private final Definition definition;

	private final Map<String, Skill> skills;

	@Autowired
	public BattleCatalog(CreatureCatalog creatures, ItemCatalog items, WorldCatalog world, QuestCatalog quests,
			WildCatalog wilds, SpeciesStatsCatalog species) {
		this(ContentLoader.read("/content/battle.json", Definition.class), creatures, items, world, quests, wilds,
				species);
	}

	public BattleCatalog(Definition raw, CreatureCatalog creatures, ItemCatalog items, WorldCatalog world,
			QuestCatalog quests, WildCatalog wilds, SpeciesStatsCatalog species) {
		skills = ContentChecks.index("/content/battle.json", "skills.id", raw.skills(), Skill::id);
		for (var skill : raw.skills()) {
			if (skill.mana() < 0 || skill.cooldown() < 0 || skill.power() < 0
					|| !Set
						.of("DAMAGE", "HEAL", "COVER", "SHIELD", "SLOW", "POISON", "MAGIC_GUARD", "WALL", "AREA",
								"RENEW", "MARK")
						.contains(skill.effect())
					|| !Set.of("ENEMY", "ENEMIES", "ALLY", "OTHER", "SELF").contains(skill.target()))
				throw ContentChecks.invalid("/content/battle.json", skill.id(), "skills", skill, "战斗技能配置无效");
			if (Set.of("DAMAGE", "SLOW", "POISON", "AREA").contains(skill.effect())
					&& (!Set.of("PA", "MA").contains(skill.attack()) || !Set.of("PD", "MD").contains(skill.defense())
							|| skill.element() == null))
				throw ContentChecks.invalid("/content/battle.json", skill.id(), "attack/defense/element", skill,
						"技能攻防通道无效");
			if (skill.lowHealthPower() != null && (!Set.of("DAMAGE", "SLOW").contains(skill.effect())
					|| skill.lowHealthPower().percent() < 1 || skill.lowHealthPower().percent() > 100
					|| skill.lowHealthPower().power() < skill.power()))
				throw ContentChecks.invalid("/content/battle.json", skill.id(), "lowHealthPower",
						skill.lowHealthPower(), "低生命威力条件无效");
		}
		var growth = new LinkedHashMap<String, Growth>();
		raw.growth().forEach((id, g) -> {
			boolean pet = creatures.contains(id);
			if (!skills.keySet().containsAll(g.skills()) || (!pet && (g.coefficients() == null
					|| g.coefficients().size() != 6 || g.coefficients().stream().anyMatch(n -> n <= 0)
					|| g.manaBase() < 0 || g.manaPerLevel() < 0)))
				throw ContentChecks.invalid("/content/battle.json", id, "growth", g, "战斗成长无效");
			if (pet) {
				// 幻兽只能使用逐形态七维目录，拒绝残留的第二套成长参数。
				if (g.coefficients() != null || g.manaBase() != 0 || g.manaPerLevel() != 0)
					throw ContentChecks.invalid("/content/battle.json", id, "growth", g, "幻兽不得配置职业成长参数");
				species.base(id);
			}
			if (creatures.contains(id) && (g.captureRate() <= 0 || g.captureRate() > 1 || g.minLevel() < 1
					|| g.maxLevel() < g.minLevel() || g.maxLevel() > GrowthRules.LEVEL_CAP || !items.hasItem(g.book())
					|| !items.hasItem(g.fragment())))
				throw ContentChecks.invalid("/content/battle.json", id, "growth", g, "野生物种配置无效");
			growth.put(id, new Growth(pet ? List.of() : g.coefficients(), g.manaBase(), g.manaPerLevel(), g.skills(),
					g.captureRate(), g.minLevel(), g.maxLevel(), g.book(), g.fragment()));
		});
		var encounters = new ArrayList<Encounter>();
		var encounterIds = new HashSet<String>();
		for (var e : raw.encounters()) {
			ContentChecks.checkId("/content/battle.json", "encounters.id", encounterIds, e.id());
			if ((e.species().isEmpty() && e.boss() == null && e.wild() == null) || e.requires() == null
					|| e.species()
						.entrySet()
						.stream()
						.anyMatch(x -> !creatures.obtainableDirectly(x.getKey()) || !growth.containsKey(x.getKey())
								|| x.getValue() <= 0))
				throw ContentChecks.invalid("/content/battle.json", e.id(), "encounters", e, "遭遇配置无效");
			world.referencedPlace("/content/battle.json", e.id(), "location", e.location());
			if (e.wild() != null) {
				WildCatalog.Profile profile;
				try {
					profile = wilds.profile(e.wild());
				}
				catch (IllegalArgumentException error) {
					throw ContentChecks.invalid("/content/battle.json", e.id(), "wild", e.wild(), "野外模板引用不存在");
				}
				if (e.boss() != null || e.tutorial() || !e.species().isEmpty() || e.count() < 1 || e.count() > 2
						|| (profile.elite() && e.count() != 1)
						|| !growth.keySet().containsAll(profile.species().keySet()))
					throw ContentChecks.invalid("/content/battle.json", e.id(), "wild/count", e, "区域遭遇引用或人数无效");
			}
			if (quests.step(e.requires()) == null)
				throw ContentChecks.invalid("/content/battle.json", e.id(), "requires", e.requires(), "遭遇前置不存在");
			if (e.boss() == null && quests.step(e.captureRequires()) == null)
				throw ContentChecks.invalid("/content/battle.json", e.id(), "captureRequires", e.captureRequires(),
						"捕捉前置不存在");
			if (e.boss() != null && (!"CAVE".equals(e.boss().policy()) || e.boss().health() <= 0
					|| e.boss().defense() <= 0 || e.boss().level() < 1 || quests.step(e.boss().rewardStep()) == null))
				throw ContentChecks.invalid("/content/battle.json", e.id(), "boss", e.boss(), "首领参数或奖励任务无效");
			encounters.add(e);
		}
		var productIds = new HashSet<String>();
		for (var p : raw.products()) {
			if (!items.hasItem(p.code()) || p.price() <= 0 || quests.step(p.requires()) == null
					|| p.locations().isEmpty())
				throw ContentChecks.invalid("/content/battle.json", p.code(), "products", p, "商店商品无效");
			for (var location : p.locations()) {
				world.referencedPlace("/content/battle.json", p.code(), "locations", location);
				// 同种补给在不同商店可有不同开放任务，但同一商店不能出现歧义报价。
				if (!productIds.add(location + ":" + p.code()) || !world.provides(location, "SUPPLIES"))
					throw ContentChecks.invalid("/content/battle.json", p.code(), "locations", location,
							"补给商品必须关联补给商店且报价唯一");
			}
		}
		validatePool(raw.supplyDrops(), items, false);
		validatePool(raw.bookDrops(), items, true);
		definition = new Definition(growth, raw.skills(), encounters, raw.products(), raw.elements(), raw.supplyDrops(),
				raw.bookDrops());
	}

	private void validatePool(Map<String, Integer> pool, ItemCatalog items, boolean books) {
		String field = books ? "bookDrops" : "supplyDrops";
		if (pool == null || pool.isEmpty() || pool.values().stream().anyMatch(v -> v <= 0)
				|| pool.keySet()
					.stream()
					.anyMatch(k -> books ? !Set.of("NONE", "BOOK", "FRAGMENT").contains(k)
							: !k.equals("NONE") && !items.hasItem(k)))
			throw ContentChecks.invalid("/content/battle.json", field, field, pool, "掉落权重或物品引用无效");
		int total = 0;
		for (var entry : pool.entrySet()) {
			if (entry.getValue() > Integer.MAX_VALUE - total)
				throw ContentChecks.invalid("/content/battle.json", entry.getKey(), field + "." + entry.getKey(),
						entry.getValue(), "掉落总权重超出整数范围");
			total += entry.getValue();
		}
	}

	public Definition definition() {
		return definition;
	}

	public Skill skill(String id) {
		return skills.get(id);
	}

	public Growth growth(String id) {
		return ContentChecks.required("/content/battle.json", "growth", id, definition.growth().get(id));
	}

	public Encounter encounter(String id) {
		return definition.encounters().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
	}

	public double element(String attack, String target) {
		return definition.elements().getOrDefault(attack, Map.of()).getOrDefault(target, 1.0);
	}

	/**
	 * effect 决定结算器，attack/defense 决定属性通道，target 决定目标阵营。
	 */
	public record Skill(String id, String effect, String element, String attack, String defense, int power, int mana,
			int cooldown, int priority, String target, LowHealthPower lowHealthPower, String description) {
		public Skill(String id, String effect, String element, String attack, String defense, int power, int mana,
				int cooldown, int priority, String target, LowHealthPower lowHealthPower) {
			this(id, effect, element, attack, defense, power, mana, cooldown, priority, target, lowHealthPower, null);
		}

		/** 普攻及首领脚本构造的固定威力攻击不携带低生命条件。 */
		public Skill(String id, String effect, String element, String attack, String defense, int power, int mana,
				int cooldown, int priority, String target) {
			this(id, effect, element, attack, defense, power, mana, cooldown, priority, target, null);
		}
	}

	/** 在实际受击者生命不高于百分比阈值时使用替代威力，不读取提交时的旧生命。 */
	public record LowHealthPower(int percent, int power) {
	}

	/**
	 * 人物系数按生命、物攻、法攻、物防、法防、速度排序，魔力使用一次函数。 幻兽只保留技能及野外生成字段，属性由SpeciesStatsCatalog提供。
	 */
	public record Growth(List<Double> coefficients, int manaBase, int manaPerLevel, List<String> skills,
			double captureRate, int minLevel, int maxLevel, String book, String fragment) {
		public Growth {
			// null 表示未配置职业系数，必须保留以供幻兽配置校验。
			coefficients = coefficients == null ? null : List.copyOf(coefficients);
			skills = List.copyOf(skills);
		}
	}

	public record Encounter(String id, String name, String location, String requires, String captureRequires,
			boolean tutorial, Map<String, Integer> species, Boss boss, String wild, int count) {
		public Encounter {
			species = species == null ? Map.of() : Map.copyOf(species);
		}
	}

	/** 剧情首领为独立模板，不伪装成可捕获物种，脚本及奖励步骤引用均启动校验。 */
	public record Boss(String name, String element, String policy, int level, int health, int attack, int defense,
			int speed, String rewardStep) {
	}

	/** 商品资格供购买命令和展示共用；任务进度不能把商品开放到未配置的商店。 */
	public List<Product> productsAt(String location, Set<String> facts) {
		return definition.products()
			.stream()
			.filter(product -> product.locations().contains(location) && facts.contains("QUEST:" + product.requires()))
			.toList();
	}

	public record Product(String code, int price, String requires, List<String> locations) {
		public Product {
			locations = locations == null ? List.of() : List.copyOf(locations);
		}
	}

	public record Definition(Map<String, Growth> growth, List<Skill> skills, List<Encounter> encounters,
			List<Product> products, Map<String, Map<String, Double>> elements, Map<String, Integer> supplyDrops,
			Map<String, Integer> bookDrops) {
		public Definition {
			growth = Map.copyOf(growth);
			skills = List.copyOf(skills);
			encounters = List.copyOf(encounters);
			products = List.copyOf(products);
			var copiedElements = new LinkedHashMap<String, Map<String, Double>>();
			elements.forEach((element, multipliers) -> copiedElements.put(element, Map.copyOf(multipliers)));
			elements = Map.copyOf(copiedElements);
			supplyDrops = Map.copyOf(supplyDrops);
			bookDrops = Map.copyOf(bookDrops);
		}
	}

}
