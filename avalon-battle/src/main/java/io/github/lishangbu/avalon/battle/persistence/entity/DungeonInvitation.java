package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** 邀请收件箱与队伍占位分离；只有接受事务才调用正式入队命令。 */
@Entity
@Table(name = "dungeon_invitation")
@Getter
@Setter
public class DungeonInvitation {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 受邀加入的副本队伍主键；接受时仍需校验队伍有效性和容量。 */
	@Column(name = "lobby_id", nullable = false)
	private Long lobbyId;

	/** 邀请发起者的角色主键；用于验证发送资格与展示邀请来源。 */
	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	/** 邀请接收者的角色主键；仅该角色可接受或拒绝邀请。 */
	@Column(name = "recipient_id", nullable = false)
	private Long recipientId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false)
	private String requestId;

	/** 邀请失效时间；达到该时间后不能接受，不因查看而延长。 */
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	/** 邀请状态：PENDING 待处理、ACCEPTED 已接受、DECLINED 已拒绝、EXPIRED 已过期。 */
	@Column(nullable = false)
	private String status;

}
