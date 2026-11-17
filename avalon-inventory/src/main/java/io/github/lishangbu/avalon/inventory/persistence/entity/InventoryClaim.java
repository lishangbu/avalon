package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 部分领取的持久化凭证，刷新或重复提交同一请求号不得继续扣减待领数量。 */
@DatabaseForeignKey(name = "inventory_claim_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "inventory_claim_item_id_fkey", columns = { "item_id" }, referencedTable = "owned_item",
		referencedColumns = { "id" })
@Entity
@Table(name = "inventory_claim", comment = "部分领取的持久化凭证，刷新或重复提交同一请求号不得继续扣减待领数量。",
		uniqueConstraints = { @UniqueConstraint(name = "inventory_claim_character_id_request_id_key",
				columnNames = { "character_id", "request_id" }) },
		check = { @CheckConstraint(name = "inventory_claim_quantity_check", constraint = "(quantity > 0)") })
@Getter
@Setter
public class InventoryClaim {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 对应物品实例主键；不能用物品模板编号替代。 */
	@Column(name = "item_id", nullable = false, comment = "对应物品实例主键；不能用物品模板编号替代。")
	private Long itemId;

	/** 本次实际从待领转入可用背包的数量；重复请求不继续领取剩余部分。 */
	@Column(name = "quantity", nullable = false, comment = "本次实际从待领转入可用背包的数量；重复请求不继续领取剩余部分。")
	private int quantity;

}
