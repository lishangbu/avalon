package io.github.lishangbu.avalon.inventory.persistence.entity;

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
@Entity
@Table(name = "dungeon_supply")
@Getter
@Setter
public class DungeonSupply {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属入本补给登记主键，同一登记内按物品实例保存额度。 */
	@Column(name = "scope_id", nullable = false)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long scopeId;

	/** 对应物品实例主键；不能用物品模板编号替代。 */
	@Column(name = "item_id", nullable = false)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long itemId;

	/** 入本额度中尚未使用的数量；实际可用量还受背包实时库存限制。 */
	private int remaining;

}
