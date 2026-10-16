package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.application.model.QuestAction;
import io.github.lishangbu.avalon.world.application.model.QuestJournalEntry;
import io.github.lishangbu.avalon.world.application.model.QuestJournalStep;
import io.github.lishangbu.avalon.world.application.model.QuestState;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalState;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import io.github.lishangbu.avalon.world.quest.model.QuestStepView;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务读侧从一条凭证查询派生已完成步骤和任务，避免并发领奖造成两份进度互相矛盾。
 */
@Component
public class QuestQueryService {

	private final CharacterQueryService characters;

	private final QuestRepository receipts;

	private final QuestCatalog catalog;

	private final CharacterFactQueryService facts;

	private final SceneActionCatalog scenes;

	private final WorldCatalog world;

	private final BattleCatalog battles;

	private final QuestRulesCatalog rules;

	private final QuestJournalStateRepository participation;

	private final QuestObjectiveQueryService objectives;

	private final QuestDeliveryRequirements delivery;

	/** 唯一运行状态来自实例引擎。 */
	private final QuestRuntimeService runtime;

	@Autowired
	public QuestQueryService(CharacterQueryService characters, QuestRepository receipts, QuestCatalog catalog,
			CharacterFactQueryService facts, SceneActionCatalog scenes, WorldCatalog world, BattleCatalog battles,
			QuestRulesCatalog rules, QuestJournalStateRepository participation, QuestObjectiveQueryService objectives,
			QuestDeliveryRequirements delivery, QuestRuntimeService runtime) {
		this.characters = characters;
		this.receipts = receipts;
		this.catalog = catalog;
		this.facts = facts;
		this.scenes = scenes;
		this.world = world;
		this.battles = battles;
		this.rules = rules;
		this.participation = participation;
		this.objectives = objectives;
		this.delivery = delivery;
		this.runtime = runtime;
	}

	/**
	 * 遭遇解锁使用精确步骤回执，不通过前端任务按钮推断进度。
	 */
	@Transactional
	public boolean done(long owner, String step) {
		return receipts.done(characters.requireId(owner)).contains(step);
	}

	/**
	 * 从一次凭证查询同时派生已完成步骤和整项任务，再用不可变内容目录筛选可用步骤。这样避免两个 READ COMMITTED
	 * 查询分别观察到领奖提交前后状态；查询不锁角色，也不执行奖励。
	 */
	@Transactional
	public QuestState state(long owner) {
		var progress = receipts.progress(characters.requireId(owner));
		var completedFacts = facts.codes(owner);
		var runtimeSteps = runtime.steps(owner);
		var targets = objectives.progress(characters.requireId(owner));
		var states = participation.findByCharacterId(characters.requireId(owner));
		Set<String> done = new HashSet<>();
		for (var receipt : progress) {
			done.add(receipt.step());
		}
		// 当前轮次覆盖历史回执，下一周期的同名步骤不会被永久完成记录隐藏。
		runtimeSteps.forEach((step, value) -> {
			if (value.completed())
				done.add(step);
			else
				done.remove(step);
		});
		var available = catalog.definition()
			.steps()
			.stream()
			.filter(step -> runtimeSteps.containsKey(step.id()) ? runtimeSteps.get(step.id()).available()
					: !done.contains(step.id()) && done.containsAll(step.requires()))
			.map(step -> {
				var running = runtimeSteps.get(step.id());
				boolean accepted = running != null && running.accepted();
				return new QuestAction(step.id(), step.quest(), step.title(),
						accepted ? step.location() : step.acceptance().location(),
						accepted ? catalog.npcName(step) : catalog.acceptanceNpcName(step), step.label(),
						running != null && running.ready() && step.delivery() == QuestDelivery.MANUAL
								&& catalog.receptionAvailable(step, completedFacts::contains)
								&& delivery.missing(owner, step) == null
								&& (step.acceptEffects().isEmpty()
										|| completedFacts.contains("QUEST_ACCEPT:" + step.id())),
						accepted ? step.npcId() : step.acceptance().npcId(), true, accepted,
						!accepted && catalog.acceptanceAvailable(step, completedFacts::contains),
						!accepted
								? "前往" + world.place(step.acceptance().location()).name() + "，接取「" + step.title() + "」。"
								: guide(step, QuestObjectiveQueryService.factsFor(step, completedFacts, targets)),
						running == null ? null : running.instance().id(),
						running == null ? 1 : running.instance().round());
			})
			.toList();
		var completed = new ArrayList<>(catalog.completedQuests(done));
		// 分支完成只要求所选路线，不能再用所有静态步骤判断整项任务是否完成。
		runtimeSteps.values().forEach(value -> {
			if (value.instance().status() == QuestStatus.COMPLETED
					&& rules.rule(value.instance().quest()).policy().repeatPolicy() == QuestRepeatPolicy.ONCE
					&& !completed.contains(value.instance().quest()))
				completed.add(value.instance().quest());
		});
		return new QuestState(available, completed, journal(done, completed, available, states, runtimeSteps));
	}

