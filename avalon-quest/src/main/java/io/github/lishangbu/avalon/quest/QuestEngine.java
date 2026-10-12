package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestGrant;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static io.github.lishangbu.avalon.quest.model.QuestStatus.*;

/**
 * 纯任务状态机：所有时间均由调用者提供，所有宿主状态均通过QuestHost读取。 对相同定义、实例、时间和查询结果得到相同结果；没有数据库、线程、静态时钟或游戏服务依赖。
 * 写入、并发锁和事务由宿主负责，Transition中的效果只有持久化事务成功才算已结算。
 */
public final class QuestEngine {

	/**
	 * 接取或恢复实例。history必须包含该主体此任务的完整次数记录，且宿主应先刷新并持久化过期状态。
	 * 同周期放弃恢复原实例，限时失败和放弃后的环任务则创建新实例；newId由宿主生成。
	 */
	public QuestTransition accept(QuestDefinition definition, List<QuestInstance> history, String newId, Instant now,
			QuestHost host) {
		var existing = history.stream().filter(value -> value.quest().equals(definition.id())).toList();
		for (var instance : existing) {
			if (instance.running()) {
				var refreshed = refresh(definition, instance, now, host);
				// 过期或失败的旧实例不占据运行位置，接取可在本次命令中直接创建下一份实例。
				if (refreshed.instance().running() || !refreshed.grants().isEmpty())
					return refreshed;
			}
			if (instance.status() == PAUSED && instance.period().equals(period(definition, now))) {
				Mutable state = new Mutable(instance);
				state.status = ACTIVE;
				state.activeSince = now;
				return advance(definition, state, now, host);
			}
		}
		String period = period(definition, now);
		requireNewAttempt(definition, existing, period);
		if (existing.stream().anyMatch(value -> value.id().equals(newId)))
			throw new IllegalArgumentException("新实例编号已经使用");
		var instance = new QuestInstance(newId, definition.id(), period, now, now, ACTIVE, 1, Map.of(), Set.of(),
				Map.of(), Map.of(), Set.of(), Map.of());
		return advance(definition, new Mutable(instance), now, host);
	}

	/**
	 * 判断是否可新开尝试，供只读入口显示次数资格。调用者传入已按当前时间预览过期状态的历史。 已有运行实例只允许继续或交付，暂停实例通过恢复入口处理，不在此处新开。
	 */
	public boolean canStart(QuestDefinition definition, List<QuestInstance> history, Instant now) {
		var existing = history.stream().filter(value -> value.quest().equals(definition.id())).toList();
		if (existing.stream().anyMatch(QuestInstance::running))
			return false;
		try {
			requireNewAttempt(definition, existing, period(definition, now));
			return true;
		}
		catch (IllegalStateException error) {
			return false;
		}
	}

	/** 完成上限与开启上限采用同一检查，查询和真实接取不能出现两套周期次数算法。 */
	private void requireNewAttempt(QuestDefinition definition, List<QuestInstance> existing, String period) {
		if (definition.repeat() == QuestRepeat.ONCE && existing.stream().anyMatch(value -> value.status() == COMPLETED))
			throw new IllegalStateException("一次性任务已经完成");
		if (!definition.retryable() && existing.stream()
			// 周期任务下一周期是新的参加机会；retryable只限制同周期失败后的重试。
			.filter(value -> definition.repeat() == QuestRepeat.ONCE || value.period().equals(period))
			.anyMatch(value -> value.status() == FAILED || value.status() == ABANDONED && timed(definition)))
			throw new IllegalStateException("此任务不允许重试");
		long used = existing.stream()
			.filter(value -> value.period().equals(period))
			.filter(value -> definition.repeat() == QuestRepeat.RING || value.status() == COMPLETED)
			.count();
		if (definition.repeat() != QuestRepeat.ONCE && used >= definition.limit())
			throw new IllegalStateException("本周期任务次数已用完");
	}

	/**
	 * 刷新截止时间、步骤开放时间、即时持有条件和自动结算。读接口可使用返回状态做预览， 但只要出现grants就必须经过正式写事务执行，绝不能在纯查询中发奖。
	 */
	public QuestTransition refresh(QuestDefinition definition, QuestInstance instance, Instant now, QuestHost host) {
		check(definition, instance, now);
		return advance(definition, new Mutable(instance), now, host);
	}

