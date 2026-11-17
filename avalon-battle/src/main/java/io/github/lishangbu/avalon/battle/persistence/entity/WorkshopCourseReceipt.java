package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** 课内命令回执；请求号、原负载及首次结果同状态事务提交，随机学习重试不重抽。 */
@DatabaseForeignKey(name = "fk_workshop_receipt_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "workshop_course_receipt", comment = "课程操作幂等回执；重试不重抽、不重复消费临时资源",
		uniqueConstraints = { @UniqueConstraint(name = "uq_workshop_receipt_character_request",
				columnNames = { "character_id", "request_id" }) })
@Getter
@Setter
public class WorkshopCourseReceipt {

	/** CosId雪花主键，向浏览器显示时不转为浮点数。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId生成的回执主键")
	private Long id;

	/** 角色归属，防止跨账号重放请求。 */
	@Column(name = "character_id", nullable = false, comment = "角色归属")
	private Long characterId;

	/** 标准UUID，同一角色内唯一。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "操作请求UUID")
	private String requestId;

	/** 包含会话、原修订号、动作及目标；同请求号不同负载必须拒绝。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "原动作负载与版本")
	private String payload;

	/** 首次成功响应，回放不调用随机学习、不扣书、不再次授予通过事实。 */
	@Column(name = "response", nullable = false, columnDefinition = "text", comment = "首次操作响应")
	private String response;

}
