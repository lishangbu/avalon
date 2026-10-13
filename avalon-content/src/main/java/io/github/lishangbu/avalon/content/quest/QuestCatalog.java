package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import java.util.HashSet;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 类型化任务目录。任务编号只出现在内容中；执行器只理解步骤前置和通用效果。
 */
@Component
public class QuestCatalog {

	private final QuestContentDefinition definition;

	private final WorldDefinition world;

	private final Map<String, QuestContentStep> steps;

	private final Map<String, WorldPlace> places;

	@Autowired
	public QuestCatalog(WorldCatalog world, ItemCatalog items, CreatureCatalog creatures) {
		this(ContentLoader.read("/content/quests.json", QuestContentDefinition.class), world, items, creatures);
	}

	public QuestCatalog(QuestContentDefinition definition, WorldCatalog world, ItemCatalog items,
			CreatureCatalog creatures) {
		this.world = world.definition();
		this.definition = validate(definition, world.definition(), items, creatures);
		validateWorldReferences(definition, this.world);
		steps = ContentChecks.index("/content/quests.json", "steps.id", definition.steps(), QuestContentStep::id);
		places = ContentChecks.index("/content/opening.json", "places.id", this.world.places(), WorldPlace::id);
	}

	/** 地图门槛与安全返回点必须引用正式任务，避免拼错编号造成可见但永远无法进入的地点。 */
	static void validateWorldReferences(QuestContentDefinition quests, WorldDefinition world) {
		Set<String> ids = new HashSet<>(quests.steps().stream().map(QuestContentStep::id).toList());
		for (var place : world.places()) {
			validateWorldFact(place.requiredFact(), ids, place.id());
			validateWorldFact(place.closedByFact(), ids, place.id());
			for (String fact : place.requiredFacts())
				validateWorldFact(fact, ids, place.id());
		}
		for (var destination : world.safeReturns())
			validateWorldFact(destination.requires(), ids, destination.location());
		for (var step : quests.steps())
			for (String fact : step.objectives().stream().map(objective -> objective.code()).toList())
				if (fact.startsWith("VISIT:")
						&& world.places().stream().noneMatch(p -> p.id().equals(fact.substring(6))))
					throw new IllegalArgumentException("任务引用未知到访地点：" + step.id() + " / " + fact);
	}

	private static void validateWorldFact(String fact, Set<String> ids, String location) {
		// 场景门禁由场景目录核验完整引用，避免这里反向依赖场景目录形成构造环。
		if (fact != null && fact.startsWith("SCENE:"))
			return;
		if (fact != null && (!fact.startsWith("QUEST:") || !ids.contains(fact.substring(6))))
			throw new IllegalArgumentException("地点引用未知任务事实：" + location + " / " + fact);
	}

	/** 返回唯一正式任务目录，供地图、场景动作和战斗引用校验。 */
	public QuestContentDefinition definition() {
		return definition;
	}

	/**
	 * 验证步骤编号、地点和NPC匹配、前置存在、效果数量及跨目录引用。通过反复扩展可达步骤检测依赖环。校验不会执行奖励，不给任务编号编写特殊分支。
	 */
	public static QuestContentDefinition validate(QuestContentDefinition d, WorldDefinition world, ItemCatalog items,
			CreatureCatalog creatures) {
		Set<String> steps = unique(d.steps().stream().map(QuestContentStep::id).toList());
		for (var place : world.places())
			for (var binding : place.npcs()) {
				validateNpcFact(binding.requiredFact(), steps);
				validateNpcFact(binding.closedByFact(), steps);
			}
		for (QuestContentStep s : d.steps()) {
			WorldPlace place = world.places()
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
			if (!s.acceptEffects().isEmpty() && (s.environmental() || (s.delivery() == QuestDelivery.AUTO)
					|| s.acceptEffects().stream().anyMatch(effect -> !effect.kind().equals("GIVE_ITEM"))))
				throw new IllegalArgumentException("接取补给仅允许人物手动接取时发普通物品");
			for (QuestEffect e : Stream.concat(s.effects().stream(), s.acceptEffects().stream()).toList()) {
				if (e.kind() == null || e.amount() <= 0)
					throw new IllegalArgumentException(
							"/content/quests.json [" + s.id() + "] effects.amount=" + e.amount() + "：奖励数量无效");
				boolean valid = switch (e.kind()) {
					case "EXPERIENCE", "GOLD" -> e.reference() == null;
					case "GIVE_ITEM", "TAKE_ITEM" -> items.hasItem(e.reference());
					case "GIVE_PET" -> creatures.obtainableDirectly(e.reference()) && e.amount() == 1;
					case "GIVE_EQUIPMENT" -> items.hasEquipment(e.reference()) && e.amount() == 1;
					default -> true; // 扩展效果由宿主注册器核验。
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
			for (QuestContentStep s : d.steps())
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

	private static boolean validReception(QuestContentStep step, WorldPlace place, WorldDefinition world) {
		// 设施资料可在原地直接确认；涉及实际行动的步骤仍检查事实，不允许到达自动领奖。
		if (step.environmental())
			return step.npcId() == null
					&& step.objectives().stream().noneMatch(goal -> goal.code().startsWith("QUEST:"));
		if (step.npcId() == null)
			return false;
		return world.npcs().stream().anyMatch(npc -> npc.id().equals(step.npcId()))
				&& place.npcs().stream().anyMatch(binding -> binding.npcId().equals(step.npcId()));
	}

	/** 查询与命令共用稳定人物编号和当前地点的接待资格。 */
	public boolean receptionAvailable(QuestContentStep step, Predicate<String> hasFact) {
		return receptionAvailable(step, world, hasFact);
	}

	/** 接取入口与交付入口独立，接取时不能要求尚未抵达的交付人物出现。 */
	public boolean acceptanceAvailable(QuestContentStep step, Predicate<String> hasFact) {
		var reception = step.acceptance();
		var place = places.get(reception.location());
		if (!WorldCatalog.allowsAccess(place, hasFact))
			return false;
		return reception.npcId() == null || place.npcs()
			.stream()
			.anyMatch(binding -> binding.npcId().equals(reception.npcId()) && WorldCatalog.present(binding, hasFact));
	}

	public String acceptanceNpcName(QuestContentStep step) {
		String id = step.acceptance().npcId();
		return id == null ? ""
				: world.npcs().stream().filter(npc -> npc.id().equals(id)).findFirst().orElseThrow().name();
	}

	/** 对指定只读定义执行同一资格检查，便于验证内容本身而无需启动容器。 */
	static boolean receptionAvailable(QuestContentStep step, WorldDefinition world, Predicate<String> hasFact) {
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
	public String npcName(QuestContentStep step) {
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
	public QuestContentStep step(String id) {
		return steps.get(id);
	}

	/**
	 * 整项任务完成以所有所属步骤的回执为准。学院并行课程必须全部完成，不能依赖任意一个步骤的最终标记。 单步骤任务同样适用，避免遗漏 completes
	 * 标记后永远不出现在完成记录中。
	 */
	public List<String> completedQuests(Set<String> completedSteps) {
		var steps = definition().steps();
		return steps.stream()
			.map(QuestContentStep::quest)
			.distinct()
			.filter(quest -> steps.stream()
				.filter(step -> step.quest().equals(quest))
				.allMatch(step -> completedSteps.contains(step.id())))
			.toList();
	}

}
