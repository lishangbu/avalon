package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@DatabaseForeignKey(name = "fk_pvp_protection_receipt_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "pvp_protection_receipt", comment = "免战牌使用幂等回执",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_pvp_protection_request", columnNames = { "character_id", "request_id" }) })
@Getter
@Setter
public class PvpProtectionReceipt {

	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "回执编号")
	private Long id;

	@Column(name = "character_id", nullable = false, comment = "使用人物编号")
	private long characterId;

	@Column(name = "request_id", nullable = false, length = 36, comment = "使用请求幂等编号")
	private String requestId;

	@Column(name = "expires_at", nullable = false, comment = "首次使用确定的到期时间")
	private Instant expiresAt;

}
