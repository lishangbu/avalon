package io.github.lishangbu.avalon.character.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 一对角色只保存一条关系；申请与已接受状态共用记录，删除后重新申请会分配新的雪花编号。 */
@Entity
@Table(name = "friendship")
@Getter
@Setter
public class Friendship {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 按角色编号排序，防止 A 到 B 与 B 到 A 各生成一条关系。 */
	@Column(name = "first_character_id", nullable = false)
	private Long firstCharacterId;

	/** 关系双方中较大的角色主键；与 firstCharacterId 组成无方向的唯一角色对。 */
	@Column(name = "second_character_id", nullable = false)
	private Long secondCharacterId;

	/** 发起好友申请的角色主键，必须属于关系双方。 */
	@Column(name = "requester_id", nullable = false)
	private Long requesterId;

	/** 是否已接受申请；false 为待处理申请，true 为双向好友关系。 */
	@Column(nullable = false)
	private boolean accepted;

}
