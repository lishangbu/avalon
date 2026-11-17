package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * 每次尝试的独立存储。JSON仅包含进度、分支及已结算效果，不包含运行用定义快照或版本。 所有写入在角色锁下完成；任务结束后保留记录，用于次数统计和重试回执。
 */
@DatabaseForeignKey(name = "fk_quest_runtime_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "quest_runtime_instance", comment = "独立任务尝试；JSON仅保存运行状态，不保存定义版本",
		uniqueConstraints = { @UniqueConstraint(name = "uq_quest_runtime_attempt",
				columnNames = { "character_id", "quest_code", "attempt" }) },
		indexes = { @Index(name = "idx_quest_runtime_history", columnList = "character_id, quest_code, accepted_at") })
@Getter
@Setter
public class QuestRuntimeInstance {

	/** UUID由宿主生成并作为全部奖励来源的一部分，不经过浏览器数值转换。 */
	@Id
	@Column(name = "id", nullable = false, length = 36, comment = "实例UUID、请求雪花编号或定义稳定编号，取决于所属表")
	private String id;

	/** 归属由登录账号解析，客户端不能选择其他角色。 */
	@Column(name = "character_id", nullable = false, comment = "服务端登录身份解析的归属角色编号")
	private Long characterId;

	/** 稳定任务编号，一项任务可以有多次独立尝试。 */
	@Column(name = "quest_code", nullable = false, length = 255, comment = "稳定任务编号，多次尝试使用不同实例")
	private String questCode;

	/** 同角色同任务递增的尝试序号，解决相同接取时刻下随机UUID不能确定先后顺序的问题。 */
	@Column(name = "attempt", nullable = false, comment = "同角色同任务递增的尝试序号，相同接取时间仍可准确定位最新实例")
	private int attempt;

	/** 不可变首次接取时间，历史排序不依赖UUID大小。 */
	@Column(name = "accepted_at", nullable = false, comment = "首次接取时间，历史按时间与实例编号稳定排序")
	private Instant acceptedAt;

	/** 类型化QuestInstance的JSON，由运行时集中编解码。 */
	@Column(name = "state", nullable = false, columnDefinition = "text", comment = "类型化任务实例JSON，保存进度、轮次、分支和结算回执")
	private String state;

}
