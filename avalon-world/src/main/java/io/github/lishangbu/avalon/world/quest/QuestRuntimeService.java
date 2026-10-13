package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.model.PlayerNotice;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.quest.*;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.persistence.entity.*;
import io.github.lishangbu.avalon.world.persistence.repository.*;
import io.github.lishangbu.avalon.world.quest.model.QuestAcceptance;
import io.github.lishangbu.avalon.world.quest.model.QuestApplied;
import io.github.lishangbu.avalon.world.quest.model.QuestBranchView;
import io.github.lishangbu.avalon.world.quest.model.QuestGoalView;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceView;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import io.github.lishangbu.avalon.world.quest.model.QuestStepView;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 实例引擎的Avalon宿主。角色锁串行化次数检查、分支选择、目标更新、结算与请求回执。 所有资产效果与状态写入同事务，不把核心返回结果当作已提交结果；任一失败整体回滚。
 * 剧情回执与日志投影仅服务展示和世界门禁，执行资格始终由实例历史决定。
 */
@Component
@RequiredArgsConstructor
public class QuestRuntimeService {

	private final QuestRuntimeInstanceRepository instances;

	private final QuestRuntimeRequestRepository requests;

	private final QuestJournalStateRepository participation;

	private final QuestRepository completions;

	private final QuestAcceptReceiptRepository acceptance;

	private final QuestEngineCatalog definitions;

	private final QuestCatalog catalog;

	private final QuestRulesCatalog rules;

	private final QuestExtensions extensions;

	private final QuestClock clock;

	private final CharacterCommandService characters;

	private final CharacterQueryService characterQueries;

	private final CharacterFactQueryService facts;

	private final CharacterFactCommandService factCommands;

	private final InventoryQueryService inventory;

	private final ObjectMapper json;

	/** 通知也在原事务内持久化，失败会撤销任务结算与全部资产效果。 */
	private final ApplicationEventPublisher notices;

	private final QuestEngine engine = new QuestEngine();

	/**
	 * 接取命令在地点、请求和前置检查后调用，不导入其他格式的状态。 返回是否新开实例，接取补给可以据此采用实例级来源，避免每日任务只发第一次补给。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public QuestAcceptance accept(long owner, String quest) {
		long character = characters.lock(owner).id();
		var definition = definitions.definition(quest);
		var now = clock.now();

		// 先更新并保存所有旧尝试，失败/过期不会继续占据唯一运行位置。
		var history = instances.findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(character, quest);
		for (var row : history) {
			var state = decode(row);
			if (state.running() || state.status() == QuestStatus.PAUSED)
				commit(owner, engine.refresh(definition, state, now, host(owner, state)));
		}
		var values = instances.findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(character, quest)
			.stream()
			.map(this::decode)
			.toList();
		try {
			var result = engine.accept(definition, values, UUID.randomUUID().toString(), now,
					host(owner, values.isEmpty() ? null : values.getLast()));
			boolean fresh = values.stream().noneMatch(value -> value.id().equals(result.instance().id()));
			var applied = commit(owner, result);
			return new QuestAcceptance(applied.instance(), fresh, applied.rewards());
		}
		catch (IllegalStateException error) {
			throw new BusinessException(409, error.getMessage());
		}
	}

	/** 当前实例下已完成的步骤用于重复任务，不让永久剧情事实误判下一周期全部完成。 */
	public Set<String> done(long owner, String quest) {
		var current = latest(characterQueries.requireId(owner), quest);
		if (current == null)
			return completions.done(characterQueries.requireId(owner));
		return current.completed();
	}

	/** 查询最新实例，不执行奖励或迁移。读侧可显示其目标、轮次、分支及截止状态。 */
	public QuestInstance latest(long character, String quest) {
		var rows = instances.findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(character, quest);
		return rows.isEmpty() ? null : decode(rows.getLast());
	}

