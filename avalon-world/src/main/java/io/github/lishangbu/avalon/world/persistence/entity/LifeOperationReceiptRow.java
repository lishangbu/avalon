package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "life_operation_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "life_operation_receipt", comment = "生活生产操作回执，结果与材料消耗和工具报废原子提交")
@Immutable
@IdClass(LifeOperationReceiptRow.Key.class)
public class LifeOperationReceiptRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "操作角色")
	private Long characterId;

	@Id
	@Column(name = "request_id", nullable = false, length = 36, comment = "标准UUID请求号")
	private String requestId;

	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "排序后规范化请求内容，防止同请求号更换材料")
	private String payload;

	@Column(name = "result", nullable = false, columnDefinition = "text", comment = "已提交结果，重试不重新抽取工具报废")
	private String result;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, String requestId) implements java.io.Serializable {
	}

}
