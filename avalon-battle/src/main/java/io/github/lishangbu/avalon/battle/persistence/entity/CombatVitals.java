package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.AssignedEntity;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 人物或伙伴的剩余生命魔力。上限来自当前成长与装备，升级时不自动增加剩余值。
 */
@Entity
@Table(name = "combat_vitals")
@Getter
@Setter
public class CombatVitals extends AssignedEntity<Long> {

	/** 资源所属战斗单位的主键，复用人物或伙伴编号，不另行生成资源记录编号。 */
	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 当前剩余生命值；零表示倒下，成长或装备变化不能自动恢复生命。 */
	private int hp;

	/** 当前剩余魔力值；上限由成长和装备计算，上限增加不自动补满。 */
	private int mp;

}
