package io.github.lishangbu.avalon.market.application.model;

import io.github.lishangbu.avalon.content.item.model.EquipmentSetDefinition;
import io.github.lishangbu.avalon.creature.application.model.CreatureCustodyDetail;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;

/**
 * MarketQueryService 使用的独立数据投影，包含listing、item、sets、own等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param listing 市场挂牌投影
 * @param item 物品代码或物品数据
 * @param own 是否属于本人
 */
public record MarketDetail(MarketListingView listing, InventoryItemView item, List<EquipmentSetDefinition> sets,
		boolean own, CreatureCustodyDetail creature) {
}
