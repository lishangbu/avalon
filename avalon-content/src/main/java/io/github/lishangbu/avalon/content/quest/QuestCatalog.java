package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.ContentChecks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类型化任务目录。任务编号只出现在内容中；执行器只理解步骤前置和通用效果。
 */
@Component
public class QuestCatalog {

	private final Definition definition;

	private final WorldCatalog.Definition world;

	private final Map<String, Step> steps;

	private final Map<String, WorldCatalog.Place> places;

	@Autowired
	public QuestCatalog(WorldCatalog world, ItemCatalog items, CreatureCatalog creatures) {
		this(ContentLoader.read("/content/quests.json", Definition.class), world, items, creatures);
	}

	public QuestCatalog(Definition definition, WorldCatalog world, ItemCatalog items, CreatureCatalog creatures) {
		this.world = world.definition();
		this.definition = validate(definition, world.definition(), items, creatures);
		validateWorldReferences(definition, this.world);
		steps = ContentChecks.index("/content/quests.json", "steps.id", definition.steps(), Step::id);
		places = ContentChecks.index("/content/opening.json", "places.id", this.world.places(), WorldCatalog.Place::id);
	}

	/** 地图门槛与安全返回点必须引用正式任务，避免拼错编号造成可见但永远无法进入的地点。 */
	static void validateWorldReferences(Definition quests, WorldCatalog.Definition world) {
		Set<String> ids = new HashSet<>(quests.steps().stream().map(Step::id).toList());
		for (var place : world.places()) {
			validateWorldFact(place.requiredFact(), ids, place.id());
			validateWorldFact(place.closedByFact(), ids, place.id());
			for (String fact : place.requiredFacts())
				validateWorldFact(fact, ids, place.id());
		}
		for (var destination : world.safeReturns())
			validateWorldFact(destination.requires(), ids, destination.location());
	}

	private static void validateWorldFact(String fact, Set<String> ids, String location) {
		// 场景门禁由场景目录核验完整引用，避免这里反向依赖场景目录形成构造环。
		if (fact != null && fact.startsWith("SCENE:"))
			return;
		if (fact != null && (!fact.startsWith("QUEST:") || !ids.contains(fact.substring(6))))
			throw new IllegalArgumentException("地点引用未知任务事实：" + location + " / " + fact);
	}

	/** 返回唯一正式任务目录，供地图、场景动作和战斗引用校验。 */
	public Definition definition() {
		return definition;
	}

