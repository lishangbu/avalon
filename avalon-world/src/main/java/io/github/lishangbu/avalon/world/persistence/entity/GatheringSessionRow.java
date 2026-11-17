package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "gathering_character_fk", columns = { "character_id" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@Entity
@Table(name = "gathering_session", comment = "当前在线采集会话，角色锁下推进，断线不补算")
@Immutable
public class GatheringSessionRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "唯一角色归属，一次只能派遣一只伙伴")
	private Long characterId;

	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "会话编号、资源点、伙伴工具、剩余备用、已确认进度及心跳版本")
	private String state;

}
