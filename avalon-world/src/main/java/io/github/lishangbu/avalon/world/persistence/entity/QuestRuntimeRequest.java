package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 实例命令的请求回执。请求号在同角色内全局唯一，不能被另一操作或实例复用。 */
@Entity
@Table(name = "quest_runtime_request",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class QuestRuntimeRequest {

	@Id
	@CosIdGenerated
	private Long id;

	@Column(name = "character_id", nullable = false)
	private Long characterId;

	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 包含操作、实例、轮次和参数的规范JSON，用于拒绝不同内容复用请求号。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次成功的完整响应，含玩家可读奖励说明，重试不按当前配置重新生成。 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
