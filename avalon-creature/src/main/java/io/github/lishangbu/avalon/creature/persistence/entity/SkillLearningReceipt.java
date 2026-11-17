package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 学习请求的不可变结算凭证；负载用于识别同一请求号被拿来学习另一只伙伴或另一本书。 */
@DatabaseForeignKey(name = "skill_learning_receipt_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "skill_learning_receipt", comment = "学习请求的不可变结算凭证；负载用于识别同一请求号被拿来学习另一只伙伴或另一本书。",
		uniqueConstraints = { @UniqueConstraint(name = "skill_learning_receipt_character_id_request_id_key",
				columnNames = { "character_id", "request_id" }) })
@Getter
@Setter
public class SkillLearningReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text",
			comment = "已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。")
	private String payload;

	/** 首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。 */
	@Column(name = "result", nullable = false, columnDefinition = "text",
			comment = "首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。")
	private String result;

}