	/**
	 * 只读预览仅推导过期、失败和达成状态，不执行自动步骤，不产生或保存奖励回执。 PAUSED保留开放步骤供恢复入口显示，但不累计事件或允许交付。
	 */
	public QuestInstance inspect(QuestDefinition definition, QuestInstance instance, Instant now, QuestHost host) {
		check(definition, instance, now);
		Mutable state = new Mutable(instance);
		expire(definition, state, now);
		if (state.running()) {
			boolean ready = definition.steps()
				.stream()
				.filter(step -> !excluded(state, step))
				.allMatch(step -> state.completed.contains(step.id())
						|| available(definition, state, step, host) && objectivesMet(state, step, host));
			state.status = ready ? READY : ACTIVE;
			// 最新目标数量或当前持有量可能让READY失效，此时必须立即重新检查周期和截止时间。
			expire(definition, state, now);
		}
		return state.result(List.of()).instance();
	}

	/** 公开同一套依赖和分支判定，读侧不需要重新实现引擎规则。 */
	public boolean available(QuestDefinition definition, QuestInstance instance, String step, QuestHost host) {
		return available(definition, new Mutable(instance), definition.step(step), host);
	}

	/** 查询即时交付资格，写命令必须在角色锁内重新验证，不能信任客户端的ready值。 */
	public boolean deliverable(QuestDefinition definition, QuestInstance instance, String step, QuestHost host) {
		var state = new Mutable(instance);
		return state.running() && available(definition, state, definition.step(step), host)
				&& satisfied(state, definition.step(step), host);
	}

	/**
	 * 接收真实行为。事件必须带稳定唯一编号、正增量和发生时间；重放不再累计。 后续步骤不能提前吃到本次行为：匹配步骤列表在任何自动结算之前固定。
	 */
	public QuestTransition record(QuestDefinition definition, QuestInstance instance, QuestEvent event, Instant now,
			QuestHost host) {
		check(definition, instance, now);
		if (event.at().isAfter(now))
			throw new IllegalArgumentException("行为时间不能晚于处理时间");
		Mutable state = new Mutable(instance);
		List<QuestGrant> grants = new ArrayList<>();
		expire(definition, state, now);
		if (!state.running() || event.at().isBefore(state.activeSince) || state.events.contains(event.id()))
			return state.result(grants);
		open(definition, state, now, host);
		expire(definition, state, now);
		if (!state.running())
			return state.result(grants);
		// 同一行为仅影响当时已经开放的步骤。计数截断到目标数量，避免整型溢出和无界增长。
		for (QuestStep step : definition.steps()) {
			if (!available(definition, state, step, host))
				continue;
			for (QuestObjective objective : step.objectives()) {
				if (objective.measure() != QuestMeasure.EVENT || !objective.code().equals(event.code()))
					continue;
				var values = state.progress.computeIfAbsent(step.id(), key -> new HashMap<>());
				long prior = values.getOrDefault(objective.id(), 0L);
				values.put(objective.id(), prior + Math.min(event.amount(), Math.max(0, objective.amount() - prior)));
			}
		}
		boolean relevant = definition.steps()
			.stream()
			.flatMap(step -> step.objectives().stream())
			.anyMatch(objective -> objective.measure() == QuestMeasure.EVENT && objective.code().equals(event.code()));
		if (relevant)
			state.events.add(event.id());
		return advance(definition, state, now, host);
	}

	/** 手动交付仅结算当前轮的指定步骤；重复交付返回空效果，宿主可重放已存首次响应。 */
	public QuestTransition deliver(QuestDefinition definition, QuestInstance instance, String stepId, Instant now,
			QuestHost host) {
		check(definition, instance, now);
		Mutable state = new Mutable(instance);
		List<QuestGrant> grants = new ArrayList<>();
		QuestStep step = definition.step(stepId);
		String receipt = state.round + ":" + stepId;
		if (state.receipts.containsKey(receipt))
			return state.result(grants);
		expire(definition, state, now);
		open(definition, state, now, host);
		expire(definition, state, now);
		if (!state.running() || !available(definition, state, step, host) || !satisfied(state, step, host))
			throw new IllegalStateException("任务未接取、已超时、分支未选择或交付要求尚未满足");
		settle(state, step, grants);
		finishRound(definition, state, grants);
		return advance(definition, state, now, host, grants);
	}

