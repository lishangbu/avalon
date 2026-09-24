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
 * 一场独立遭遇。JSON 保存当场个体与资源，active 部分唯一索引阻止重复开战。
 */
@Entity
@Table(name = "battle")
@Getter
@Setter
public class Battle {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/**
	 * 开战客户端请求号，仅用于幂等，不能用作随机种子。
	 */
	@Column(name = "request_id", nullable = false, length = 64)
	private String requestId;

	/** 是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。 */
	private boolean active;

	/** 完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。 */
	@Column(nullable = false, columnDefinition = "text")
	private String state;

}
