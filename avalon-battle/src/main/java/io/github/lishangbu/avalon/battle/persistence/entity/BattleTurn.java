package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.UniqueConstraint;
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
@DatabaseForeignKey(name = "battle_turn_battle_id_fkey", columns = { "battle_id" }, referencedTable = "battle",
		referencedColumns = { "id" })
@Entity
@Table(name = "battle_turn", comment = "每个战局回合只保存一份公开结果。旧回合重发直接返回它，不执行本轮新指令。",
		uniqueConstraints = {
				@UniqueConstraint(name = "battle_turn_battle_id_round_key", columnNames = { "battle_id", "round" }),
				@UniqueConstraint(name = "uq_battle_turn_request", columnNames = { "battle_id", "request_id" }) })
@Getter
@Setter
public class BattleTurn {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属战局主键；与独立副本进度编号区分。 */
	@Column(name = "battle_id", nullable = false, comment = "所属战局主键；与独立副本进度编号区分。")
	private Long battleId;

	/** 战局回合序号；同一战局内按回合保存，重试不得再次执行。 */
	@Column(name = "round", nullable = false, comment = "战局回合序号；同一战局内按回合保存，重试不得再次执行。")
	private int round;

	/** 客户端UUID请求号，同一战局中的重试保持不变。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "完整回合命令的UUID请求号，重试保持不变")
	private String requestId;

	/** 完整请求的稳定JSON文本，用于同号异参拒绝，绝非战斗日志。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "已确认的完整请求载荷，用于同号异参拒绝，不保存战斗日志")
	private String payload;

	/** 该回合公开战斗结果的 JSON 文本；旧回合重发返回原结果，不重算战斗。 */
	@Column(name = "response", nullable = false, columnDefinition = "text",
			comment = "该回合公开战斗结果的 JSON 文本；旧回合重发返回原结果，不重算战斗。")
	private String response;

}
