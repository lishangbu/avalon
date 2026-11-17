package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 合成结果独立于成品库存数量保存，成品使用后重试旧请求也不会再发一本。 */
@DatabaseForeignKey(name = "crafting_receipt_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "crafting_receipt", comment = "合成结果独立于成品库存数量保存，成品使用后重试旧请求也不会再发一本。",
		uniqueConstraints = { @UniqueConstraint(name = "crafting_receipt_character_id_request_id_key",
				columnNames = { "character_id", "request_id" }) })
@Getter
@Setter
public class CraftingReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属角色主键；操作时由认证账号定位角色并校验归属。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色主键；操作时由认证账号定位角色并校验归属。")
	private Long characterId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text",
			comment = "已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。")
	private String payload;

	/** 首次合成发放的成品物品编号，重试不重新选择配方结果。 */
	@Column(name = "product", nullable = false, length = 255, comment = "首次合成发放的成品物品编号，重试不重新选择配方结果。")
	private String product;

	/** 首次合成结果的绑定状态，独立于成品当前是否仍在背包保存。 */
	@Column(name = "bound", nullable = false, comment = "首次合成结果的绑定状态，独立于成品当前是否仍在背包保存。")
	private boolean bound;

}
