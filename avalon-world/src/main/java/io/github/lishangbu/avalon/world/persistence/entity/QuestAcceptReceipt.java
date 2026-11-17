package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;

/** 接取回执独立于完成回执；同一任务多个新请求只记录重放，不重复执行补给。 */
@DatabaseForeignKey(name = "fk_quest_accept_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "quest_accept_receipt", comment = "任务显式接取回执，补给与接取事实同事务，独立于完成奖励",
		uniqueConstraints = { @UniqueConstraint(name = "uq_quest_accept_character_request",
				columnNames = { "character_id", "request_id" }) },
		indexes = { @Index(name = "ix_quest_accept_character_step", columnList = "character_id, step, id") })
@Getter
@Setter
public class QuestAcceptReceipt {

	/** CosId雪花主键，浏览器按字符串处理。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId雪花主键")
	private Long id;

	/** 来自登录账号并已持锁的角色归属。 */
	@Column(name = "character_id", nullable = false, comment = "接取任务的角色归属")
	private Long characterId;

	/** 原标准UUID请求号，同一角色内唯一。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "同一角色内唯一标准UUID请求号")
	private String requestId;

	/** 接取的稳定任务步骤编号，不以标题判断任务身份。 */
	@Column(name = "step", nullable = false, length = 255, comment = "接取的稳定任务步骤编号")
	private String step;

	/** 所属任务实例，不允许缺失；不同实例的补给回执独立保存。 */
	@Column(name = "runtime_id", nullable = false, length = 36, comment = "接取补给所属任务实例")
	private String runtimeId;

	/** 环内补给按轮次发放，恢复同一轮不重新发。 */
	@ColumnDefault("1")
	@Column(name = "runtime_round", nullable = false, comment = "委托环内补给轮次，重接同轮不重复发放")
	private int runtimeRound = 1;

	/** 原任务步骤和位置版本，防止同请求号被另一个动作复用。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "原步骤与位置版本，重复请求需完全一致")
	private String payload;

	/** 首次接取时刻；后续同任务新请求沿用，作为捕获来源时间核验依据。 */
	@Column(name = "accepted_at", nullable = false, comment = "第一次接取时刻，同任务后续请求沿用，供捕获时间核验")
	private Instant acceptedAt;

	/** 本请求的原始响应；重复请求只解码，不重新发放。 */
	@Column(name = "response", nullable = false, columnDefinition = "text", comment = "首次响应原文，包含原补给说明或已接取提示")
	private String response;

}
