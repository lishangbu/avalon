package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 每人每轮的不可变提交凭证，保存完整输入以区分安全重试与异参覆盖。 */
@Entity
@Table(name = "dungeon_submission")
@Getter
@Setter
public class DungeonSubmission {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属战局主键；与独立副本进度编号区分。 */
	@Column(name = "battle_id", nullable = false)
	private Long battleId;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 战局回合序号；同一战局内按回合保存，重试不得再次执行。 */
	private int round;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

}
