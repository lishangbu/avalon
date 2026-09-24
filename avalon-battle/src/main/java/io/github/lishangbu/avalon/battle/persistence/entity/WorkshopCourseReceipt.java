package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** 课内命令回执；请求号、原负载及首次结果同状态事务提交，随机学习重试不重抽。 */
@Entity
@Table(name = "workshop_course_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class WorkshopCourseReceipt {

	/** CosId雪花主键，向浏览器显示时不转为浮点数。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 角色归属，防止跨账号重放请求。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 标准UUID，同一角色内唯一。 */
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 包含会话、原修订号、动作及目标；同请求号不同负载必须拒绝。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次成功响应，回放不调用随机学习、不扣书、不再次授予通过事实。 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
