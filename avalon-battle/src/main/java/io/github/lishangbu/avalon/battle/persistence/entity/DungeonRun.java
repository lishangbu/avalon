package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
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

/** 三场遭遇共用一个进度锁；单场战局仍有独立存档与提交凭证。 */
@DatabaseForeignKey(name = "dungeon_run_active_battle_id_fkey", columns = { "active_battle_id" },
		referencedTable = "dungeon_battle", referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_run_leader_id_fkey", columns = { "leader_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@PartialIndex(name = "dungeon_run_active_battle", columns = { "active_battle_id" },
		predicate = "(active_battle_id IS NOT NULL)", unique = true)
@Entity
@Table(name = "dungeon_run", comment = "三场遭遇共用一个进度锁；单场战局仍有独立存档与提交凭证。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_run_leader_id_request_id_key",
				columnNames = { "leader_id", "request_id" }) })
@Getter
@Setter
public class DungeonRun {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 发起副本或队伍的角色主键，不是账号编号。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "leader_id", nullable = false, comment = "发起副本或队伍的角色主键，不是账号编号。")
	private Long leaderId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 64, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。 */
	@Column(name = "active", nullable = false, comment = "是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。")
	private boolean active;

	/** 当前进行中的战局主键；战间为空，副本进度仍可保持有效。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "active_battle_id", comment = "当前进行中的战局主键；战间为空，副本进度仍可保持有效。")
	private Long activeBattleId;

	/** 完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。 */
	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。")
	private String state;

}