	/** 当前实例的开放步骤和即时交付资格由引擎统一计算，纯查询不执行自动奖励。 */
	public Map<String, QuestStepView> steps(long owner) {
		Map<String, QuestStepView> result = new HashMap<>();
		for (var row : instances
			.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(characterQueries.requireId(owner))) {
			var stored = decode(row);
			var definition = definitions.definition(stored.quest());
			var view = engine.inspect(definition, stored, clock.now(), host(owner, stored));
			for (var step : definition.steps()) {
				boolean selectable = engine.available(definition, view, step.id(), host(owner, view));
				boolean ready = selectable && engine.deliverable(definition, view, step.id(), host(owner, view));
				boolean terminal = !view.running() && view.status() != QuestStatus.PAUSED;
				var history = instances
					.findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(characterQueries.requireId(owner),
							stored.quest())
					.stream()
					.map(this::decode)
					.map(value -> engine.inspect(definition, value, clock.now(), host(owner, value)))
					.toList();
				boolean repeatable = engine.canStart(definition, history, clock.now());
				boolean entry = terminal && repeatable && step.requires().isEmpty()
						|| view.status() == QuestStatus.PAUSED && selectable;
				result.put(step.id(), new QuestStepView(view, view.running() && selectable || entry, view.running(),
						ready, !entry && view.completed().contains(step.id())));
			}
		}
		return result;
	}

	/**
	 * 交付使用实例和轮次，不接受奖励参数。已有步骤回执直接返回空效果； 完整响应由实例请求回执保存，核心仅防止资产再次执行。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public QuestApplied deliver(long owner, String step) {
		var source = catalog.step(step);
		if (source == null)
			throw new BusinessException(404, "任务步骤不存在");
		long character = characters.lock(owner).id();
		var definition = definitions.definition(source.quest());

		var instance = latest(character, source.quest());
		if (instance == null)
			throw new BusinessException(409, "请先接取任务");
		try {
			return commit(owner, engine.deliver(definition, instance, step, clock.now(), host(owner, instance)));
		}
		catch (IllegalStateException error) {
			throw new BusinessException(409, error.getMessage());
		}
	}

	/** 放弃后的状态投影修订号供客户端拒绝过期操作；进度是否保留由核心规则决定。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void abandon(long owner, String quest) {
		long character = characters.lock(owner).id();
		var definition = definitions.definition(quest);

		var current = latest(character, quest);
		if (current == null)
			throw new BusinessException(409, "任务尚未接取");
		try {
			commit(owner, engine.abandon(definition, current, clock.now()));
		}
		catch (IllegalStateException error) {
			throw new BusinessException(409, error.getMessage());
		}
	}

	/**
	 * 行为观察器在原事务内调用。事件时刻及编号由服务器生成，归属由事件携带真实角色； 同一行为逐项推进已接任务，奖励可能发布新事件，因此提交前先保存状态以防递归重复结算。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void record(long owner, QuestEvent event) {
		long character = characters.lock(owner).id();
		var now = clock.now();
		var rows = instances.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(character);
		for (var row : rows) {
			var state = decode(row);
			if (!state.running())
				continue;
			var definition = definitions.definition(state.quest());
			commit(owner, engine.record(definition, state, event, now, host(owner, state)));
		}
	}

	/** 实例读接口显示全部历史尝试；预览只更新时间和条件，不实际保存或发奖。 */
	@Transactional
	public List<QuestInstance> state(long owner) {
		long character = characterQueries.requireId(owner);
		return instances.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(character)
			.stream()
			.map(this::decode)
			.toList();
	}

	/** 玩家视图过滤事件与资产回执，只返回已开放目标、截止时刻及可选路线。 */
	@Transactional
	public List<QuestInstanceView> views(long owner) {
		List<QuestInstanceView> result = new ArrayList<>();
		for (var row : instances
			.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(characterQueries.requireId(owner))) {
			var stored = decode(row);
			var definition = definitions.definition(stored.quest());
			var host = host(owner, stored);
			var view = engine.inspect(definition, stored, clock.now(), host);
			List<QuestGoalView> goals = new ArrayList<>();
			List<QuestBranchView> branches = new ArrayList<>();
			Instant deadline = definition.timeout() == null ? null : definition.timeout().at(view.acceptedAt());
			for (var step : definition.steps()) {
				boolean opened = engine.available(definition, view, step.id(), host(owner, view))
						|| view.completed().contains(step.id());
				if (opened) {
					for (var objective : step.objectives()) {
						long value = objective.measure() == QuestMeasure.EVENT
								? view.progress().getOrDefault(step.id(), Map.of()).getOrDefault(objective.id(), 0L)
								: host.value(objective.measure(), objective.code());
						goals.add(new QuestGoalView(step.id(), catalog.step(step.id()).title(), objective.id(),
								objective.measure(), Math.min(objective.amount(), value), objective.amount()));
					}
					if (!view.completed().contains(step.id()) && step.timeout() != null
							&& view.openedAt().containsKey(step.id())) {
						Instant at = step.timeout().at(view.openedAt().get(step.id()));
						if (deadline == null || at.isBefore(deadline))
							deadline = at;
					}
				}
				if (view.running() && step.branch() != null && !view.choices().containsKey(step.branch().group())) {
					boolean eligible = step.requires().stream().allMatch(view.completed()::contains)
							&& step.activation().stream().allMatch(host::test);
					if (eligible) {
						var option = new QuestBranchView(step.branch().group(), step.branch().option(),
								catalog.step(step.id()).title());
						if (branches.stream()
							.noneMatch(prior -> prior.group().equals(option.group())
									&& prior.option().equals(option.option())))
							branches.add(option);
					}
				}
			}
			result.add(new QuestInstanceView(view.id(), view.quest(), view.period(), view.round(), view.status(),
					deadline, definition.repeat() == QuestRepeat.RING ? definition.ringSize() : 0, List.copyOf(goals),
					List.copyOf(branches), view.choices(), definition.timeout() != null
							|| definition.steps().stream().anyMatch(step -> step.timeout() != null)));
		}
		return List.copyOf(result);
	}

