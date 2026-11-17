package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import io.github.lishangbu.avalon.quest.QuestInstance;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 任务日志的持久化投影，只从实例同步状态，不独立推进任务生命周期。 所有状态变更必须在持有角色锁的命令事务中执行，不能由读侧自动接取。
 */
@DatabaseForeignKey(name = "fk_quest_participation_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "quest_journal_state", comment = "角色整项任务参与状态，独立于步骤回执与永久事实",
		uniqueConstraints = { @UniqueConstraint(name = "uq_quest_participation_character_code",
				columnNames = { "character_id", "quest_code" }) })
@Getter
@NoArgsConstructor
public class QuestJournalState {

	/** CosId生成的内部记录主键；实体不直接作为API响应。 */
	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "CosId雪花主键")
	private Long id;

	/** 登录身份解析出的角色，不接受客户端指定归属。 */
	@Column(name = "character_id", nullable = false, comment = "任务归属角色")
	private Long characterId;

	/** 整项任务稳定编号，与步骤编号分开。 */
	@Column(name = "quest_code", nullable = false, length = 255, comment = "整项任务编号，与步骤编号分离")
	private String questCode;

	/** ACTIVE可累计目标，ABANDONED暂停，COMPLETED不可再次接取。 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16, comment = "参与状态ACTIVE、ABANDONED或COMPLETED")
	private QuestJournalStateStatus status;

	/** 首次接取时间，重接不覆盖，不能作为当前有效窗口的起点。 */
	@Column(name = "first_accepted_at", nullable = false, comment = "首次接取时间，重接保留")
	private Instant firstAcceptedAt;

	/** 最近一次接取时间，用于区分放弃前后行为；事件仍须核验当时状态。 */
	@Column(name = "active_since", nullable = false, comment = "最近恢复接取时间，仅激活状态记录新目标")
	private Instant activeSince;

	/** 最新运行实例指针，用于任务日志的展示投影；实例历史独立保存。 */
	@Column(name = "runtime_id", nullable = false, length = 36, comment = "当前任务实例编号")
	private String runtimeId;

	/** 每次有效状态转换递增，供客户端拒绝过期操作。 */
	@Column(name = "revision", nullable = false, comment = "状态转换版本，用于拒绝过期写操作")
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
