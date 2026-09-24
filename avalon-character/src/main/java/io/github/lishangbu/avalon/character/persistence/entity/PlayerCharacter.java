package io.github.lishangbu.avalon.character.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * 角色持久化状态。职业创建后不提供修改入口，位置仅由校验相邻关系的服务变更。
 */
@Entity
@Table(name = "player_character")
@Getter
@Setter
public class PlayerCharacter {

	/**
	 * CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。
	 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 不可变的账号归属。查询必须使用认证身份限定此字段，不能信任客户端提交的账号编号。
	 */
	@Column(name = "account_id", nullable = false, unique = true)
	private Long accountId;

	/**
	 * 实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * 角色创建时选择的职业编号。当前设计不允许改职业，服务端不会信任客户端重选请求。
	 */
	@Column(nullable = false)
	private String profession;

	/**
	 * 当前地点的内容编号。移动仅允许经过地图出口，修改 URL 不改变此状态。
	 */
	@Column(nullable = false)
	private String location;

	/**
	 * 累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。
	 */
	@Column(nullable = false)
	private long experience;

	/**
	 * Hibernate 乐观版本。位置命令还会比较客户端预期 revision，拒绝旧页面重复移动。
	 */
	@Version
	private Long version;

	/** 最近成功移动的客户端版本；剧情救援或战败传送后清空，不能重放旧道路请求。 */
	@Column(name = "last_move_revision")
	private Long lastMoveRevision;

	/**
	 * 当前战局编号。只由战斗用例开启和清除，战外命令在角色锁内检查。
	 */
	@Column(name = "active_battle_id")
	private Long activeBattleId;

	/** 战外隔离课程占用；临时资源只属于该会话，结束前阻止真实资产与位置变更。 */
	@Column(name = "active_course_id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long activeCourseId;

	/** 在线生活会话占用；与采集/钓鱼状态同事务创建及释放。 */
	@Column(name = "active_life_session", length = 36)
	private String activeLifeSession;

	/** 派遣伙伴；钓鱼等无伙伴的活动保持为空。 */
	@Column(name = "dispatched_creature_id")
	private Long dispatchedCreatureId;

	/** 战间仍持有副本门禁；与单场战局编号分开，失败重试不会释放编队。 */
	@Column(name = "active_dungeon_id")
	private Long activeDungeonId;

	/** 装备或战间资源发生变化时递增；副本准备记录必须与此修订号一致。 */
	@Column(name = "preparation_revision", nullable = false)
	private long preparationRevision;

	/** 匹配占位与正式副本分离；确认期限内只限制新开个人战斗，不中断当前战局。 */
	@Column(name = "dungeon_lobby_id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long dungeonLobbyId;

	/** 副本组队确认占位的截止时间；没有占位时为空，超时由组队流程释放。 */
	@Column(name = "dungeon_hold_until")
	private Instant dungeonHoldUntil;

	/**
	 * 金币余额，奖励和消费必须与对应凭证处于同一事务。
	 */
	@Column(nullable = false)
	private long gold;

}
