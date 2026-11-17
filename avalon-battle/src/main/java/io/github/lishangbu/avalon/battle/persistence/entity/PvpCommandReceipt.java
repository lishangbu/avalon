package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 每位参与者的完整请求与响应，重放只读首次响应。 */
@DatabaseForeignKey(name = "fk_pvp_receipt_battle", columns = { "battle_id" }, referencedTable = "pvp_battle",
		referencedColumns = { "id" })
@DatabaseForeignKey(name = "fk_pvp_receipt_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "pvp_command_receipt", comment = "训练师对战操作幂等回执",
		uniqueConstraints = { @UniqueConstraint(name = "uq_pvp_command_request",
				columnNames = { "battle_id", "character_id", "request_id" }) })
@Getter
@Setter
public class PvpCommandReceipt {

	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "回执编号")
	private Long id;

	@Column(name = "battle_id", nullable = false, comment = "所属对战编号")
	private Long battleId;

	@Column(name = "character_id", nullable = false, comment = "操作人物编号")
	private Long characterId;

	@Column(name = "request_id", nullable = false, length = 36, comment = "操作请求幂等编号")
	private String requestId;

	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "首次请求载荷")
	private String payload;

	@Column(name = "response", nullable = false, columnDefinition = "text", comment = "首次响应快照")
	private String response;

}
