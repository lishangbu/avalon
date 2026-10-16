package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import io.github.lishangbu.avalon.quest.QuestInstance;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 任务日志的持久化投影，只从实例同步状态，不独立推进任务生命周期。 所有状态变更必须在持有角色锁的命令事务中执行，不能由读侧自动接取。
 */
@Entity
@Table(name = "quest_journal_state",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "quest_code" }))
@Getter
@NoArgsConstructor
public class QuestJournalState {

	/** CosId生成的内部记录主键；实体不直接作为API响应。 */
	@Id
	@CosIdGenerated
	private Long id;

	/** 登录身份解析出的角色，不接受客户端指定归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 整项任务稳定编号，与步骤编号分开。 */
	@Column(name = "quest_code", nullable = false)
	private String questCode;

	/** ACTIVE可累计目标，ABANDONED暂停，COMPLETED不可再次接取。 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private QuestJournalStateStatus status;

	/** 首次接取时间，重接不覆盖，不能作为当前有效窗口的起点。 */
	@Column(name = "first_accepted_at", nullable = false)
	private Instant firstAcceptedAt;

	/** 最近一次接取时间，用于区分放弃前后行为；事件仍须核验当时状态。 */
	@Column(name = "active_since", nullable = false)
	private Instant activeSince;

	/** 最新运行实例指针，用于任务日志的展示投影；实例历史独立保存。 */
	@Column(name = "runtime_id", length = 36, nullable = false)
	private String runtimeId;

	/** 每次有效状态转换递增，供客户端拒绝过期操作。 */
	@Column(nullable = false)
	private long revision;

	public static QuestJournalState begin(long characterId, String questCode, Instant now) {
		if (characterId <= 0 || questCode == null || questCode.isBlank() || now == null)
			throw new IllegalArgumentException("任务参与信息不完整");
		var value = new QuestJournalState();
		value.characterId = characterId;
		value.questCode = questCode;
		value.status = QuestJournalStateStatus.ACTIVE;
		value.firstAcceptedAt = now;
		value.activeSince = now;
		return value;
	}

	/**
	 * 把引擎状态投影到任务日志。READY仍显示ACTIVE，目标达成不等于已经发奖。 状态或实例变化才递增修订号，事件计数变化不会使正常放弃请求失效。
	 */
	public void synchronize(QuestInstance instance) {
		QuestJournalStateStatus next = switch (instance.status()) {
			case ACTIVE, READY -> QuestJournalStateStatus.ACTIVE;
			case PAUSED, ABANDONED -> QuestJournalStateStatus.ABANDONED;
			case COMPLETED -> QuestJournalStateStatus.COMPLETED;
			case FAILED -> QuestJournalStateStatus.FAILED;
			case EXPIRED -> QuestJournalStateStatus.EXPIRED;
		};
		if (status != next || runtimeId != null && !runtimeId.equals(instance.id()))
			revision++;
		runtimeId = instance.id();
		status = next;
		activeSince = instance.activeSince();
	}

}
