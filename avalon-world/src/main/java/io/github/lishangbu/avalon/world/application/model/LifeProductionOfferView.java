package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * LifeProductionQueryService 使用的独立数据投影，包含item、name、price、locations等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param item 物品代码或物品数据
 * @param name 展示名称
 * @param price 价格
 * @param locations 允许的地点代码
 * @param available 当前是否满足可用条件
 */
public record LifeProductionOfferView(String item, String name, int price, List<String> locations, boolean available) {
}
