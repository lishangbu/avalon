package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 永久发现事实独立于可转手、可进化的个体；只增不删，不承载当前持有数量。 */
@Entity
@Table(name = "creature_discovery")
@Getter
@Setter
public class CreatureDiscovery {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 已发现物种的稳定内容编号；个体转手或进化不删除既有发现记录。 */
	@Column(nullable = false)
	private String species;

	/** 已发现的特性槽位 A、B 或 H；H 表示隐藏槽位，不表示必然更强。 */
	@Column(name = "ability_slot", nullable = false)
	private String abilitySlot;

	/** 该次发现对应的特性名称；两个普通槽位允许使用相同特性。 */
	@Column(nullable = false)
	private String ability;

}
