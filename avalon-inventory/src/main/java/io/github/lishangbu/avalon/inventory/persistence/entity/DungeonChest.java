package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 宝箱是绑定角色的领取凭证；抽取结果在胜利事务内保存，领取后仍保留防重记录。 */
@Entity
@Table(name = "dungeon_chest")
@Getter
@Setter
public class DungeonChest {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。 */
	@Column(name = "run_id", nullable = false, unique = true)
	private Long runId;

	/** 宝箱展示名称；防重使用副本进度编号，不依赖名称。 */
	@Column(nullable = false)
	private String name;

	/** 胜利时固定的奖励清单 JSON；领取只发放既定结果，不再次随机抽取。 */
	@Column(nullable = false)
	private String loot;

	/** 是否已经完成领取；领取和发奖同事务，保留记录防止重复领取。 */
	@Column(nullable = false)
	private boolean claimed;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
