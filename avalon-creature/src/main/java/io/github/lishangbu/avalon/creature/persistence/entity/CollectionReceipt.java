package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 账号级一次性图鉴结算；保存实际发放值，不因后来调整目录而改写历史收益。 */
@Entity
@Table(name = "collection_receipt", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "code" }))
@Getter
@Setter
public class CollectionReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 所属账号主键；凭证按账号隔离，不能由客户端越权指定。 */
	@Column(name = "account_id", nullable = false)
	private Long accountId;

	/** 账号级图鉴奖励的稳定编号；同账号同编号只结算一次。 */
	@Column(nullable = false)
	private String code;

	/** 本次实际发放的金币数量；保存历史值，不随后续奖励配置调整。 */
	@Column(nullable = false)
	private int gold;

	/** 本次奖励物品编号；无物品奖励时允许为空。 */
	@Column(name = "item_code")
	private String itemCode;

	/** 本次实际发放的物品数量；没有物品奖励时为零。 */
	@Column(nullable = false)
	private int quantity;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
