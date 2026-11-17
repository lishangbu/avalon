package io.github.lishangbu.avalon.market.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 市场操作回执和资金、实物同事务保存；同账号请求号跨操作唯一。 */
@DatabaseForeignKey(name = "market_receipt_account_fkey", columns = { "account_id" }, referencedTable = "account",
		referencedColumns = { "id" })
@Entity
@Table(name = "market_receipt", comment = "市场操作回执和资金、实物同事务保存；同账号请求号跨操作唯一。",
		uniqueConstraints = {
				@UniqueConstraint(name = "market_receipt_request_key", columnNames = { "account_id", "request_id" }) })
@Getter
@Setter
public class MarketReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 所属账号主键；凭证按账号隔离，不能由客户端越权指定。 */
	@Column(name = "account_id", nullable = false, comment = "所属账号主键；凭证按账号隔离，不能由客户端越权指定。")
	private Long accountId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false, length = 36, comment = "客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。")
	private String requestId;

	/** 市场操作类型标识；与负载共同核验，禁止同一请求号重放为另一种操作。 */
	@Column(name = "operation", nullable = false, length = 255, comment = "市场操作类型标识；与负载共同核验，禁止同一请求号重放为另一种操作。")
	private String operation;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text",
			comment = "已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。")
	private String payload;

	/** 首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。 */
	@Column(name = "result", nullable = false, columnDefinition = "text",
			comment = "首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。")
	private String result;

}
