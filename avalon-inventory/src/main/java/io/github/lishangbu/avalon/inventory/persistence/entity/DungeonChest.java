package io.github.lishangbu.avalon.inventory.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** 宝箱是绑定角色的领取凭证；抽取结果在胜利事务内保存，领取后仍保留防重记录。 */
@DatabaseForeignKey(name = "dungeon_chest_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "dungeon_chest_run_id_fkey", columns = { "run_id" }, referencedTable = "dungeon_run",
		referencedColumns = { "id" })
@Entity
@Table(name = "dungeon_chest", comment = "宝箱是绑定角色的领取凭证；抽取结果在胜利事务内保存，领取后仍保留防重记录。",
		uniqueConstraints = { @UniqueConstraint(name = "dungeon_chest_run_id_key", columnNames = { "run_id" }) },
		indexes = { @Index(name = "dungeon_chest_owner", columnList = "character_id, claimed") })
@Getter
@Setter
public class DungeonChest {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。 */
	@Column(name = "run_id", nullable = false, comment = "所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。")
	private Long runId;

	/** 宝箱展示名称；防重使用副本进度编号，不依赖名称。 */
	@Column(name = "name", nullable = false, length = 100, comment = "宝箱展示名称；防重使用副本进度编号，不依赖名称。")
	private String name;

	/** 胜利时固定的奖励清单 JSON；领取只发放既定结果，不再次随机抽取。 */
	@Column(name = "loot", nullable = false, columnDefinition = "text", comment = "胜利时固定的奖励清单 JSON；领取只发放既定结果，不再次随机抽取。")
	private String loot;

	/** 是否已经完成领取；领取和发奖同事务，保留记录防止重复领取。 */
	@ColumnDefault("false")
	@Column(name = "claimed", nullable = false, comment = "是否已经完成领取；领取和发奖同事务，保留记录防止重复领取。")
	private boolean claimed;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false, comment = "首次成功创建时间，使用服务端时间保存；重试不覆盖。")
	private Instant createdAt;

}
