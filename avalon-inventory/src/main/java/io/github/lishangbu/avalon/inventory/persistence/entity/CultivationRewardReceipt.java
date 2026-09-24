package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 同一来源完成事件对每名参与者仅结算一次；无掉落也保存凭证，不能重试刷书。 */
@Entity
@Table(name = "cultivation_reward_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "event_key" }))
@Getter
@Setter
public class CultivationRewardReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 来源类型与完成事件编号组成的幂等键；同一事件对每名参与者最多结算一次。 */
	@Column(name = "event_key", nullable = false, length = 160)
	private String eventKey;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。 */
	@Column(nullable = false, columnDefinition = "text")
	private String result;

}