	/**
	 * 确认互斥分支。只有该组存在前置已完成的入口时可选择；确认后同轮不可反悔。 公共汇合步骤不会因为未选路线而永远阻塞。
	 */
	public QuestTransition choose(QuestDefinition definition, QuestInstance instance, String group, String option,
			Instant now, QuestHost host) {
		check(definition, instance, now);
		Mutable state = new Mutable(instance);
		expire(definition, state, now);
		if (!state.running())
			throw new IllegalStateException("当前实例不能选择分支");
		String prior = state.choices.get(group);
		if (prior != null && !prior.equals(option))
			throw new IllegalStateException("已确认的分支不能更改");
		boolean eligible = definition.steps()
			.stream()
			.anyMatch(step -> step.branch() != null && step.branch().group().equals(group)
					&& step.branch().option().equals(option) && dependencies(definition, state, step)
					&& step.activation().stream().allMatch(host::test));
		if (!eligible)
			throw new IllegalStateException("分支不存在或尚未开放");
		state.choices.put(group, option);
		return advance(definition, state, now, host);
	}

	/**
	 * 普通支线和同周期重复任务暂停并保留进度；委托环或任何带计时步骤的任务终止本次实例。 完成回执和已发奖励不删除，新实例重新开始时自然使用新的奖励来源。
	 */
	public QuestTransition abandon(QuestDefinition definition, QuestInstance instance, Instant now) {
		check(definition, instance, now);
		if (!definition.abandonable())
			throw new IllegalStateException("此任务不能放弃");
		Mutable state = new Mutable(instance);
		expire(definition, state, now);
		if (state.status == PAUSED || state.status == ABANDONED)
			return state.result(List.of());
		if (!state.running())
			throw new IllegalStateException("已结束的实例不能放弃");
		boolean timed = timed(definition);
		state.status = definition.repeat() == QuestRepeat.RING || timed ? ABANDONED : PAUSED;
		return state.result(List.of());
	}

	/** DAILY和RING按当地日期计数；WEEKLY从当地周一开始；ONCE使用永久周期。 */
	public String period(QuestDefinition definition, Instant now) {
		var date = now.atZone(ZoneId.of(definition.timezone())).toLocalDate();
		return switch (definition.repeat()) {
			case ONCE -> "ONCE";
			case DAILY, RING -> date.toString();
			case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
		};
	}

	/** 变更配置时校验已使用结构：禁止删除步骤、改依赖、分支、目标身份或生命周期策略。 */
	public void compatible(QuestDefinition before, QuestDefinition after) {
		if (!before.id().equals(after.id()) || before.repeat() != after.repeat()
				|| !before.timezone().equals(after.timezone()) || before.ringSize() != after.ringSize()
				|| !Objects.equals(before.timeout(), after.timeout()))
			throw new IllegalArgumentException("任务生命周期修改不兼容，请使用新任务编号：" + before.id());
		if (before.steps().size() != after.steps().size())
			throw new IllegalArgumentException("任务步骤增删不兼容，请使用新任务编号：" + before.id());
		for (QuestStep old : before.steps()) {
			QuestStep next = after.step(old.id());
			if (!old.requires().equals(next.requires()) || !Objects.equals(old.branch(), next.branch())
					|| !Objects.equals(old.timeout(), next.timeout()) || old.delivery() != next.delivery()
					|| !old.activation().equals(next.activation())
					|| !old.objectives()
						.stream()
						.map(o -> List.of(o.id(), o.measure().name(), o.code()))
						.toList()
						.equals(next.objectives()
							.stream()
							.map(o -> List.of(o.id(), o.measure().name(), o.code()))
							.toList()))
				throw new IllegalArgumentException("任务流程修改不兼容，请使用新任务编号：" + before.id());
		}
	}

	/** 自动步骤按依赖顺序推进；每轮最多结算步骤数，整环最多ringSize轮，避免无目标环无限循环。 */
	private QuestTransition advance(QuestDefinition definition, Mutable state, Instant now, QuestHost host) {
		return advance(definition, state, now, host, new ArrayList<>());
	}

