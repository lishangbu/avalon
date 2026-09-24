package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 步骤完成与回复一并持久化。重复请求返回首次战利品说明，不再次执行效果。
 */
@Entity
@Table(name = "quest_receipt", uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "step" }))
@Getter
@Setter
public class QuestReceipt {

	/**
	 * CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。
	 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。
	 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/**
	 * 已完成步骤编号。同一角色与步骤唯一，是整个步骤命令防重和回执重放的依据。
	 */
	@Column(nullable = false)
	private String step;

	/**
	 * 步骤所属主线任务编号，用于查询已完成任务，不替代步骤级幂等键。
	 */
	@Column(name = "quest_code", nullable = false)
	private String questCode;

	/**
	 * 本步骤是否标记整项任务完成；普通对话步骤也有凭证，但不会进入已完成任务列表。
	 */
	@Column(nullable = false)
	private boolean completed;

	/**
	 * 首次成功结果的 JSON 回执。重试只读取它，不再执行发奖；损坏时应报内部错误。
	 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

	/**
	 * 首次完成时间，用于稳定展示顺序；相同时间由主键作为第二排序条件。
	 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
