package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import io.github.lishangbu.avalon.quest.QuestInstance;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 角色对整项任务的参与状态。步骤回执与目标记录独立保存，暂停不会删除它们。 所有状态变更必须在持有角色锁的命令事务中执行，不能由读侧自动接取。
 */
@Entity
@Table(name = "quest_participation",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "quest_code" }))
@Getter
@NoArgsConstructor
public class QuestParticipation {

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
	private QuestParticipationStatus status;

	/** 首次接取时间，重接不覆盖，不能作为当前有效窗口的起点。 */
	@Column(name = "first_accepted_at", nullable = false)
	private Instant firstAcceptedAt;

	/** 最近一次接取时间，用于区分放弃前后行为；事件仍须核验当时状态。 */
	@Column(name = "active_since", nullable = false)
	private Instant activeSince;

	/** 最新运行实例指针，仅用于旧任务日志的兼容投影；实例历史独立保存。 */
	@Column(name = "runtime_id", length = 36)
	private String runtimeId;

	/** 每次有效状态转换递增，供客户端拒绝过期操作。 */
	@Column(nullable = false)
	private long revision;

	public static QuestParticipation begin(long characterId, String questCode, Instant now) {
		if (characterId <= 0 || questCode == null || questCode.isBlank() || now == null)
			throw new IllegalArgumentException("任务参与信息不完整");
		var value = new QuestParticipation();
		value.characterId = characterId;
		value.questCode = questCode;
		value.status = QuestParticipationStatus.ACTIVE;
		value.firstAcceptedAt = now;
		value.activeSince = now;
		return value;
	}

	/** 重复接取不改变有效窗口；放弃后重接仅恢复状态，不重新发首次补给。 */
	public void resume(Instant now) {
		if (status == QuestParticipationStatus.COMPLETED)
			throw new BusinessException(409, "任务已完成，不能重复接取");
		if (status == QuestParticipationStatus.ACTIVE)
			return;
		if (now == null || now.isBefore(activeSince))
			throw new IllegalArgumentException("任务恢复时间无效");
		activeSince = now;
		status = QuestParticipationStatus.ACTIVE;
		revision++;
	}

	/** 是否允许放弃由内容规则决定；主线即使收到伪造请求也不能暂停。 */
	public void abandon(boolean abandonable) {
		if (!abandonable)
			throw new BusinessException(409, "此任务不能放弃");
		if (status == QuestParticipationStatus.COMPLETED)
			throw new BusinessException(409, "已完成任务不能放弃");
		if (status == QuestParticipationStatus.ABANDONED)
			return;
		status = QuestParticipationStatus.ABANDONED;
		revision++;
	}

	/** 仅在交付效果与回执均成功的事务中调用；目标达成不等于已领奖。 */
	public void complete() {
		if (status == QuestParticipationStatus.COMPLETED)
			return;
		if (status != QuestParticipationStatus.ACTIVE)
			throw new BusinessException(409, "请先重新接取任务");
		status = QuestParticipationStatus.COMPLETED;
		revision++;
	}

	/**
	 * 把引擎状态投影到旧日志接口。READY仍显示ACTIVE，目标达成不等于已经发奖。 状态或实例变化才递增修订号，事件计数变化不会使正常放弃请求失效。
	 */
	public void synchronize(QuestInstance instance) {
		QuestParticipationStatus next = switch (instance.status()) {
			case ACTIVE, READY -> QuestParticipationStatus.ACTIVE;
			case PAUSED, ABANDONED -> QuestParticipationStatus.ABANDONED;
			case COMPLETED -> QuestParticipationStatus.COMPLETED;
			case FAILED -> QuestParticipationStatus.FAILED;
			case EXPIRED -> QuestParticipationStatus.EXPIRED;
		};
		if (status != next || runtimeId != null && !runtimeId.equals(instance.id()))
			revision++;
		runtimeId = instance.id();
		status = next;
		activeSince = instance.activeSince();
	}

}
