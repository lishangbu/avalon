package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "life_skill_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "life_skill", comment = "角色生活技能；角色锁约束最多三个专业，半点熟练度保持衰减精度",
		check = { @CheckConstraint(name = "life_skill_points_check",
				constraint = "((half_points >= 0) AND (half_points <= 130816))") })
@Immutable
@IdClass(LifeSkillRow.Key.class)
public class LifeSkillRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "角色归属")
	private Long characterId;

	@Id
	@Column(name = "skill", nullable = false, length = 24, comment = "生活技能稳定编号")
	private String skill;

	@ColumnDefault("0")
	@Column(name = "half_points", nullable = false, comment = "累计半点熟练度，最高130816对应65408点")
	private Long halfPoints;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, String skill) implements java.io.Serializable {
	}

}
