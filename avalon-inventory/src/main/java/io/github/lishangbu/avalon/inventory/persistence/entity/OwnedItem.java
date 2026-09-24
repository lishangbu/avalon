package io.github.lishangbu.avalon.inventory.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * 按来源保存任务物品与装备实例；同一奖励来源唯一，防止重复发放。
 */
@Entity
@Table(name = "owned_item", uniqueConstraints = @UniqueConstraint(columnNames = { "character_id", "source" }))
@Getter
@Setter
public class OwnedItem {

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

	/** 原始发放凭证随转手或堆叠拆分保留，独立于本次取得来源；外部历史导入未知时允许为空。 */
	@Column(name = "origin_grant_id")
	private Long originGrantId;

	/**
	 * 本次取得来源，任务发放为步骤加效果序号，转手后由成交来源替换；原始出处由 originGrantId 保留。
	 */
	@Column(nullable = false)
	private String source;

	/**
	 * 内容目录中的物品编号。用于交付匹配；显示名称不能替代稳定编号参与业务判断。
	 */
	@Column(nullable = false)
	private String code;

	/**
	 * 实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * 当前数量，数据库约束禁止负数。交付后为零仍保留来源记录，不删除防重依据。
	 */
	@Column(nullable = false)
	private int quantity;

	/** 已固定但尚未进入背包的数量，不能消耗或穿戴；部分领取只在两个数量之间转移。 */
	@Column(name = "pending_quantity", nullable = false)
	private int pendingQuantity;

	/** 市场托管数量独立于可用和普通待领库存，任何普通消耗入口都不能使用。 */
	@Column(name = "custody_quantity", nullable = false)
	private int custodyQuantity;

	/**
	 * 装备部位；任务物品为 null，不能通过穿戴命令转换为装备。
	 */
	private String slot;

	/**
	 * 穿戴所需人物等级，校验时读取当前持锁角色等级而非客户端值。
	 */
	@Column(name = "required_level", nullable = false)
	private int requiredLevel;

	/**
	 * 装备原始属性的 JSON 存储文本，强化不能覆盖此值；展示和穿戴按强化等级重新计算。
	 */
	@Column(nullable = false)
	private String stats;

	/** 当前强化等级独立保存；失败掉级仍从原始属性计算，不累乘或反向除算。 */
	@Column(name = "enhancement_level", nullable = false)
	private int enhancementLevel;

	/**
	 * 是否占用装备部位。切换装备须先 flush 卸下记录，再穿戴新实例以满足部分唯一索引。
	 */
	@Column(nullable = false)
	private boolean equipped;

	/**
	 * 实例绑定状态；由实际来源和培养消耗决定，转手不能解除绑定。
	 */
	@Column(nullable = false)
	private boolean bound;

	/** 获得时固定的品质标识，不另乘属性；卖价由来源决定，零表示不允许出售。 */
	private String quality;

	/** 该实例出售给 NPC 的金币价格；按取得来源固定，零表示禁止出售，不是市场售价。 */
	@Column(name = "sell_price", nullable = false)
	private int sellPrice;

}
