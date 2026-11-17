package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** 邀请收件箱与队伍占位分离；只有接受事务才调用正式入队命令。 */
@DatabaseForeignKey(name = "dungeon_invitation_lobby_id_fkey", columns = { "lobby_id" },
		referencedTable = "dungeon_lobby", referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_invitation_recipient_id_fkey", columns = { "recipient_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_invitation_sender_id_fkey", columns = { "sender_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@PartialIndex(name = "dungeon_invitation_inbox", columns = { "recipient_id", "id" },
		predicate = "((status)::text = 'PENDING'::text)", unique = false)
@PartialIndex(name = "dungeon_invitation_pending", columns = { "lobby_id", "recipient_id" },
		predicate = "((status)::text = 'PENDING'::text)", unique = true)
@Entity
@Table(name = "dungeon_invitation", comment = "邀请收件箱与队伍占位分离；只有接受事务才调用正式入队命令。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_invitation_sender_id_request_id_key",
				columnNames = { "sender_id", "request_id" }) },
		check = { @CheckConstraint(name = "dungeon_invitation_check", constraint = "(sender_id <> recipient_id)"),
				@CheckConstraint(name = "dungeon_invitation_status_check",
						constraint = "((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'DECLINED'::character varying, 'EXPIRED'::character varying])::text[]))") })
@Getter
@Setter
public class DungeonInvitation {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 受邀加入的副本队伍主键；接受时仍需校验队伍有效性和容量。 */
	@Column(name = "lobby_id", nullable = false, comment = "受邀加入的副本队伍主键；接受时仍需校验队伍有效性和容量。")
	private Long lobbyId;

	/** 邀请发起者的角色主键；用于验证发送资格与展示邀请来源。 */
	@Column(name = "sender_id", nullable = false, comment = "邀请发起者的角色主键；用于验证发送资格与展示邀请来源。")
	private Long senderId;

	/** 邀请接收者的角色主键；仅该角色可接受或拒绝邀请。 */
	@Column(name = "recipient_id", nullable = false, comment = "邀请接收者的角色主键；仅该角色可接受或拒绝邀请。")
	private Long recipientId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 64, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 邀请失效时间；达到该时间后不能接受，不因查看而延长。 */
	@Column(name = "expires_at", nullable = false, comment = "邀请失效时间；达到该时间后不能接受，不因查看而延长。")
	private Instant expiresAt;

	/** 邀请状态：PENDING 待处理、ACCEPTED 已接受、DECLINED 已拒绝、EXPIRED 已过期。 */
	@Column(name = "status", nullable = false, length = 12,
			comment = "邀请状态：PENDING 待处理、ACCEPTED 已接受、DECLINED 已拒绝、EXPIRED 已过期。")
	private String status;

}
