package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 一场共享对战只有一份状态，双方角色占用由应用事务共同维护。 */
@Entity
@Table(name = "pvp_battle")
@Getter
@Setter
public class PvpBattle {

	@Id
	@CosIdGenerated
	private Long id;

	@Column(name = "attacker_id", nullable = false)
	private Long attackerId;

	@Column(name = "defender_id", nullable = false)
	private Long defenderId;

	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	private boolean active;

	@Column(nullable = false, columnDefinition = "text")
	private String state;

}
