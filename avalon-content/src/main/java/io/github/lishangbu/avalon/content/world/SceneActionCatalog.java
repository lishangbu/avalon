package io.github.lishangbu.avalon.content.world;

import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** 启动期有限场景动作目录；客户端只选择动作或报告选项，不能提供奖励与成功事实。 */
@Component
public class SceneActionCatalog {

	private final Definition definition;

	private final WorldCatalog world;

	public SceneActionCatalog(WorldCatalog world, QuestCatalog quests, ItemCatalog items) {
		this.world = world;
		definition = validate(ContentLoader.read("/content/scene-actions.json", Definition.class), world.definition(),
				quests.definition(), items);
		validateWorldReferences(definition, world.definition(), quests.definition());
	}

	/** 校验引用与动作依赖，拒绝任意物品、未知事实和纯动作环。 */
	public static Definition validate(Definition definition, WorldCatalog.Definition world,
			QuestCatalog.Definition quests, ItemCatalog items) {
		Set<String> ids = new HashSet<>();
		for (Action action : definition.actions())
			if (action.id() == null || action.id().isBlank() || !ids.add(action.id()))
				throw new IllegalArgumentException("场景动作编号为空或重复");
		Set<String> known = new HashSet<>();
		ids.forEach(id -> known.add("SCENE:" + id));
		quests.steps().forEach(step -> known.add("QUEST:" + step.id()));
		// 胜利事实由战斗结算产生，场景只能等待任务声明的战斗成果，不能自行写入胜利。
		Set<String> victories = new HashSet<>();
		quests.steps()
			.forEach(step -> step.facts().stream().filter(fact -> fact.startsWith("VICTORY:")).forEach(victories::add));
		known.addAll(victories);
		Set<String> informationIds = new HashSet<>();
		for (var information : definition.information()) {
			var place = world.places()
				.stream()
				.filter(p -> p.id().equals(information.location()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("资料所在地点不存在"));
			if (blank(information.id()) || ids.contains(information.id()) || !informationIds.add(information.id())
					|| blank(information.label()) || blank(information.description())
					|| !known.containsAll(information.requires())
					|| information.closedByFact() != null && !known.contains(information.closedByFact())
					|| information.npcId() != null
							&& place.npcs().stream().noneMatch(binding -> binding.npcId().equals(information.npcId())))
				throw new IllegalArgumentException("地点或人物资料文本、编号或前置无效");
		}
		// 资料只能读取，任务与动作不得等待一个已经不再产生的观察事实。
		for (var step : quests.steps())
			for (String fact : step.facts())
				if (fact.startsWith("SCENE:") && !ids.contains(fact.substring(6)))
					throw new IllegalArgumentException("任务引用未知场景动作：" + step.id() + " / " + fact);
		for (Action action : definition.actions()) {
			var place = world.places()
				.stream()
				.filter(p -> p.id().equals(action.location()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("场景动作地点不存在"));
			if (action.type() == null || action.minimumActive() < 0 || action.minimumActive() > 2
					|| blank(action.label()) || blank(action.description()) || blank(action.success())
					|| !known.containsAll(action.requires())
					|| action.closedByFact() != null && !known.contains(action.closedByFact()))
				throw new IllegalArgumentException("场景动作文本、类型或事实条件无效");
			if (action.npcId() != null && (world.npcs().stream().noneMatch(npc -> npc.id().equals(action.npcId()))
					|| place.npcs().stream().noneMatch(npc -> npc.npcId().equals(action.npcId()))))
				throw new IllegalArgumentException("场景接待人物没有地点绑定");
			if (action.type() == Type.COLLECT_TASK || action.type() == Type.DELIVER_TASK) {
				if (!items.hasItem(action.itemCode()) || !items.item(action.itemCode()).task()
						|| action.quantity() <= 0)
					throw new IllegalArgumentException("场景物品交接只允许配置正数任务物品");
				// 消耗物品的交接必须归属唯一目标；后续任务可以通过步骤前置引用交接结果。
				if (action.type() == Type.DELIVER_TASK && quests.steps()
					.stream()
					.filter(step -> step.facts().contains("SCENE:" + action.id()))
					.count() > 1)
					throw new IllegalArgumentException("消耗物品的场景动作不能同时作为多个任务目标：" + action.id());
			}
			else if (action.itemCode() != null || action.quantity() != 0)
				throw new IllegalArgumentException("非采集动作不能发物品");
			Set<String> choices = new HashSet<>();
			for (Choice choice : action.choices())
				if (blank(choice.id()) || blank(choice.label()) || !choices.add(choice.id()))
					throw new IllegalArgumentException("报告选项为空或重复");
			if (action.type() == Type.PRESENT_RECORD) {
				if (choices.size() < 2 || action.answers().isEmpty() || !choices.containsAll(action.answers())
						|| blank(action.incorrect()))
					throw new IllegalArgumentException("报告缺少有效选项、正确答案或重看反馈");
			}
			else if (!choices.isEmpty() || !action.answers().isEmpty() || action.answer() != null
					|| action.incorrect() != null)
				throw new IllegalArgumentException("非报告动作不能配置答案");
		}
		// 合并任务与场景动作事实依赖，避免任务等动作、动作又等同一任务的隐蔽闭环。
		Set<String> reached = new HashSet<>(victories);
		Set<String> locations = new HashSet<>();
		boolean changed;
		do {
			changed = false;
			// 道路与任务交替展开；只检查目标地点门槛会漏掉途中唯一道路被自身任务锁住的情况。
			for (var place : world.places())
				if ((place.id().equals(world.start()) || world.places()
					.stream()
					.filter(origin -> locations.contains(origin.id()))
					.anyMatch(origin -> origin.exits().contains(place.id())))
						&& receptionReachable(place.id(), null, world, reached))
					changed |= locations.add(place.id());
			for (var step : quests.steps())
				if (step.requires().stream().allMatch(id -> reached.contains("QUEST:" + id))
						&& locations.contains(step.acceptance().location()) && locations.contains(step.location())
						&& receptionReachable(step.acceptance().location(), step.acceptance().npcId(), world, reached)
						&& receptionReachable(step.location(), step.npcId(), world, reached)
						&& step.facts().stream().filter(fact -> fact.startsWith("SCENE:")).allMatch(reached::contains))
					changed |= reached.add("QUEST:" + step.id());
			for (var action : definition.actions())
				if (reached.containsAll(action.requires()) && locations.contains(action.location())
						&& receptionReachable(action.location(), action.npcId(), world, reached))
					changed |= reached.add("SCENE:" + action.id());
		}
		while (changed);
		if (!reached.containsAll(known))
			throw new IllegalArgumentException("场景动作、任务前置或接待门槛存在环或未知场景事实");
		return definition;
	}

	/** 将地点与人物的开放门槛纳入依赖图，不能用任务自身完成事实解锁自己的接取或交付入口。 */
	private static boolean receptionReachable(String location, String npc, WorldCatalog.Definition world,
			Set<String> reached) {
		var place = world.places().stream().filter(value -> value.id().equals(location)).findFirst().orElseThrow();
		if (place.requiredFact() != null && !reached.contains(place.requiredFact())
				|| !reached.containsAll(place.requiredFacts()))
			return false;
		return npc == null || place.npcs()
			.stream()
			.anyMatch(binding -> binding.npcId().equals(npc)
					&& (binding.requiredFact() == null || reached.contains(binding.requiredFact())));
	}

	/** 完整目录加载后核验地图门禁；动作单元测试可独立校验局部动作集合。 */
	static void validateWorldReferences(Definition definition, WorldCatalog.Definition world,
			QuestCatalog.Definition quests) {
		Set<String> known = new HashSet<>();
		definition.actions().forEach(action -> known.add("SCENE:" + action.id()));
		quests.steps().forEach(step -> known.add("QUEST:" + step.id()));
		for (var place : world.places()) {
			var gates = new ArrayList<>(place.requiredFacts());
			if (place.requiredFact() != null)
				gates.add(place.requiredFact());
			if (place.closedByFact() != null)
				gates.add(place.closedByFact());
			if (!known.containsAll(gates))
				throw new IllegalArgumentException("地图引用未知场景或任务事实：" + place.id());
		}
		for (var destination : world.safeReturns())
			if (destination.requires() != null && !known.contains(destination.requires()))
				throw new IllegalArgumentException("返回点引用未知场景或任务事实");
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	public List<Action> actions() {
		return definition.actions();
	}

	public Action action(String id) {
		return actions().stream().filter(action -> action.id().equals(id)).findFirst().orElse(null);
	}

	/** 查询与写命令共用资格检查；人物离开、地点关闭和前置变化都不能只靠页面隐藏。 */
	public boolean available(Action action, Predicate<String> facts) {
		var place = world.place(action.location());
		return world.accessible(place, facts) && action.requires().stream().allMatch(facts)
				&& (action.closedByFact() == null || !facts.test(action.closedByFact()))
				&& (action.npcId() == null || place.npcs()
					.stream()
					.anyMatch(
							binding -> binding.npcId().equals(action.npcId()) && WorldCatalog.present(binding, facts)));
	}

	public enum Type {

		COLLECT_TASK, DELIVER_TASK, CARE, PRESENT_RECORD

	}

	public record Choice(String id, String label) {
	}

	/** answer只存在服务端内容，不允许将本记录直接作为HTTP读投影返回。 */
	public record Action(String id, Type type, String location, String label, String description, List<String> requires,
			String closedByFact, String npcId, String itemCode, int quantity, List<Choice> choices, String answer,
			List<String> answers, String success, String incorrect, int minimumActive) {
		public Action {
			requires = requires == null ? List.of() : List.copyOf(requires);
			choices = choices == null ? List.of() : List.copyOf(choices);
			answers = answers == null ? answer == null ? List.of() : List.of(answer) : List.copyOf(answers);
		}

	}

	/** 直接显示在设施或人物页面的资料；读取不生成事实、回执或奖励。 */
	public record Information(String id, String location, String label, String description, List<String> requires,
			String closedByFact, String npcId) {
		public Information {
			requires = requires == null ? List.of() : List.copyOf(requires);
		}
	}

	public List<Information> information() {
		return definition.information();
	}

	public boolean available(Information information, Predicate<String> facts) {
		var place = world.place(information.location());
		return world.accessible(place, facts) && information.requires().stream().allMatch(facts)
				&& (information.closedByFact() == null || !facts.test(information.closedByFact()))
				&& (information.npcId() == null || place.npcs()
					.stream()
					.anyMatch(binding -> binding.npcId().equals(information.npcId())
							&& WorldCatalog.present(binding, facts)));
	}

	public record Definition(List<Action> actions, List<Information> information) {
		public Definition(List<Action> actions) {
			this(actions, List.of());
		}

		public Definition {
			actions = actions == null ? List.of() : List.copyOf(actions);
			information = information == null ? List.of() : List.copyOf(information);
		}
	}

}
