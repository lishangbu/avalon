package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** 最早成员截止时间独立索引，避免 内存调度器 扫描全部 JSON 存档。 */
@Entity
@Table(name = "dungeon_lobby")
@Getter
@Setter
public class DungeonLobby {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 发起副本或队伍的角色主键，不是账号编号。 */
	@Column(name = "leader_id", nullable = false)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long leaderId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false)
	private String requestId;

	/** 队伍是否仍开放；关闭后的历史记录不能继续接受入队。 */
	private boolean open;

	/** 最早成员的确认截止时间；无待超时占位时允许为空，用于定时清理查询。 */
	private Instant deadline;

	/** 完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。 */
	@Column(nullable = false, columnDefinition = "text")
	private String state;

}
