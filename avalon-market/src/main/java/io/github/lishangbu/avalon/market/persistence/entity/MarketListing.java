package io.github.lishangbu.avalon.market.persistence.entity;

import org.hibernate.annotations.Comment;
import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 挂单保存托管数量和固定截止时间；部分成交只减少 remaining，不刷新期限。 实物保持所属模块管理，assetId 指向托管实例；跨模块不建立 JPA 关联。
 */
@DatabaseForeignKey(name = "market_listing_seller_fkey", columns = { "seller_account_id" }, referencedTable = "account",
		referencedColumns = { "id" })
@PartialIndex(name = "market_listing_asset_in_custody", columns = { "kind", "asset_id" },
		predicate = "((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RETURN_PENDING'::character varying])::text[]))",
		unique = true)
@Entity
@Table(name = "market_listing",
		comment = "挂单保存托管数量和固定截止时间；部分成交只减少 remaining，不刷新期限。 实物保持所属模块管理，assetId 指向托管实例；跨模块不建立 JPA 关联。",
		indexes = { @Index(name = "market_listing_expiry", columnList = "state, expires_at"),
				@Index(name = "market_listing_item_search", columnList = "state, item_category, unit_price, id"),
				@Index(name = "market_listing_seller_state", columnList = "seller_account_id, state") },
		check = { @CheckConstraint(name = "market_enhancement_range",
				constraint = "((enhancement_level IS NULL) OR (((kind)::text = 'ITEM'::text) AND ((item_category)::text = 'EQUIPMENT'::text) AND ((enhancement_level >= 0) AND (enhancement_level <= 7))))"),
				@CheckConstraint(name = "market_listing_values_check",
						constraint = "(((kind)::text = ANY ((ARRAY['ITEM'::character varying, 'CREATURE'::character varying])::text[])) AND ((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SOLD'::character varying, 'RETURN_PENDING'::character varying, 'RETURNED'::character varying])::text[])) AND (unit_price >= 2) AND (original_quantity > 0) AND ((remaining >= 0) AND (remaining <= original_quantity)) AND (expires_at > created_at) AND (((kind)::text <> 'CREATURE'::text) OR (original_quantity = 1)) AND ((((state)::text = ANY ((ARRAY['SOLD'::character varying, 'RETURNED'::character varying])::text[])) AND (remaining = 0)) OR (((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RETURN_PENDING'::character varying])::text[])) AND (remaining > 0))))") })
@Getter
@Setter
public class MarketListing {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。")
	private Long id;

	/** 卖家账号主键；资金结算和跨账号锁按账号编号处理。 */
	@Column(name = "seller_account_id", nullable = false, comment = "卖家账号主键；资金结算和跨账号锁按账号编号处理。")
	private Long sellerAccountId;

	/** 托管实物的实例主键；根据 kind 指向物品或伙伴，不建立跨模块实体关联。 */
	@Column(name = "asset_id", nullable = false, comment = "托管实物的实例主键；根据 kind 指向物品或伙伴，不建立跨模块实体关联。")
	private Long assetId;

	/** 物品挂单各来源实例的剩余托管量；成交逐批交付，不合并奖励出处。 */
	@ElementCollection
	@Comment("物品挂单按奖励来源分别保存托管实例与剩余数量")
	@Comment(on = "asset_id", value = "库存模块托管实例，保留原奖励出处")
	@CollectionTable(name = "market_listing_item",
			joinColumns = @JoinColumn(name = "listing_id", nullable = false, comment = "所属市场挂单"),
			foreignKey = @ForeignKey(name = "market_listing_item_listing_id_fkey"))
	@MapKeyColumn(name = "asset_id", nullable = false)
	@Column(name = "remaining", nullable = false, comment = "该来源尚未成交或领取退货的数量",
			check = @CheckConstraint(name = "market_listing_item_remaining_check", constraint = "remaining > 0"))
	private Map<Long, Integer> itemAssets = new LinkedHashMap<>();

	/** 托管实物类型：ITEM 为物品或装备，CREATURE 为伙伴。 */
	@Column(name = "kind", nullable = false, length = 16, comment = "托管实物类型：ITEM 为物品或装备，CREATURE 为伙伴。")
	private String kind;

