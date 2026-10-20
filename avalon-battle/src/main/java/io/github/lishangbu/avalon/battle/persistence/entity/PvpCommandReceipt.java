package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 每位参与者的完整请求与响应，重放只读首次响应。 */
@Entity
@Table(name = "pvp_command_receipt")
@Getter
@Setter
public class PvpCommandReceipt {

	@Id
	@CosIdGenerated
	private Long id;

	@Column(name = "battle_id", nullable = false)
	private Long battleId;

	@Column(name = "character_id", nullable = false)
	private Long characterId;

	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
