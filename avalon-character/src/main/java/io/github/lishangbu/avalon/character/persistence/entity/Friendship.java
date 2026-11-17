package io.github.lishangbu.avalon.character.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 一对角色只保存一条关系；申请与已接受状态共用记录，删除后重新申请会分配新的雪花编号。 */
@DatabaseForeignKey(name = "friendship_first_character_id_fkey", columns = { "first_character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "friendship_requester_id_fkey", columns = { "requester_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "friendship_second_character_id_fkey", columns = { "second_character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "friendship", comment = "一对角色只保存一条关系；申请与已接受状态共用记录，删除后重新申请会分配新的雪花编号。",
		uniqueConstraints = { @UniqueConstraint(name = "friendship_first_character_id_second_character_id_key",
				columnNames = { "first_character_id", "second_character_id" }) },
		indexes = { @Index(name = "friendship_first_page", columnList = "first_character_id, id"),
				@Index(name = "friendship_second_page", columnList = "second_character_id, id") },
		check = {
				@CheckConstraint(name = "friendship_check", constraint = "(first_character_id < second_character_id)"),
				@CheckConstraint(name = "friendship_check1",
						constraint = "((requester_id = first_character_id) OR (requester_id = second_character_id))") })
@Getter
@Setter
public class Friendship {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 按角色编号排序，防止 A 到 B 与 B 到 A 各生成一条关系。 */
	@Column(name = "first_character_id", nullable = false, comment = "按角色编号排序，防止 A 到 B 与 B 到 A 各生成一条关系。")
	private Long firstCharacterId;

	/** 关系双方中较大的角色主键；与 firstCharacterId 组成无方向的唯一角色对。 */
	@Column(name = "second_character_id", nullable = false, comment = "关系双方中较大的角色主键；与 firstCharacterId 组成无方向的唯一角色对。")
	private Long secondCharacterId;

	/** 发起好友申请的角色主键，必须属于关系双方。 */
	@Column(name = "requester_id", nullable = false, comment = "发起好友申请的角色主键，必须属于关系双方。")
	private Long requesterId;

	/** 是否已接受申请；false 为待处理申请，true 为双向好友关系。 */
	@ColumnDefault("false")
	@Column(name = "accepted", nullable = false, comment = "是否已接受申请；false 为待处理申请，true 为双向好友关系。")
	private boolean accepted;

}