	private QuestTransition advance(QuestDefinition definition, Mutable state, Instant now, QuestHost host,
			List<QuestGrant> grants) {
		expire(definition, state, now);
		if (!state.running())
			return state.result(grants);
		int budget = definition.steps().size() * (definition.repeat() == QuestRepeat.RING ? definition.ringSize() : 1);
		while (budget-- > 0 && state.running()) {
			open(definition, state, now, host);
			expire(definition, state, now);
			if (!state.running())
				break;
			QuestStep next = definition.steps()
				.stream()
				.filter(step -> step.delivery() == QuestDelivery.AUTO && available(definition, state, step, host)
						&& satisfied(state, step, host))
				.findFirst()
				.orElse(null);
			if (next == null)
				break;
			settle(state, next, grants);
			finishRound(definition, state, grants);
		}
		if (state.running()) {
			open(definition, state, now, host);
			// READY只判断目标，不要求交付地点、人物和消耗条件，故离开NPC不会丢失跨周期领奖资格。
			boolean ready = definition.steps()
				.stream()
				.filter(step -> !excluded(state, step))
				.allMatch(step -> state.completed.contains(step.id())
						|| available(definition, state, step, host) && objectivesMet(state, step, host));
			state.status = ready ? READY : ACTIVE;
			// 最新目标数量或当前持有量可能让READY失效，此时必须立即重新检查周期和截止时间。
			expire(definition, state, now);
		}
		return state.result(grants);
	}

	/** 不在未选择路线中开放步骤，也不把上轮的开放时间带到下一轮。 */
	private void open(QuestDefinition definition, Mutable state, Instant now, QuestHost host) {
		if (!state.running())
			return;
		for (QuestStep step : definition.steps())
			if (available(definition, state, step, host))
				state.openedAt.putIfAbsent(step.id(), now);
	}

	/** READY实例在周期切换后仍可交付；进行中或暂停实例跨周期过期，环任务仅限制开启日而不强制跨日清空。 */
	private void expire(QuestDefinition definition, Mutable state, Instant now) {
		if (state.status != ACTIVE && state.status != READY && state.status != PAUSED)
			return;
		if (state.status != READY
				&& (definition.repeat() == QuestRepeat.DAILY || definition.repeat() == QuestRepeat.WEEKLY)
				&& !state.period.equals(period(definition, now))) {
			state.status = EXPIRED;
			return;
		}
		// 周期切换保留READY领奖资格，但显式限时约束仍然有效，不能无限延后限时交付。
		if (definition.timeout() != null && !now.isBefore(definition.timeout().at(state.acceptedAt)))
			state.status = FAILED;
		for (QuestStep step : definition.steps()) {
			Instant opened = state.openedAt.get(step.id());
			if (step.timeout() != null && opened != null && !state.completed.contains(step.id())
					&& !excluded(state, step) && !now.isBefore(step.timeout().at(opened)))
				state.status = FAILED;
		}
	}

	/** 选择后未选路线视为跳过；尚未选择时不能把任何路线视为已完成。 */
	private boolean excluded(Mutable state, QuestStep step) {
		return step.branch() != null && state.choices.containsKey(step.branch().group())
				&& !state.choices.get(step.branch().group()).equals(step.branch().option());
	}

	/** 汇合节点只等待已完成或明确跳过的路线；尚未选择的路线不会被视为跳过。 */
	private boolean dependencies(QuestDefinition definition, Mutable state, QuestStep step) {
		return step.requires()
			.stream()
			.allMatch(id -> state.completed.contains(id) || excluded(state, definition.step(id)));
	}

	/** 开放资格同时核验本轮未完成、所选路线、步骤依赖和宿主前置，事件与查询共用。 */
	private boolean available(QuestDefinition definition, Mutable state, QuestStep step, QuestHost host) {
		return !state.completed.contains(step.id()) && !excluded(state, step)
				&& (step.branch() == null || step.branch().option().equals(state.choices.get(step.branch().group())))
				&& dependencies(definition, state, step) && step.activation().stream().allMatch(host::test);
	}

	/** 事件目标读取实例累计值，其余目标读取宿主实时状态，不把永久知识写成新行为。 */
	private boolean objectivesMet(Mutable state, QuestStep step, QuestHost host) {
		return step.objectives().stream().allMatch(objective -> {
			long value = objective.measure() == QuestMeasure.EVENT
					? state.progress.getOrDefault(step.id(), Map.of()).getOrDefault(objective.id(), 0L)
					: host.value(objective.measure(), objective.code());
			return value >= objective.amount();
		});
	}