	/** 挂单状态：ACTIVE 在售、SOLD 售罄、RETURN_PENDING 待领退货、RETURNED 已领退货。 */
	@Column(name = "state", nullable = false, length = 24,
			comment = "挂单状态：ACTIVE 在售、SOLD 售罄、RETURN_PENDING 待领退货、RETURNED 已领退货。")
	private String state;

	/** 每件商品的金币单价；成交按数量计算总价，再按规则扣税。 */
	@Column(name = "unit_price", nullable = false, comment = "每件商品的金币单价；成交按数量计算总价，再按规则扣税。")
	private long unitPrice;

	/** 首次上架的总数量；部分成交不修改该历史值。 */
	@Column(name = "original_quantity", nullable = false, comment = "首次上架的总数量；部分成交不修改该历史值。")
	private int originalQuantity;

	/** 尚未售出或领取退货的托管数量；降为零后挂单进入终态。 */
	@Column(name = "remaining", nullable = false, comment = "尚未售出或领取退货的托管数量；降为零后挂单进入终态。")
	private int remaining;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false, comment = "首次成功创建时间，使用服务端时间保存；重试不覆盖。")
	private Instant createdAt;

	/** 上架时固定的失效时间；部分成交和重连不刷新期限。 */
	@Column(name = "expires_at", nullable = false, comment = "上架时固定的失效时间；部分成交和重连不刷新期限。")
	private Instant expiresAt;

	/** 上架时从已核对实物建立查询索引；托管期间不能培养或修改实例属性。 */
	@Column(name = "item_code", length = 255, comment = "上架时从已核对实物建立查询索引；托管期间不能培养或修改实例属性。")
	private String itemCode;

	/** 上架时的物品名称或伙伴昵称，用于市场展示和名称检索。 */
	@Column(name = "item_name", length = 255, comment = "上架时的物品名称或伙伴昵称，用于市场展示和名称检索。")
	private String itemName;

	/** 上架装备的部位编号；非装备为空，不能作为装备匹配。 */
	@Column(name = "item_slot", length = 255, comment = "上架装备的部位编号；非装备为空，不能作为装备匹配。")
	private String itemSlot;

	/** 上架装备的品质标识；没有品质的商品为空。 */
	@Column(name = "item_quality", length = 255, comment = "上架装备的品质标识；没有品质的商品为空。")
	private String itemQuality;

	/** 上架装备的穿戴等级或伙伴等级；按商品类型解释，用于范围筛选。 */
	@Column(name = "item_level", comment = "上架装备的穿戴等级或伙伴等级；按商品类型解释，用于范围筛选。")
	private Integer itemLevel;

	/** 检索分类：EQUIPMENT 装备、BOOK 技能书、FRAGMENT 碎片、MATERIAL 材料、CREATURE 伙伴。 */
	@Column(name = "item_category", length = 16,
			comment = "检索分类：EQUIPMENT 装备、BOOK 技能书、FRAGMENT 碎片、MATERIAL 材料、CREATURE 伙伴。")
	private String itemCategory;

	/** 上架伙伴是否为 H 隐藏特性槽位；非伙伴商品为空。 */
	@Column(name = "creature_hidden", comment = "上架伙伴是否为 H 隐藏特性槽位；非伙伴商品为空。")
	private Boolean creatureHidden;

	/** 装备独有的上架时等级；非装备为空，不能混入“未强化装备”筛选。 */
	@Column(name = "enhancement_level", comment = "装备独有的上架时等级；非装备为空，不能混入“未强化装备”筛选。")
	private Integer enhancementLevel;

	/** 技能精确匹配使用独立集合表，避免 JSON 子串把同名前缀技能误当作已学。 */
	@ElementCollection
	@Comment("上架伙伴的技能检索集合；按完整技能名匹配，随挂单实物建立。")
	@CollectionTable(name = "market_listing_skill",
			joinColumns = @JoinColumn(name = "listing_id", nullable = false, comment = "对应市场挂单主键，挂单与技能名共同组成主键。"),
			foreignKey = @ForeignKey(name = "market_listing_skill_listing"))
	@Column(name = "skill", nullable = false, comment = "上架时已学技能的完整名称；用于精确匹配查询。")
	private Set<String> creatureSkills = new HashSet<>();

}
