package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestBranchMode;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 与游戏无关的任务定义。稳定编号标识内容，运行时使用当前定义。 条件和效果只声明扩展类型及参数，核心不认识角色、背包、地图或具体奖励服务。
 * 构造时执行结构校验，使启动加载与离线校验得到相同结果。
 */
public record QuestDefinition(String id, boolean main, boolean abandonable, QuestRepeat repeat, String timezone,
		int limit, int ringSize, QuestTimeout timeout, boolean retryable, List<QuestStep> steps,
		List<QuestEffect> ringRewards) {

	/** 不同任务可以复用步骤名，持久化及奖励来源必须再包含实例编号和环内轮次。 */
	public QuestDefinition {
		text(id, "任务编号");
		timezone = timezone == null ? "Asia/Hong_Kong" : timezone;
		ZoneId.of(timezone);
		repeat = repeat == null ? QuestRepeat.ONCE : repeat;
		steps = steps == null ? List.of() : List.copyOf(steps);
		ringRewards = ringRewards == null ? List.of() : List.copyOf(ringRewards);
		if (steps.isEmpty() || limit <= 0 || main && abandonable || repeat == QuestRepeat.RING && ringSize <= 0)
			throw new IllegalArgumentException("任务步骤、次数限制、环长度或主线放弃规则无效：" + id);
		if (repeat != QuestRepeat.RING && !ringRewards.isEmpty())
			throw new IllegalArgumentException("只有环任务可以配置整环奖励：" + id);
		Set<String> ids = unique(steps.stream().map(QuestStep::id).toList(), "步骤编号");
		Set<String> reached = new HashSet<>();
		while (true) {
			int before = reached.size();
			for (QuestStep step : steps) {
				if (!ids.containsAll(step.requires()))
					throw new IllegalArgumentException("未知前置：" + step.id());
				if (reached.containsAll(step.requires()))
					reached.add(step.id());
			}
			if (before == reached.size())
				break;
		}
		if (!reached.equals(ids))
			throw new IllegalArgumentException("任务步骤依赖存在环：" + id);
		// 不允许一条路线依赖另一条互斥路线，否则跳过逻辑会让该路线无条件越过真实前置。
		for (QuestStep step : steps) {
			if (step.branch() == null)
				continue;
			for (String required : step.requires()) {
				QuestBranch parent = steps.stream()
					.filter(s -> s.id().equals(required))
					.findFirst()
					.orElseThrow()
					.branch();
				if (parent != null && parent.group().equals(step.branch().group()) && !parent.equals(step.branch()))
					throw new IllegalArgumentException("互斥分支不能相互依赖：" + step.id());
			}
		}
		var branchModes = new HashMap<String, QuestBranchMode>();
		for (QuestStep step : steps) {
			if (step.branch() == null)
				continue;
			QuestBranchMode previous = branchModes.putIfAbsent(step.branch().group(), step.branch().mode());
			if (previous != null && previous != step.branch().mode())
				throw new IllegalArgumentException("同一分支组不能混用互斥与可切换语义：" + step.branch().group());
		}
	}

	/** 按稳定编号查找步骤；未知编号是配置或调用错误，不能默默降级为已完成。 */
	public QuestStep step(String stepId) {
		return steps.stream()
			.filter(step -> step.id().equals(stepId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("未知步骤：" + stepId));
	}

	/** 字符串空值和重复编号在加载阶段直接拒绝，不依赖数据库兜底。 */
	private static Set<String> unique(List<String> values, String label) {
		Set<String> result = new HashSet<>();
		for (String value : values) {
			text(value, label);
			if (!result.add(value))
				throw new IllegalArgumentException(label + "重复：" + value);
		}
		return result;
	}

	private static void text(String value, String label) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(label + "不能为空");
	}
}
