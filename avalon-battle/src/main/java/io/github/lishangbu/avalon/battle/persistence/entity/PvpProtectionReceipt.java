package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pvp_protection_receipt")
@Getter
@Setter
public class PvpProtectionReceipt {

	@Id
	@CosIdGenerated
	private Long id;

	private long characterId;

	private String requestId;

	private Instant expiresAt;

}