	/**
	 * 新客户端命令用请求号、实例及轮次拒绝重试误伤下一次委托。操作在角色锁内重放完整首次响应。 请求号不是权限凭据，仍按登录账号查实例归属；目标行为没有公开HTTP写接口。
	 */
	@Transactional
	public QuestApplied command(long owner, QuestInstanceRequest request) {
		if (request == null || request.requestId() == null || request.operation() == null || request.instance() == null)
			throw new BusinessException(400, "实例命令不完整");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "请求号必须为标准UUID");
		}
		long character = characters.requireOutsideDungeon(owner).id();
		String payload = json.writeValueAsString(request);
		var prior = requests.findByCharacterIdAndRequestId(character, request.requestId()).orElse(null);
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号不能用于不同实例命令");
			return json.readValue(prior.getResponse(), QuestApplied.class);
		}
		var row = instances.findByIdAndCharacterId(request.instance(), character)
			.orElseThrow(() -> new BusinessException(404, "任务实例不存在"));
		var state = decode(row);
		if (state.round() != request.round())
			throw new BusinessException(409, "任务轮次已变化，请刷新");
		var definition = definitions.definition(state.quest());
		QuestTransition transition;
		try {
			transition = switch (request.operation()) {
				case REFRESH -> engine.refresh(definition, state, clock.now(), host(owner, state));
				case DELIVER -> engine.deliver(definition, state, request.step(), clock.now(), host(owner, state));
				case CHOOSE -> engine.choose(definition, state, request.group(), request.option(), clock.now(),
						host(owner, state));
				case ABANDON -> engine.abandon(definition, state, clock.now());
			};
		}
		catch (IllegalStateException error) {
			throw new BusinessException(409, error.getMessage());
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, error.getMessage());
		}
		var committed = commit(owner, transition);
		// 交付对白来自当前正式内容，并随首次请求响应保存；重放不重算配置文本。
		var applied = new QuestApplied(committed.instance(), committed.rewards(),
				request.operation() == QuestOperation.DELIVER ? catalog.step(request.step()).dialogue() : "任务状态已更新");
		var receipt = new QuestRuntimeRequest();
		receipt.setCharacterId(character);
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setResponse(json.writeValueAsString(applied));
		requests.saveAndFlush(receipt);
		return applied;
	}

	/**
	 * 状态、资产和剧情投影共同提交。先保存实例防止奖励触发嵌套事件时再次领奖； 嵌套更新完成后必须重新读取最终实例，不能把旧副本覆盖回去。
	 */
	private QuestApplied commit(long owner, QuestTransition transition) {
		long character = characters.lock(owner).id();
		var state = transition.instance();
		var row = instances.findByIdAndCharacterId(state.id(), character).orElseGet(QuestRuntimeInstance::new);
		Set<String> previousKeys = row.getState() == null ? Set.of() : decode(row).receipts().keySet();
		var newlySettled = state.receipts().keySet().stream().filter(key -> !previousKeys.contains(key)).toList();
		if (row.getAttempt() == 0)
			row.setAttempt(instances.findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(character, state.quest())
				.stream()
				.mapToInt(QuestRuntimeInstance::getAttempt)
				.max()
				.orElse(0) + 1);
		row.setId(state.id());
		row.setCharacterId(character);
		row.setQuestCode(state.quest());
		row.setAcceptedAt(state.acceptedAt());
		row.setState(json.writeValueAsString(state));
		instances.saveAndFlush(row);
		var projection = participation.findByCharacterIdAndQuestCode(character, state.quest()).orElse(null);
		if (projection == null) {
			projection = QuestJournalState.begin(character, state.quest(), state.acceptedAt());
			projection.synchronize(state);
			participation.save(projection);
		}
		projection.synchronize(state);
		// 奖励会发布嵌套行为事件。先占用一次性回执，使嵌套观察器不能写入空奖励响应；
		// 外层效果全部成功后再填充最终说明，失败则占位记录与全部资产一起回滚。
		List<QuestReceipt> pendingReceipts = new ArrayList<>();
		if (rules.rule(state.quest()).policy().repeatPolicy() == QuestRepeatPolicy.ONCE) {
			for (String stepId : state.completed()) {
				if (completions.prior(character, stepId) != null)
					continue;
				var step = catalog.step(stepId);
				var receipt = new QuestReceipt();
				receipt.setCharacterId(character);
				receipt.setStep(stepId);
				receipt.setQuestCode(state.quest());
				receipt.setCompleted(step.completes());
				receipt.setCreatedAt(clock.now());
				receipt.setResponse(json.writeValueAsString(new QuestResult(stepId, step.dialogue(), List.of())));
				completions.saveAndFlush(receipt);
				pendingReceipts.add(receipt);
			}
		}
		List<String> rewards = new ArrayList<>();
		for (var grant : transition.grants())
			rewards.addAll(extensions.apply(owner, grant));
		for (var receipt : pendingReceipts)
			receipt.setResponse(json.writeValueAsString(new QuestResult(receipt.getStep(),
					catalog.step(receipt.getStep()).dialogue(), List.copyOf(rewards))));
		completions.flush();
		// 核心回执已包含所有结算步骤；永久事实只作为剧情门禁，不能用于重复任务本轮资格。
		Set<String> settledSteps = new HashSet<>(state.completed());
		for (String key : state.receipts().keySet())
			if (!key.equals("RING"))
				settledSteps.add(key.substring(key.indexOf(':') + 1));
		for (var step : settledSteps)
			if (!facts.codes(owner).contains("QUEST:" + step))
				factCommands.record(owner, "QUEST:" + step);
		// 同一实例轮次的每个步骤只通知一次；重放请求和嵌套行为不会再次发布结算通知。
		for (String key : newlySettled) {
			String title = key.equals("RING") ? rules.rule(state.quest()).title()
					: catalog.step(key.substring(key.indexOf(':') + 1)).title();
			notices.publishEvent(new PlayerNotice(owner, "QUEST:" + state.id() + ":" + key, "任务交付完成：" + title + "。"));
		}
		return new QuestApplied(decode(row), List.copyOf(rewards));
	}

	/** 当前状态下的查询适配器；接取补给使用实例级回执，而非永久接取事实。 */
	private QuestHost host(long owner, QuestInstance instance) {
		return new QuestHost() {
			public long value(QuestMeasure measure, String code) {
				if (measure == QuestMeasure.HISTORY)
					return facts.codes(owner).contains(code) ? 1 : 0;
				if (!code.startsWith("ITEM:"))
					throw new IllegalArgumentException("未知持有目标：" + code);
				String item = code.substring(5);
				return inventory.list(owner)
					.stream()
					.filter(value -> value.code().equals(item))
					.mapToLong(value -> value.quantity())
					.sum();
			}

			public boolean test(QuestCondition condition) {
				if (condition.kind().equals("ACCEPTED_STEP") && instance != null)
					return acceptance.accepted(characterQueries.requireId(owner), condition.reference())
						.stream()
						.anyMatch(receipt -> instance.id().equals(receipt.getRuntimeId())
								&& receipt.getRuntimeRound() == instance.round());
				return extensions.test(owner, condition);
			}
		};
	}

	/** 存储损坏时明确失败，不能以空进度替代以免重新发奖或突破周期次数。 */
	private QuestInstance decode(QuestRuntimeInstance row) {
		return json.readValue(row.getState(), QuestInstance.class);
	}

}
