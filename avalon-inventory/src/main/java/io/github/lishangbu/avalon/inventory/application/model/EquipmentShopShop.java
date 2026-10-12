package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/**
 * EquipmentShopQueryService 使用的独立数据投影，包含available、products等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param available 当前是否满足可用条件
 * @param products 产物声明列表
 */
public record EquipmentShopShop(boolean available, List<EquipmentShopProduct> products) {
}
