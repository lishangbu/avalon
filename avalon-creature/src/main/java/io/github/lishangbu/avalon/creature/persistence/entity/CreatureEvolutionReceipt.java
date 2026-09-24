package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 进化请求及已提交结果与个体、材料同事务持久化，响应丢失后返回原结果。 */
@Entity
@Table(name = "creature_evolution_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class CreatureEvolutionReceipt {

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

	/** 首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。 */
	@Column(nullable = false, columnDefinition = "text")
	private String result;

}