	/** 交付资格比目标达成多即时条件；人物、地点和消耗不足时保留进度但拒绝领奖。 */
	private boolean satisfied(Mutable state, QuestStep step, QuestHost host) {
		return objectivesMet(state, step, host) && step.conditions().stream().allMatch(host::test);
	}

	/** 同轮步骤回执先记录再产生效果，宿主整体回滚可保证任何失败都不消耗领奖资格。 */
	private void settle(Mutable state, QuestStep step, List<QuestGrant> grants) {
		String key = state.round + ":" + step.id();
		state.receipts.put(key, step.effects());
		state.completed.add(step.id());
		append(state.id + ":" + key, step.effects(), grants);
	}

	/** 每次委托独立结算，第十次完成后额外结算整环奖励；跨轮不保留计数或分支。 */
	private void finishRound(QuestDefinition definition, Mutable state, List<QuestGrant> grants) {
		if (!definition.steps()
			.stream()
			.filter(step -> !excluded(state, step))
			.allMatch(step -> state.completed.contains(step.id())))
			return;
		if (definition.repeat() == QuestRepeat.RING && state.round < definition.ringSize()) {
			state.round++;
			state.progress.clear();
			state.completed.clear();
			state.choices.clear();
			state.openedAt.clear();
			state.status = ACTIVE;
		}
		else {
			if (definition.repeat() == QuestRepeat.RING) {
				state.receipts.put("RING", definition.ringRewards());
				append(state.id + ":RING", definition.ringRewards(), grants);
			}
			state.status = COMPLETED;
		}
	}

	/** 保持配置效果顺序，并给每个效果分配稳定实例来源，供宿主资产账本防重。 */
	private void append(String source, List<QuestEffect> effects, List<QuestGrant> grants) {
		for (int i = 0; i < effects.size(); i++)
			grants.add(new QuestGrant(source + ":" + i, effects.get(i)));
	}

	/** 任一实例或步骤限时都使放弃成为终止行为，不能通过暂停冻结秒表。 */
	private boolean timed(QuestDefinition definition) {
		return definition.timeout() != null || definition.steps().stream().anyMatch(step -> step.timeout() != null);
	}

	/** 禁止回拨处理时间，否则暂停后的旧行为或重试可能污染当前有效窗口。 */
	private void check(QuestDefinition definition, QuestInstance instance, Instant now) {
		if (!definition.id().equals(instance.quest()) || now.isBefore(instance.activeSince()))
			throw new IllegalArgumentException("任务定义不匹配或处理时间倒退");
	}

	/** 单次运算内部可变副本，永不向调用者暴露；构造结果再次深拷贝为不可变实例。 */
	private static final class Mutable {

		private final String id;

		private final String quest;

		private final String period;

		private final Instant acceptedAt;

		private Instant activeSince;

		private QuestStatus status;

		private int round;

		private final Map<String, Map<String, Long>> progress = new HashMap<>();

		private final Set<String> completed;

		private final Map<String, String> choices;

		private final Map<String, Instant> openedAt;

		private final Set<String> events;

		private final Map<String, List<QuestEffect>> receipts;

		/** 深拷贝嵌套进度，单次计算不能修改调用者持有或已持久化的不可变实例。 */
		private Mutable(QuestInstance instance) {
			id = instance.id();
			quest = instance.quest();
			period = instance.period();
			acceptedAt = instance.acceptedAt();
			activeSince = instance.activeSince();
			status = instance.status();
			round = instance.round();
			instance.progress().forEach((key, value) -> progress.put(key, new HashMap<>(value)));
			completed = new HashSet<>(instance.completed());
			choices = new HashMap<>(instance.choices());
			openedAt = new HashMap<>(instance.openedAt());
			events = new HashSet<>(instance.events());
			receipts = new HashMap<>(instance.receipts());
		}

		/** 只有活动与目标达成待交付状态可继续运算，暂停和终态拒绝新行为。 */
		private boolean running() {
			return status == ACTIVE || status == READY;
		}

		/** 封装不可变的新状态和待执行效果；返回本身不代表宿主事务已经提交。 */
		private QuestTransition result(List<QuestGrant> grants) {
			return new QuestTransition(new QuestInstance(id, quest, period, acceptedAt, activeSince, status, round,
					progress, completed, choices, openedAt, events, receipts), grants);
		}

	}

}
