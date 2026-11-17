package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.CheckConstraint;
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

/** 每人每轮的不可变提交凭证，保存完整输入以区分安全重试与异参覆盖。 */
@DatabaseForeignKey(name = "dungeon_submission_battle_id_character_id_fkey", columns = { "battle_id", "character_id" },
		referencedTable = "dungeon_battle_member", referencedColumns = { "battle_id", "character_id" })
@Entity
@Table(name = "dungeon_submission", comment = "每人每轮的不可变提交凭证，保存完整输入以区分安全重试与异参覆盖。",
		uniqueConstraints = {
				@UniqueConstraint(name = "dungeon_submission_battle_id_character_id_round_key",
						columnNames = { "battle_id", "character_id", "round" }),
				@UniqueConstraint(name = "uq_dungeon_submission_request",
						columnNames = { "battle_id", "character_id", "request_id" }) },
		check = { @CheckConstraint(name = "dungeon_submission_round_check", constraint = "(round > 0)") })
@Getter
@Setter
public class DungeonSubmission {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属战局主键；与独立副本进度编号区分。 */
	@Column(name = "battle_id", nullable = false, comment = "所属战局主键；与独立副本进度编号区分。")
	private Long battleId;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 战局回合序号；同一战局内按回合保存，重试不得再次执行。 */
	@Column(name = "round", nullable = false, comment = "战局回合序号；同一战局内按回合保存，重试不得再次执行。")
	private int round;

	/** 客户端UUID请求号，同一战局中的重试保持不变。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "成员提交命令的UUID请求号，重试保持不变")
	private String requestId;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text",
			comment = "已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。")
	private String payload;

}
