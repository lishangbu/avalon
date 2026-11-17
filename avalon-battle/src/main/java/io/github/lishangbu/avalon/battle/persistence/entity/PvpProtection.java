package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@DatabaseForeignKey(name = "fk_pvp_protection_character", columns = { "id" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@Entity
@Table(name = "pvp_protection", comment = "人物免战保护")
@Getter
@Setter
public class PvpProtection {

	@Id
	@Column(name = "id", nullable = false, comment = "受保护人物编号")
	private Long id;

	@Column(name = "expires_at", nullable = false, comment = "保护到期时间")
	private Instant expiresAt;

}