	/**
	 * 验证步骤编号、地点和NPC匹配、前置存在、效果数量及跨目录引用。通过反复扩展可达步骤检测依赖环。校验不会执行奖励，不给任务编号编写特殊分支。
	 */
	public static Definition validate(Definition d, WorldCatalog.Definition world, ItemCatalog items,
			CreatureCatalog creatures) {
		Set<String> steps = unique(d.steps().stream().map(Step::id).toList());
		for (var place : world.places())
			for (var binding : place.npcs()) {
				validateNpcFact(binding.requiredFact(), steps);
				validateNpcFact(binding.closedByFact(), steps);
			}
		for (Step s : d.steps()) {
			if (s.placement() != null && (s.placement().sourcePrefix() == null || s.placement().sourcePrefix().isBlank()
					|| s.placement().position() < 0 || s.placement().position() > 3))
				throw new IllegalArgumentException(
						"/content/quests.json [" + s.id() + "] placement=" + s.placement() + "：编队条件无效");
			WorldCatalog.Place place = world.places()
				.stream()
				.filter(p -> p.id().equals(s.location()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"/content/quests.json [" + s.id() + "] location=" + s.location() + "：任务地点不存在"));
			if (s.quest() == null || s.title() == null || s.label() == null || s.dialogue() == null
					|| !validReception(s, place, world) || !steps.containsAll(s.requires()))
				throw new IllegalArgumentException("/content/quests.json [" + s.id() + "] requires=" + s.requires()
						+ ", npcId=" + s.npcId() + "：任务前置或 NPC 无效");
			var acceptance = s.acceptance();
			var origin = world.places()
				.stream()
				.filter(p -> p.id().equals(acceptance.location()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("/content/quests.json [" + s.id()
						+ "] acceptance.location=" + acceptance.location() + "：任务接取地点不存在"));
			if (acceptance.npcId() != null
					&& (world.npcs().stream().noneMatch(npc -> npc.id().equals(acceptance.npcId()))
							|| origin.npcs().stream().noneMatch(binding -> binding.npcId().equals(acceptance.npcId()))))
				throw new IllegalArgumentException(
						"/content/quests.json [" + s.id() + "] acceptance=" + acceptance + "：任务接取人物没有对应地点绑定");
			if (!s.acceptEffects().isEmpty() && (s.environmental() || s.onArrival()
					|| s.acceptEffects().stream().anyMatch(effect -> effect.kind() != Kind.GIVE_ITEM)))
				throw new IllegalArgumentException("接取补给仅允许人物手动接取时发普通物品");
			for (Effect e : Stream.concat(s.effects().stream(), s.acceptEffects().stream()).toList()) {
				if (e.kind() == null || e.amount() <= 0)
					throw new IllegalArgumentException(
							"/content/quests.json [" + s.id() + "] effects.amount=" + e.amount() + "：奖励数量无效");
				boolean valid = switch (e.kind()) {
					case EXPERIENCE, GOLD -> e.reference() == null;
					case GIVE_ITEM, TAKE_ITEM -> items.hasItem(e.reference());
					case GIVE_PET -> creatures.obtainableDirectly(e.reference()) && e.amount() == 1;
					case GIVE_EQUIPMENT -> items.hasEquipment(e.reference()) && e.amount() == 1;
				};
				if (!valid)
					throw new IllegalArgumentException("/content/quests.json [" + s.id() + "] effects.reference="
							+ e.reference() + ", kind=" + e.kind() + "：奖励引用无效");
			}
		}
		Set<String> reached = new HashSet<>();
		boolean changed;
		do {
			changed = false;
			for (Step s : d.steps())
				if (reached.containsAll(s.requires()))
					changed |= reached.add(s.id());
		}
		while (changed);
		if (reached.size() != steps.size())
			throw new IllegalArgumentException("/content/quests.json requires：任务依赖存在环，未解析步骤="
					+ steps.stream().filter(id -> !reached.contains(id)).sorted().toList());
		return d;
	}

	/** 接待事实目前只允许指向本任务目录的完成步骤，拼错或未经实现的事实不能启动。 */
	private static void validateNpcFact(String fact, Set<String> steps) {
		if (fact != null && (!fact.startsWith("QUEST:") || !steps.contains(fact.substring(6))))
			throw new IllegalArgumentException("NPC 接待条件引用未知任务事实");
	}

	private static boolean validReception(Step step, WorldCatalog.Place place, WorldCatalog.Definition world) {
		// 环境任务由真实行动事实支撑，不给石头或战场编造接待人物，也不允许到达即领奖。
		if (step.environmental())
			return step.npcId() == null && !step.onArrival() && !step.facts().isEmpty()
					&& step.facts()
						.stream()
						.anyMatch(fact -> fact.startsWith("SCENE:") || fact.startsWith("VICTORY:")
								|| fact.startsWith("TRAINING:"));
		if (step.npcId() == null)
			return false;
		return world.npcs().stream().anyMatch(npc -> npc.id().equals(step.npcId()))
				&& place.npcs().stream().anyMatch(binding -> binding.npcId().equals(step.npcId()));
	}

	/** 查询与命令共用稳定人物编号和当前地点的接待资格。 */
	public boolean receptionAvailable(Step step, Predicate<String> hasFact) {
		return receptionAvailable(step, world, hasFact);
	}

	/** 接取入口与交付入口独立，接取时不能要求尚未抵达的交付人物出现。 */
	public boolean acceptanceAvailable(Step step, Predicate<String> hasFact) {
		var reception = step.acceptance();
		var place = places.get(reception.location());
		if (!WorldCatalog.allowsAccess(place, hasFact))
			return false;
		return reception.npcId() == null || place.npcs()
			.stream()
			.anyMatch(binding -> binding.npcId().equals(reception.npcId()) && WorldCatalog.present(binding, hasFact));
	}

	public String acceptanceNpcName(Step step) {
		String id = step.acceptance().npcId();
		return id == null ? ""
				: world.npcs().stream().filter(npc -> npc.id().equals(id)).findFirst().orElseThrow().name();
	}

	/** 对指定只读定义执行同一资格检查，便于验证内容本身而无需启动容器。 */
	static boolean receptionAvailable(Step step, WorldCatalog.Definition world, Predicate<String> hasFact) {
		var place = world.places()
			.stream()
			.filter(value -> value.id().equals(step.location()))
			.findFirst()
			.orElseThrow();
		if (!validReception(step, place, world))
			return false;
		if (step.npcId() == null)
			return true;
		return place.npcs()
			.stream()
			.anyMatch(binding -> binding.npcId().equals(step.npcId()) && WorldCatalog.present(binding, hasFact));
	}

	/** 名称只从已校验名录取得，客户端不能通过自由姓名选择接待者。 */
	public String npcName(Step step) {
		if (step.environmental())
			return "";
		return world.npcs().stream().filter(npc -> npc.id().equals(step.npcId())).findFirst().orElseThrow().name();
	}

	private static Set<String> unique(List<String> ids) {
		Set<String> set = new HashSet<>();
		for (String id : ids)
			if (id == null || id.isBlank() || !set.add(id))
				throw new IllegalArgumentException("内容编号重复或为空");
		return set;
	}

	/**
	 * 按稳定步骤编号定位定义。未知请求返回 null，由命令层转换404；不能根据步骤显示标题执行任务。
	 */
	public Step step(String id) {
		return steps.get(id);
	}

	/**
	 * 整项任务完成以所有所属步骤的回执为准。学院并行课程必须全部完成，不能依赖任意一个步骤的最终标记。 单步骤任务同样适用，避免遗漏 completes
	 * 标记后永远不出现在完成记录中。
	 */
	public List<String> completedQuests(Set<String> completedSteps) {
		var steps = definition().steps();
		return steps.stream()
			.map(Step::quest)
			.distinct()
			.filter(quest -> steps.stream()
				.filter(step -> step.quest().equals(quest))
				.allMatch(step -> completedSteps.contains(step.id())))
			.toList();
	}

	public enum Kind {

		EXPERIENCE, GOLD, GIVE_ITEM, TAKE_ITEM, GIVE_PET, GIVE_EQUIPMENT

	}

	/** 校验奖励引用与步骤有向无环关系，缺少配置时拒绝开服，禁止静默忽略未知效果。 */

	public record Effect(Kind kind, String reference, int amount) {
	}

	/** 任务仅引用稳定人物编号；环境步骤不设置人物，奖励和接取补给分别配置。 */
	public record Step(String id, String quest, String title, String location, String label, String dialogue,
			List<String> requires, boolean completes, List<Effect> effects, Placement placement, List<String> facts,
			int minimumActive, boolean onArrival, String npcId, boolean environmental, List<Effect> acceptEffects,
			Reception acceptance) {
		public Step {
			// 多数委托在同一处接取与回报，只有不同地点的委托需要显式覆盖，不能让页面猜测。
			acceptance = acceptance == null ? new Reception(location, npcId) : acceptance;
			requires = requires == null ? List.of() : List.copyOf(requires);
			effects = effects == null ? List.of() : List.copyOf(effects);
			acceptEffects = acceptEffects == null ? List.of() : List.copyOf(acceptEffects);
			facts = facts == null ? List.of() : List.copyOf(facts);
			if (facts.stream().anyMatch(code -> code == null || code.isBlank()))
				throw new IllegalArgumentException("任务事实条件为空");
			if (minimumActive < 0 || minimumActive > 2)
				throw new IllegalArgumentException("任务出战数量条件无效");
		}
	}

	/** npcId为空表示现场接取；无须为环境目标创造虚构人物。 */
	public record Reception(String location, String npcId) {
		public Reception {
			if (location == null || location.isBlank() || npcId != null && npcId.isBlank())
				throw new IllegalArgumentException("接取地点或人物编号为空");
		}
	}

	/**
	 * 声明式持有条件；来源前缀对应捕获事件，不会把幻兽作为任务交付物扣除。
	 */
	public record Placement(String sourcePrefix, int position) {
	}

	public record Definition(List<Step> steps) {
		public Definition {
			steps = List.copyOf(steps);
		}
	}

}
