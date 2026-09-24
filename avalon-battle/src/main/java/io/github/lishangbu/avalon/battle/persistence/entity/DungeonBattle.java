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

import java.time.Instant;

/** 一场多人遭遇；截止时间另存可索引列，存档仍是恢复完整回合的唯一来源。 */
@Entity
@Table(name = "dungeon_battle")
@Getter
@Setter
public class DungeonBattle {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。 */
	private boolean active;

	/** 服务端处理超时的截止时间；独立列用于调度查询，避免扫描 JSON 存档。 */
	@Column(nullable = false)
	private Instant deadline;

	/** 完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。 */
	@Column(nullable = false, columnDefinition = "text")
	private String state;

}
