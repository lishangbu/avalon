package io.github.lishangbu.avalon.creature.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 玩家拥有的幻兽实例，特性槽位在生成时确定，技能独立保存以便后续学习与遗忘。
 */
@Entity
@Table(name = "owned_creature", uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "source" }))
@Getter
@Setter
public class OwnedCreature {

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

	/** 首次获得者不随交易变更，用于保持来源防重；未记录时为空，不猜测身份。 */
	@Column(name = "origin_character_id")
	private Long originCharacterId;

	/** 仅亲自捕获才保存捕获者；任务赠宠没有捕获者，不能把当前持有者冒充捕获者。 */
	@Column(name = "captor_character_id")
	private Long captorCharacterId;

	/** 初始来源类别在创建时固定；进化、交易不覆盖原始来源。 */
	@Column(name = "origin_kind", nullable = false, length = 16)
	private String originKind = "UNKNOWN";

	/** 未知来源默认绑定；捕获入口明确设为未绑定，后续绑定材料只允许将其改为绑定。 */
	@Column(nullable = false)
	private boolean bound = true;

	/** 显式记录是否自定义过昵称，进化不能凭名称与默认名相等就误覆盖玩家昵称。 */
	@Column(name = "custom_name", nullable = false)
	private boolean customName;

	/**
	 * 奖励来源幂等键，当前为步骤编号加效果顺序；同一角色与来源组合只能生成一次实例。
	 */
	@Column(nullable = false)
	private String source;

	/**
	 * 物种目录的稳定编号。用于关联模板，实例等级、技能和特性仍独立持久化。
	 */
	@Column(nullable = false)
	private String species;

	/**
	 * 实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。
	 */
	@Column(nullable = false)
	private String name;

	/** 成功更名次数随个体保存；不由名称是否等于物种默认名推算免费资格。 */
	@Column(name = "rename_count", nullable = false)
	private int renameCount;

	/**
	 * 实例元素名称，来自启动校验后的模板。当前不在此实体中计算属性相克倍率。
	 */
	@Column(nullable = false)
	private String element;

	/**
	 * 实例等级，赠宠创建时取模板值，不根据加入前人物经验追赶等级。
	 */
	@Column(nullable = false)
	private int level;

	/**
	 * 累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。
	 */
	@Column(nullable = false)
	private long experience;

	/**
	 * 生成时确定的 A/B/H 槽位。保存槽位本身，以便后续进化维持槽位语义。
	 */
	@Column(name = "ability_slot", nullable = false)
	private String abilitySlot;

	/**
	 * 该实例当前特性名称；不能仅凭是否隐藏槽位推断效果更强。
	 */
	@Column(nullable = false)
	private String ability;

	/**
	 * 实例技能列表的 JSON 文本，由持久化 codec 转换，顺序和内容不能由客户端任意覆盖。
	 */
	@Column(nullable = false)
	private String skills;

	/**
	 * 从零开始的随身位置；同一角色不能重复占位，容量与出战数量是两种不同约束。
	 */
	@Column(name = "party_position")
	private Integer partyPosition;

	/** 玩家主动保护的个体不能放生；锁定不限制正常编队和学习。 */
	@Column(nullable = false)
	private boolean locked;

	/** 市场托管不占编队或仓库位置，禁止所有普通培养和管理命令访问。 */
	@Column(name = "in_custody", nullable = false)
	private boolean inCustody;

	public boolean unavailable() {
		return inCustody || releasedAt != null;
	}

	/** 展示候选与正式托管共用资格，不能因为切换客户端而放开剧情或出战保护。 */
	public boolean canEnterCustody() {
		return !unavailable() && !bound && !locked && partyPosition != null && partyPosition >= 4
				&& "CAPTURE".equals(originKind) && !source.startsWith("TUTORIAL:");
	}

	/** 放生只释放占位，保留来源、物种和已发现特性，防止历史来源重新发宠。 */
	@Column(name = "released_at")
	private Instant releasedAt;

}
