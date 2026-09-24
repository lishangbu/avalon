package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 放生审计保存玩家确认的完整个体信息；同一请求重试不能重复执行或改换名单。 */
@Entity
@Table(name = "creature_release_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class CreatureReleaseReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false)
	private String requestId;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
