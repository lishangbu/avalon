package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "fishing_session_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "fishing_session", comment = "服务端权威钓鱼状态及最近一局结果")
@Immutable
public class FishingSessionRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "钓鱼角色")
	private Long characterId;

	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "固定步长进度、操作序号、鱼种、鱼竿和终局结果")
	private String state;

}