	/** 同一份回执生成待办与完成记录；多步骤任务只显示一行，尚未开启的任务不暴露。 */
	private List<QuestJournalEntry> journal(Set<String> done, List<String> completed, List<QuestAction> available,
			List<QuestJournalState> states, Map<String, QuestStepView> runtimeSteps) {
		Set<String> visible = new HashSet<>(available.stream().map(QuestAction::quest).toList());
		var availableById = available.stream().collect(Collectors.toMap(QuestAction::id, value -> value));
		states.forEach(state -> visible.add(state.getQuestCode()));
		catalog.definition()
			.steps()
			.stream()
			.filter(step -> done.contains(step.id()))
			.forEach(step -> visible.add(step.quest()));
		return rules.definition().quests().stream().filter(rule -> visible.contains(rule.id())).map(rule -> {
			var state = states.stream()
				.filter(value -> value.getQuestCode().equals(rule.id()))
				.findFirst()
				.orElse(null);
			var instance = runtimeSteps.values()
				.stream()
				.filter(value -> value.instance().quest().equals(rule.id()))
				.map(QuestStepView::instance)
				.findFirst()
				.orElse(null);
			// 选定路线后只统计公共步骤与所选路线，避免永远显示缺少未选路线的完成数量。
			var allSteps = catalog.definition()
				.steps()
				.stream()
				.filter(step -> step.quest().equals(rule.id()))
				.filter(step -> instance == null || step.branch() == null
						|| !instance.choices().containsKey(step.branch().group())
						|| instance.choices().get(step.branch().group()).equals(step.branch().option()))
				.toList();
			// 尚未开放的后续步骤不返回标题或地点；已完成记录和当前入口仍可查看。
			var steps = allSteps.stream()
				.filter(step -> done.contains(step.id()) || availableById.containsKey(step.id()))
				.map(step -> new QuestJournalStep(step.id(), step.title(),
						availableById.containsKey(step.id()) ? availableById.get(step.id()).location()
								: step.location(),
						done.contains(step.id())))
				.toList();
			var labels = rules.definition().tags().stream().filter(tag -> rule.tags().contains(tag.id())).toList();
			// 读侧采用引擎预览状态，离线超时无需等待下一条写命令才能显示失败。
			String status = state == null ? "AVAILABLE" : state.getStatus().name();
			if (instance != null)
				status = switch (instance.status()) {
					case ACTIVE, READY -> "ACTIVE";
					case PAUSED, ABANDONED -> "ABANDONED";
					case FAILED -> "FAILED";
					case EXPIRED -> "EXPIRED";
					case COMPLETED -> "COMPLETED";
				};
			if (!completed.contains(rule.id()) && "COMPLETED".equals(status)
					&& available.stream().anyMatch(action -> action.quest().equals(rule.id())))
				status = "AVAILABLE";
			return new QuestJournalEntry(rule.id(), rule.title(), rule.type(), labels, rule.policy().repeatPolicy(),
					completed.contains(rule.id()), (int) steps.stream().filter(QuestJournalStep::completed).count(),
					allSteps.size(), steps, completed.contains(rule.id()) ? "COMPLETED" : status, rule.abandonable(),
					state == null ? 0 : state.getRevision());
		}).toList();
	}

	/** 从尚未满足的事实推导下一步，领取与交付地点不同的任务不能只显示最终交付地点。 */
	private String guide(QuestContentStep step, Set<String> completed) {
		for (String fact : step.objectives().stream().map(objective -> objective.code()).toList()) {
			if (completed.contains(fact))
				continue;
			if (fact.startsWith("SCENE:")) {
				var action = scenes.action(fact.substring(6));
				if (action != null)
					return "前往" + world.place(action.location()).name() + "，" + action.label() + "。";
			}
			if (fact.startsWith("VICTORY:")) {
				var encounter = battles.definition()
					.encounters()
					.stream()
					.filter(value -> fact.equals("VICTORY:" + value.id()))
					.findFirst();
				if (encounter.isPresent())
					return "前往" + world.place(encounter.get().location()).name() + "，完成「" + encounter.get().name()
							+ "」战斗。";
			}
			if (fact.startsWith("VISIT:"))
				return "沿相邻道路前往" + world.place(fact.substring(6)).name() + "，到达后自动记录行程。";
		}
		return "前往" + world.place(step.location()).name() + "，" + step.label() + "。";
	}

}
