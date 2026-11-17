package io.github.lishangbu.avalon.battle.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 一场共享对战只有一份状态，双方角色占用由应用事务共同维护。 */
@DatabaseForeignKey(name = "fk_pvp_attacker", columns = { "attacker_id" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@DatabaseForeignKey(name = "fk_pvp_defender", columns = { "defender_id" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@Entity
@Table(name = "pvp_battle", comment = "训练师共享回合对战",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_pvp_start_request", columnNames = { "attacker_id", "request_id" }) },
		check = { @CheckConstraint(name = "ck_pvp_distinct", constraint = "(attacker_id <> defender_id)") })
@Getter
@Setter
public class PvpBattle {

	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "对战编号")
	private Long id;

	@Column(name = "attacker_id", nullable = false, comment = "发起方人物编号")
	private Long attackerId;

	@Column(name = "defender_id", nullable = false, comment = "受袭方人物编号")
	private Long defenderId;

	@Column(name = "request_id", nullable = false, length = 36, comment = "发起请求幂等编号")
	private String requestId;

	@Column(name = "active", nullable = false, comment = "是否仍在对战")
	private boolean active;

	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "双方战斗状态与本轮行动快照")
	private String state;

}
