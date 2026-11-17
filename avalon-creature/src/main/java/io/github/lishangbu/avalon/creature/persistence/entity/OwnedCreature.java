package io.github.lishangbu.avalon.creature.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
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
@DatabaseForeignKey(name = "creature_captor_character_fkey", columns = { "captor_character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "creature_origin_character_fkey", columns = { "origin_character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "owned_creature_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "owned_creature", comment = "契约宠物实例，特性槽位在生成时确定，技能独立保存以便后续学习与遗忘。",
		uniqueConstraints = {
				@UniqueConstraint(name = "owned_creature_character_id_party_position_key",
						columnNames = { "character_id", "party_position" }),
				@UniqueConstraint(name = "owned_creature_character_id_source_key",
						columnNames = { "character_id", "source" }) },
		indexes = {
				@Index(name = "creature_origin_source", columnList = "origin_character_id, source", unique = true) },
		check = { @CheckConstraint(name = "creature_origin_kind_check",
				constraint = "((origin_kind)::text = ANY ((ARRAY['UNKNOWN'::character varying, 'GIFT'::character varying, 'CAPTURE'::character varying])::text[]))"),
				@CheckConstraint(name = "creature_release_position",
						constraint = "(((released_at IS NULL) AND (NOT in_custody) AND (party_position IS NOT NULL)) OR ((released_at IS NOT NULL) AND (NOT in_custody) AND (party_position IS NULL)) OR ((released_at IS NULL) AND in_custody AND (party_position IS NULL) AND (NOT bound) AND (NOT locked)))"),
				@CheckConstraint(name = "owned_creature_level_check", constraint = "((level >= 1) AND (level <= 50))"),
				@CheckConstraint(name = "owned_creature_rename_count_check", constraint = "(rename_count >= 0)") })
@Getter
@Setter
public class OwnedCreature {

	/**
	 * CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。
	 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。")
	private Long id;

	/**
	 * 所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。
	 */
	@Column(name = "character_id", nullable = false, comment = "所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。")
	private Long characterId;

	/** 首次获得者不随交易变更，用于保持来源防重；未记录时为空，不猜测身份。 */
	@Column(name = "origin_character_id", comment = "首次获得者不随交易变更，用于保持来源防重。来源身份独立于当前所有权。")
	private Long originCharacterId;

	/** 仅亲自捕获才保存捕获者；任务赠宠没有捕获者，不能把当前持有者冒充捕获者。 */
	@Column(name = "captor_character_id", comment = "仅亲自捕获才保存捕获者；任务赠宠没有捕获者，不能把当前持有者冒充捕获者。")
	private Long captorCharacterId;

	/** 初始来源类别在创建时固定；进化、交易不覆盖原始来源。 */
	@ColumnDefault("'UNKNOWN'::character varying")
	@Column(name = "origin_kind", nullable = false, length = 16, comment = "初始来源类别在创建时固定；进化、交易不覆盖原始来源。")
	private String originKind = "UNKNOWN";

	/** 未知来源默认绑定；捕获入口明确设为未绑定，后续绑定材料只允许将其改为绑定。 */
	@ColumnDefault("true")
	@Column(name = "bound", nullable = false, comment = "未知来源默认绑定；捕获入口明确设为未绑定，后续绑定材料只允许将其改为绑定。")
	private boolean bound = true;

	/** 显式记录是否自定义过昵称，进化不能凭名称与默认名相等就误覆盖玩家昵称。 */
	@ColumnDefault("false")
	@Column(name = "custom_name", nullable = false, comment = "显式记录是否自定义过昵称，进化不能凭名称与默认名相等就误覆盖玩家昵称。")
	private boolean customName;

	/**
	 * 奖励来源幂等键，当前为步骤编号加效果顺序；同一角色与来源组合只能生成一次实例。
	 */
	@Column(name = "source", nullable = false, length = 255, comment = "奖励来源幂等键，当前为步骤编号加效果顺序；同一角色与来源组合只能生成一次实例。")
	private String source;

	/**
	 * 物种目录的稳定编号。用于关联模板，实例等级、技能和特性仍独立持久化。
	 */
	@Column(name = "species", nullable = false, length = 255, comment = "物种目录的稳定编号。用于关联模板，实例等级、技能和特性仍独立持久化。")
	private String species;

	/**
	 * 实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。
	 */
	@Column(name = "name", nullable = false, length = 255, comment = "实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。")
	private String name;

	/** 成功更名次数随个体保存；不由名称是否等于物种默认名推算免费资格。 */
	@ColumnDefault("0")
	@Column(name = "rename_count", nullable = false, comment = "成功更名次数随个体保存；不由名称是否等于物种默认名推算免费资格。")
	private int renameCount;

	/**
	 * 实例元素名称，来自启动校验后的模板。当前不在此实体中计算属性相克倍率。
	 */
	@Column(name = "element", nullable = false, length = 255, comment = "实例元素名称，来自启动校验后的模板。当前不在此实体中计算属性相克倍率。")
	private String element;

	/**
	 * 实例等级，赠宠创建时取模板值，不根据加入前人物经验追赶等级。
	 */
	@Column(name = "level", nullable = false, comment = "实例等级，赠宠创建时取模板值，不根据加入前人物经验追赶等级。")
	private int level;

	/**
	 * 累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。
	 */
	@Column(name = "experience", nullable = false, comment = "累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。")
	private long experience;

	/**
	 * 生成时确定的 A/B/H 槽位。保存槽位本身，以便后续进化维持槽位语义。
	 */
	@Column(name = "ability_slot", nullable = false, length = 255, comment = "生成时确定的 A/B/H 槽位。保存槽位本身，以便后续进化维持槽位语义。")
	private String abilitySlot;

	/**
	 * 该实例当前特性名称；不能仅凭是否隐藏槽位推断效果更强。
	 */
	@Column(name = "ability", nullable = false, length = 255, comment = "该实例当前特性名称；不能仅凭是否隐藏槽位推断效果更强。")
	private String ability;

	/**
	 * 实例技能列表的 JSON 文本，由持久化 codec 转换，顺序和内容不能由客户端任意覆盖。
	 */
	@Column(name = "skills", nullable = false, length = 255, comment = "实例技能列表的 JSON 文本，由持久化 codec 转换，顺序和内容不能由客户端任意覆盖。")
	private String skills;

	/**
	 * 从零开始的随身位置；同一角色不能重复占位，容量与出战数量是两种不同约束。
	 */
	@Column(name = "party_position", comment = "从零开始的随身位置；同一角色不能重复占位，容量与出战数量是两种不同约束。")
	private Integer partyPosition;

	/** 玩家主动保护的个体不能放生；锁定不限制正常编队和学习。 */
	@ColumnDefault("false")
	@Column(name = "locked", nullable = false, comment = "玩家主动保护的个体不能放生；锁定不限制正常编队和学习。")
	private boolean locked;

	/** 市场托管不占编队或仓库位置，禁止所有普通培养和管理命令访问。 */
	@ColumnDefault("false")
	@Column(name = "in_custody", nullable = false, comment = "市场托管不占编队或仓库位置，禁止所有普通培养和管理命令访问。")
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
	@Column(name = "released_at", comment = "放生只释放占位，保留来源、物种和已发现特性，防止历史来源重新发宠。")
	private Instant releasedAt;

}
