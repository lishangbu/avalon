package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "personal_farm_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "personal_farm", comment = "每角色唯一农田，村庄与诺姆城共享，按现实时间生长")
@Immutable
public class PersonalFarmRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "农田所属角色")
	private Long characterId;

	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "已扩建格数、作物、种子绑定及首次浇水时间")
	private String state;

}
