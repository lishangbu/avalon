package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.UniqueConstraint;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 实例级可用额度。实际能用多少还须取真实背包数量的较小值，待领数量不能直接使用。 */
@DatabaseForeignKey(name = "dungeon_supply_item_id_fkey", columns = { "item_id" }, referencedTable = "owned_item",
		referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_supply_scope_id_fkey", columns = { "scope_id" },
		referencedTable = "dungeon_supply_scope", referencedColumns = { "id" })
@Entity
@Table(name = "dungeon_supply", comment = "实例级可用额度。实际能用多少还须取真实背包数量的较小值，待领数量不能直接使用。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_supply_scope_id_item_id_key",
				columnNames = { "scope_id", "item_id" }) },
		check = { @CheckConstraint(name = "dungeon_supply_remaining_check", constraint = "(remaining >= 0)") })
@Getter
@Setter
public class DungeonSupply {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属入本补给登记主键，同一登记内按物品实例保存额度。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "scope_id", nullable = false, comment = "所属入本补给登记主键，同一登记内按物品实例保存额度。")
	private Long scopeId;

	/** 对应物品实例主键；不能用物品模板编号替代。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "item_id", nullable = false, comment = "对应物品实例主键；不能用物品模板编号替代。")
	private Long itemId;

	/** 入本额度中尚未使用的数量；实际可用量还受背包实时库存限制。 */
	@Column(name = "remaining", nullable = false, comment = "入本额度中尚未使用的数量；实际可用量还受背包实时库存限制。")
	private int remaining;

}
