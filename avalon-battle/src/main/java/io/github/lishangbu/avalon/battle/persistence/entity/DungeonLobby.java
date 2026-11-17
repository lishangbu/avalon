package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** 最早成员截止时间独立索引，避免 内存调度器 扫描全部 JSON 存档。 */
@DatabaseForeignKey(name = "dungeon_lobby_leader_id_fkey", columns = { "leader_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@PartialIndex(name = "dungeon_lobby_deadline", columns = { "deadline" }, predicate = "open", unique = false)
@Entity
@Table(name = "dungeon_lobby", comment = "最早成员截止时间独立索引，避免 Quartz 扫描全部 JSON 存档。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_lobby_leader_id_request_id_key",
				columnNames = { "leader_id", "request_id" }) })
@Getter
@Setter
public class DungeonLobby {

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

	/** 队伍是否仍开放；关闭后的历史记录不能继续接受入队。 */
	@Column(name = "open", nullable = false, comment = "队伍是否仍开放；关闭后的历史记录不能继续接受入队。")
	private boolean open;

	/** 最早成员的确认截止时间；无待超时占位时允许为空，用于定时清理查询。 */
	@Column(name = "deadline", comment = "最早成员的确认截止时间；无待超时占位时允许为空，用于定时清理查询。")
	private Instant deadline;

	/** 完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。 */
	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。")
	private String state;

}
