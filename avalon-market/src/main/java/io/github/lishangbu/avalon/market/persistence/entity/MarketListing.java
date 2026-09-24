package io.github.lishangbu.avalon.market.persistence.entity;

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
@Entity
@Table(name = "market_listing")
@Getter
@Setter
public class MarketListing {

	/** CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 卖家账号主键；资金结算和跨账号锁按账号编号处理。 */
	@Column(name = "seller_account_id", nullable = false)
	private Long sellerAccountId;

	/** 托管实物的实例主键；根据 kind 指向物品或伙伴，不建立跨模块实体关联。 */
	@Column(name = "asset_id", nullable = false)
	private Long assetId;

	/** 物品挂单各来源实例的剩余托管量；成交逐批交付，不合并奖励出处。 */
	@ElementCollection
	@CollectionTable(name = "market_listing_item", joinColumns = @JoinColumn(name = "listing_id"))
	@MapKeyColumn(name = "asset_id")
	@Column(name = "remaining", nullable = false)
	private Map<Long, Integer> itemAssets = new LinkedHashMap<>();

	/** 托管实物类型：ITEM 为物品或装备，CREATURE 为伙伴。 */
	@Column(nullable = false, length = 16)
	private String kind;

	/** 挂单状态：ACTIVE 在售、SOLD 售罄、RETURN_PENDING 待领退货、RETURNED 已领退货。 */
	@Column(nullable = false, length = 24)
	private String state;

	/** 每件商品的金币单价；成交按数量计算总价，再按规则扣税。 */
	@Column(name = "unit_price", nullable = false)
	private long unitPrice;

	/** 首次上架的总数量；部分成交不修改该历史值。 */
	@Column(name = "original_quantity", nullable = false)
	private int originalQuantity;

	/** 尚未售出或领取退货的托管数量；降为零后挂单进入终态。 */
	@Column(nullable = false)
	private int remaining;

	/** 首次成功创建时间，使用服务端时间保存；重试不覆盖。 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/** 上架时固定的失效时间；部分成交和重连不刷新期限。 */
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	/** 上架时从已核对实物建立查询索引；托管期间不能培养或修改实例属性。 */
	@Column(name = "item_code")
	private String itemCode;

	/** 上架时的物品名称或伙伴昵称，用于市场展示和名称检索。 */
	@Column(name = "item_name")
	private String itemName;

	/** 上架装备的部位编号；非装备为空，不能作为装备匹配。 */
	@Column(name = "item_slot")
	private String itemSlot;

	/** 上架装备的品质标识；没有品质的商品为空。 */
	@Column(name = "item_quality")
	private String itemQuality;

	/** 上架装备的穿戴等级或伙伴等级；按商品类型解释，用于范围筛选。 */
	@Column(name = "item_level")
	private Integer itemLevel;

	/** 检索分类：EQUIPMENT 装备、BOOK 技能书、FRAGMENT 碎片、MATERIAL 材料、CREATURE 伙伴。 */
	@Column(name = "item_category", length = 16)
	private String itemCategory;

	/** 上架伙伴是否为 H 隐藏特性槽位；非伙伴商品为空。 */
	@Column(name = "creature_hidden")
	private Boolean creatureHidden;

	/** 装备独有的上架时等级；非装备为空，不能混入“未强化装备”筛选。 */
	@Column(name = "enhancement_level")
	private Integer enhancementLevel;

	/** 技能精确匹配使用独立集合表，避免 JSON 子串把同名前缀技能误当作已学。 */
	@ElementCollection
	@CollectionTable(name = "market_listing_skill", joinColumns = @JoinColumn(name = "listing_id"))
	@Column(name = "skill", nullable = false)
	private Set<String> creatureSkills = new HashSet<>();

}
