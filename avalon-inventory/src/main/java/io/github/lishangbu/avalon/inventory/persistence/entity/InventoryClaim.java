package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 部分领取的持久化凭证，刷新或重复提交同一请求号不得继续扣减待领数量。 */
@Entity
@Table(name = "inventory_claim")
@Getter
@Setter
public class InventoryClaim {

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

	/** 对应物品实例主键；不能用物品模板编号替代。 */
	@Column(name = "item_id", nullable = false)
	private Long itemId;

	/** 本次实际从待领转入可用背包的数量；重复请求不继续领取剩余部分。 */
	@Column(nullable = false)
	private int quantity;

}
