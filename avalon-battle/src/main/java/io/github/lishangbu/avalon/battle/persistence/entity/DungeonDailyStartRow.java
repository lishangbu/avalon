package io.github.lishangbu.avalon.battle.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "dungeon_daily_start_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "dungeon_daily_start", comment = "角色每个副本的每日发起次数，仅记录发起者消耗。",
		check = { @CheckConstraint(name = "dungeon_daily_start_starts_check", constraint = "(starts > 0)") })
@Immutable
@IdClass(DungeonDailyStartRow.Key.class)
public class DungeonDailyStartRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "消耗每日发起次数的角色主键。")
	private Long characterId;

	@Id
	@Column(name = "dungeon", nullable = false, length = 64, comment = "独立计数的副本标识。")
	private String dungeon;

	@Id
	@Column(name = "day", nullable = false, comment = "副本次数归属的业务日期，由服务端每日重置规则确定。")
	private java.time.LocalDate day;

	@Column(name = "starts", nullable = false, comment = "该角色当日已经使用的发起次数；普通队员不消耗此计数。")
	private Integer starts;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, String dungeon, java.time.LocalDate day) implements java.io.Serializable {
	}

}
