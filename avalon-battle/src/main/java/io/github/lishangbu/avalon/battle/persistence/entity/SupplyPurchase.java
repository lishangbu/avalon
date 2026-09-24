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
 * 购买凭证与扣款、道具发放共同提交，响应丢失重试时不重复扣款。
 */
@Entity
@Table(name = "supply_purchase")
@Getter
@Setter
public class SupplyPurchase {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 64)
	private String requestId;

	/** 本次购买的补给物品编号；与幂等请求一起核验，防止重试换购另一种物品。 */
	@Column(nullable = false)
	private String code;

}
