package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 可持久化的不可变任务实例。每次重试、每个新周期和每次新开委托环均使用新id； 环内各轮复用实例，但进度和步骤完成记录每轮清空，奖励回执一直保留。
 * 不存定义版本或配置快照，receipts仅保存已经结算的效果，禁止重新执行它们。
 */
public record QuestInstance(String id, String quest, String period, Instant acceptedAt, Instant activeSince,
		QuestStatus status, int round, Map<String, Map<String, Long>> progress, Set<String> completed,
		Map<String, String> choices, Map<String, Instant> openedAt, Set<String> events,
		Map<String, List<QuestEffect>> receipts) {

	/** 深拷贝所有嵌套集合，防止宿主或调用者绕过状态机改写已结算进度。 */
	public QuestInstance {
		if (id == null || id.isBlank() || quest == null || quest.isBlank() || period == null || acceptedAt == null
				|| activeSince == null || status == null || round <= 0)
			throw new IllegalArgumentException("任务实例信息不完整");
		progress = progress.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
		completed = Set.copyOf(completed);
		choices = Map.copyOf(choices);
		openedAt = Map.copyOf(openedAt);
		events = Set.copyOf(events);
		receipts = receipts.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
	}

	/** 只有活动或已达成待交付实例占据任务的唯一运行位置；PAUSED可在合法周期内恢复。 */
	public boolean running() {
		return status == QuestStatus.ACTIVE || status == QuestStatus.READY;
	}
}
