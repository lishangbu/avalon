package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 实例命令的请求回执。请求号在同角色内全局唯一，不能被另一操作或实例复用。 */
@DatabaseForeignKey(name = "fk_quest_runtime_request_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "quest_runtime_request", comment = "实例写命令的首次响应及规范请求，奖励和回执同事务",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_quest_runtime_request", columnNames = { "character_id", "request_id" }) })
@Getter
@Setter
public class QuestRuntimeRequest {

	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "实例UUID、请求雪花编号或定义稳定编号，取决于所属表")
	private Long id;

	@Column(name = "character_id", nullable = false, comment = "服务端登录身份解析的归属角色编号")
	private Long characterId;

	@Column(name = "request_id", nullable = false, length = 36, comment = "角色内唯一的标准UUID请求号，重试重放首次响应")
	private String requestId;

	/** 包含操作、实例、轮次和参数的规范JSON，用于拒绝不同内容复用请求号。 */
	@Column(name = "payload", nullable = false, columnDefinition = "text", comment = "规范请求JSON，拒绝相同请求号提交不同操作或参数")
	private String payload;

	/** 首次成功的完整响应，含玩家可读奖励说明，重试不按当前配置重新生成。 */
	@Column(name = "response", nullable = false, columnDefinition = "text", comment = "首次成功响应JSON，奖励与状态共事务保存")
	private String response;

}
