package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
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

/** 入本登记即使没有药品也必须存在，防止重连后按新库存重新登记。 */
@DatabaseForeignKey(name = "dungeon_supply_scope_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@PartialIndex(name = "dungeon_supply_active_character", columns = { "character_id" }, predicate = "active",
		unique = true)
@Entity
@Table(name = "dungeon_supply_scope", comment = "入本登记即使没有药品也必须存在，防止重连后按新库存重新登记。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_supply_scope_run_id_character_id_key",
				columnNames = { "run_id", "character_id" }) },
		check = { @CheckConstraint(name = "dungeon_supply_scope_run_id_check", constraint = "(run_id > 0)") })
@Getter
@Setter
public class DungeonSupplyScope {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "run_id", nullable = false, comment = "所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。")
	private Long runId;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */

	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 入本补给登记是否仍有效；退出副本后失效，重连不能按新库存补登记。 */
	@Column(name = "active", nullable = false, comment = "入本补给登记是否仍有效；退出副本后失效，重连不能按新库存补登记。")
	private boolean active;

}
