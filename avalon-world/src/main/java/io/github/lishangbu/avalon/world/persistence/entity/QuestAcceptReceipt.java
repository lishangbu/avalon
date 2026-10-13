package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;

/** 接取回执独立于完成回执；同一任务多个新请求只记录重放，不重复执行补给。 */
@Entity
@Table(name = "quest_accept_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class QuestAcceptReceipt {

	/** CosId雪花主键，浏览器按字符串处理。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 来自登录账号并已持锁的角色归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 原标准UUID请求号，同一角色内唯一。 */
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 接取的稳定任务步骤编号，不以标题判断任务身份。 */
	@Column(nullable = false)
	private String step;

	/** 所属任务实例，不允许缺失；不同实例的补给回执独立保存。 */
	@Column(name = "runtime_id", length = 36, nullable = false)
	private String runtimeId;

	/** 环内补给按轮次发放，恢复同一轮不重新发。 */
	@Column(name = "runtime_round", nullable = false)
	private int runtimeRound = 1;

	/** 原任务步骤和位置版本，防止同请求号被另一个动作复用。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次接取时刻；后续同任务新请求沿用，作为捕获来源时间核验依据。 */
	@Column(name = "accepted_at", nullable = false)
	private Instant acceptedAt;

	/** 本请求的原始响应；重复请求只解码，不重新发放。 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
