package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "life_skill_receipt_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "life_skill_receipt", comment = "学习与遗忘请求回执，重放不误删重新学习的专业")
@Immutable
@IdClass(LifeSkillReceiptRow.Key.class)
public class LifeSkillReceiptRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "经认证解析的角色归属")
	private Long characterId;

	@Id
	@Column(name = "request_id", nullable = false, length = 36, comment = "标准UUID幂等请求号")
	private String requestId;

	@Column(name = "action", nullable = false, length = 16, comment = "LEARN或FORGET")
	private String action;

	@Column(name = "skill", nullable = false, length = 24, comment = "本次请求的技能编号")
	private String skill;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, String requestId) implements java.io.Serializable {
	}

}
