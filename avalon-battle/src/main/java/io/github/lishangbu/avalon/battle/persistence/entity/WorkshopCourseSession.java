package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** 一次战外隔离课程；临时库存只存此会话JSON，结束清空后保留结果供审计与重试。 */
@Entity
@Table(name = "workshop_course_session",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class WorkshopCourseSession {

	/** CosId生成的会话主键，序列化成字符串防止浏览器精度丢失。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 已认证账号对应的角色归属，所有读取也必须核对此值。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 开始请求的标准UUID，同一请求重放不重新生成临时输入。 */
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 本次只允许BQ021/BQ022，不能由客户端传课程数值或技能池。 */
	@Column(nullable = false, length = 16)
	private String course;

	/** ACTIVE/PASSED/EXITED；非活动历史会话不可继续操作。 */
	@Column(nullable = false, length = 16)
	private String status;

	/** 完整临时状态与修订号，断线读取此状态而非重新抽样或重新配资源。 */
	@Column(nullable = false, columnDefinition = "text")
	private String state;

	/** 首次入课时刻，用于审计，不用于现实限时或强制过期。 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
