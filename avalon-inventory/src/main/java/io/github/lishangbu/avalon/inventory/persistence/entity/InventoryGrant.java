package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.AssignedEntity;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 原始发放凭证不随实例转手、拆分或托管改变；奖励防重不再依赖当前持有者。 */
@Entity
@Table(name = "inventory_grant", uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "source" }))
@Getter
@Setter
public class InventoryGrant extends AssignedEntity<Long> {

	/** 首次库存与出处复用雪花编号；补录出处时由 CosId 单独分配，转手及拆分不改变该编号。 */
	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 首次获得奖励的角色主键；实例转手后仍保留原始发放对象。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 原始发放来源键；与首次获得角色组成唯一键，不因消耗、拆分或交易改变。 */
	@Column(nullable = false)
	private String source;

}
