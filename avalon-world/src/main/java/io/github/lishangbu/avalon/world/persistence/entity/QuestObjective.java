package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/** 有效接取期间完成的步骤目标。放弃保留记录，永久事实不能反向补写此表。 */
@Entity
@Table(name = "quest_objective",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "step", "fact" }))
@Getter
@NoArgsConstructor
public class QuestObjective {

	/** 内部雪花主键，不作为客户端指定的目标。 */
	@Id
	@CosIdGenerated
	private Long id;

	/** 真实行为的角色归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 任务步骤编号；同一行为可以分别推进多个符合资格的步骤。 */
	@Column(nullable = false)
	private String step;

	/** 正式内容声明的目标事实编号。 */
	@Column(nullable = false)
	private String fact;

	/** 首次有效完成时间，重复行为和重接不覆盖。 */
	@Column(name = "completed_at", nullable = false)
	private Instant completedAt;

	public QuestObjective(long characterId, String step, String fact) {
		this.characterId = characterId;
		this.step = step;
		this.fact = fact;
		this.completedAt = Instant.now();
	}

}
