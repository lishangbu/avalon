package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Immutable;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "dungeon_battle_member_battle_id_fkey", columns = { "battle_id" },
		referencedTable = "dungeon_battle", referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_battle_member_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "dungeon_battle_member", comment = "副本战局与参战角色的关联表，用于归属校验和重连定位。")
@Immutable
@IdClass(DungeonBattleMemberRow.Key.class)
public class DungeonBattleMemberRow {

	@Id
	@Column(name = "battle_id", nullable = false, comment = "参与的副本战局主键。")
	private Long battleId;

	@Id
	@Column(name = "character_id", nullable = false, comment = "参战角色主键，与战局编号组成唯一成员关系。")
	private Long characterId;

	/** JDBC 行的复合主键。 */
	public record Key(Long battleId, Long characterId) implements Serializable {
	}

}
