package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentChecks;

import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 当前结契伙伴模板，不在任务目录中定义物种；捕捉系统后续复用此领域目录。
 */
@Component
public class CreatureCatalog {

	private final Map<String, Pet> pets;

	private final List<Pet> orderedPets;

	private final Set<String> evolvedSpecies;

	private final EvolutionCatalog.Definition evolution;

	public CreatureCatalog() {
		this(ContentLoader.read("/content/creatures.json", Definition.class),
				ContentLoader.read("/content/evolution.json", EvolutionCatalog.Definition.class));
	}

	public CreatureCatalog(Definition definition, EvolutionCatalog.Definition evolution) {
		this.evolution = evolution;
		Set<String> ids = new HashSet<>();
		Map<String, Pet> entries = new LinkedHashMap<>();
		for (Pet pet : definition.pets()) {
			ContentChecks.checkId(ids, pet.id());
			if (pet.level() < 1 || pet.level() > GrowthRules.LEVEL_CAP
					|| !Set.of("A", "B", "H").contains(pet.abilitySlot()) || pet.skills().size() > 7
					|| new HashSet<>(pet.skills()).size() != pet.skills().size() || pet.name() == null
					|| pet.name().isBlank() || pet.element() == null || pet.element().isBlank() || pet.ability() == null
					|| pet.ability().isBlank() || pet.skills().stream().anyMatch(String::isBlank))
				throw new IllegalArgumentException("伙伴模板无效：" + pet.id());
			entries.put(pet.id(), pet);
		}
		// 新形态继承原属性、普通槽位和初始技能模板，实际进化仍保留个体已有槽位与技能。
		// 这里只扩展物种查询；不能通过赠宠或捕捉入口创建进化形态。
		var evolved = new HashSet<String>();
		appendEvolutionForms(entries, evolution.routes(), evolved);
		pets = Map.copyOf(entries);
		orderedPets = pets.values().stream().sorted(Comparator.comparing(Pet::id)).toList();
		evolvedSpecies = Set.copyOf(evolved);
	}

	/**
	 * 按依赖顺序注册相邻形态，JSON排列顺序不决定进化资格。每轮必须至少完成一条路线， 否则存在循环或缺失源形态，启动直接失败；不悄悄跳过导致玩家断链。
	 */
	static void appendEvolutionForms(Map<String, Pet> entries, List<EvolutionCatalog.Route> routes,
			Set<String> evolved) {
		var ids = new HashSet<>(entries.keySet());
		var sources = new HashSet<String>();
		for (var route : routes) {
			ContentChecks.checkId(ids, route.target());
			ContentChecks.checkId(sources, route.source());
			if (route.source().equals(route.target()) || route.level() < 1 || route.level() > 50)
				throw new IllegalArgumentException("进化目标或等级无效：" + route.source());
		}
		var remaining = new ArrayList<>(routes);
		while (!remaining.isEmpty()) {
			boolean progressed = false;
			for (var iterator = remaining.iterator(); iterator.hasNext();) {
				var route = iterator.next();
				var original = entries.get(route.source());
				if (original == null)
					continue;
				if (route.level() <= original.level())
					throw new IllegalArgumentException("相邻进化等级必须递增：" + route.source());
				entries.put(route.target(), new Pet(route.target(), route.name(), original.element(), route.level(),
						original.abilitySlot(), original.ability(), original.skills(), null, route.description()));
				evolved.add(route.target());
				iterator.remove();
				progressed = true;
			}
			if (!progressed)
				throw new IllegalArgumentException("进化链存在循环或缺失源形态");
		}
	}

	/**
	 * 供任务引用启动校验使用，仅检查目录定义，不代表玩家具备领取资格。
	 */
	/** 形态构建、学习池与进化资格共用同一份只读定义。 */
	public EvolutionCatalog.Definition evolutionDefinition() {
		return evolution;
	}

	public boolean contains(String id) {
		return pets.containsKey(id);
	}

	/** 野外与赠宠内容只允许原形态，新形态必须通过已实现的进化或交易流程获得。 */
	public boolean obtainableDirectly(String id) {
		return contains(id) && !evolvedSpecies.contains(id);
	}

	/**
	 * 读取赠宠模板；实例仍需在持锁命令内创建并保存，不能把模板当作玩家已经拥有的伙伴。
	 */
	public Pet pet(String id) {
		return ContentChecks.required("/content/creatures.json + /content/evolution.json", "species.id", id,
				pets.get(id));
	}

	/** 图鉴按稳定编号展示目录；未捕获个体的特性不从模板推测。 */
	public List<Pet> all() {
		return orderedPets;
	}

	public record Pet(String id, String name, String element, int level, String abilitySlot, String ability,
			List<String> skills, String image, String description) {
		public Pet(String id, String name, String element, int level, String abilitySlot, String ability,
				List<String> skills, String image) {
			this(id, name, element, level, abilitySlot, ability, skills, image, null);
		}

		public Pet {
			skills = skills == null ? List.of() : List.copyOf(skills);
		}
	}

	public record Definition(List<Pet> pets) {
		public Definition {
			pets = List.copyOf(pets);
		}
	}

}
