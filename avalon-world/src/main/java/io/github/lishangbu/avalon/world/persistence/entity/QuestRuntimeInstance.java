package io.github.lishangbu.avalon.world.persistence.entity;

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
@Entity
@Table(name = "quest_runtime_instance")
@Getter
@Setter
public class QuestRuntimeInstance {

	/** UUID由宿主生成并作为全部奖励来源的一部分，不经过浏览器数值转换。 */
	@Id
	@Column(length = 36)
	private String id;

	/** 归属由登录账号解析，客户端不能选择其他角色。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 稳定任务编号，一项任务可以有多次独立尝试。 */
	@Column(name = "quest_code", nullable = false)
	private String questCode;

	/** 同角色同任务递增的尝试序号，解决相同接取时刻下随机UUID不能确定先后顺序的问题。 */
	@Column(nullable = false)
	private int attempt;

	/** 不可变首次接取时间，历史排序不依赖UUID大小。 */
	@Column(name = "accepted_at", nullable = false)
	private Instant acceptedAt;

	/** 类型化QuestInstance的JSON，由运行时集中编解码。 */
	@Column(nullable = false, columnDefinition = "text")
	private String state;

}
