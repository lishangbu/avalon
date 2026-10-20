package io.github.lishangbu.avalon.battle.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pvp_protection")
@Getter
@Setter
public class PvpProtection {

	@Id
	private Long id;

	private Instant expiresAt;

}
