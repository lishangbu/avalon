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

/**
 * 每个战局回合只保存一份公开结果。旧回合重发直接返回它，不执行本轮新指令。
 */
@Entity
@Table(name = "battle_turn")
@Getter
@Setter
public class BattleTurn {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属战局主键；与独立副本进度编号区分。 */
	@Column(name = "battle_id", nullable = false)
	private Long battleId;

	/** 战局回合序号；同一战局内按回合保存，重试不得再次执行。 */
	private int round;

	/** 客户端UUID请求号，同一战局中的重试保持不变。 */
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 完整请求的稳定JSON文本，用于同号异参拒绝，绝非战斗日志。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 该回合公开战斗结果的 JSON 文本；旧回合重发返回原结果，不重算战斗。 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
