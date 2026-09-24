package io.github.lishangbu.avalon.market.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 市场操作回执和资金、实物同事务保存；同账号请求号跨操作唯一。 */
@Entity
@Table(name = "market_receipt", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "request_id" }))
@Getter
@Setter
public class MarketReceipt {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	private Long id;

	/** 所属账号主键；凭证按账号隔离，不能由客户端越权指定。 */
	@Column(name = "account_id", nullable = false)
	private Long accountId;

	/** 客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。 */
	@Column(name = "request_id", nullable = false)
	private String requestId;

	/** 市场操作类型标识；与负载共同核验，禁止同一请求号重放为另一种操作。 */
	@Column(nullable = false)
	private String operation;

	/** 已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。 */
	@Column(nullable = false, columnDefinition = "text")
	private String result;

}
