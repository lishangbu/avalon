package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** 场景动作请求回执，与任务物品和成功事实同事务提交；错误报告也保留原响应。 */
@Entity
@Table(name = "scene_action_receipt",
		uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "request_id" }))
@Getter
@Setter
public class SceneActionReceipt {

	/** CosId雪花主键，浏览器序列化为字符串避免精度丢失。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 经认证账号解析并持锁的角色归属，不接收客户端角色编号。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 同一角色内唯一的标准UUID请求号。 */
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	/** 包含动作、原位置版本及选项的已核验请求，重复请求必须完全相同。 */
	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	/** 首次执行的成功或错误报告响应，重放不重新判断答案或发奖。 */
	@Column(nullable = false, columnDefinition = "text")
	private String response;

}
