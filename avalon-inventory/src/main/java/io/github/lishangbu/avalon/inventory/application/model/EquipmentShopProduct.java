package io.github.lishangbu.avalon.inventory.application.model;

import java.util.Map;

/**
 * EquipmentShopQueryService 使用的独立数据投影，包含code、name、slot、level等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param code 内容目录中的稳定代码
 * @param name 展示名称
 * @param slot 资产槽位
 * @param level 等级
 * @param quality 品质
 * @param price 价格
 * @param stats 属性数据
 */
public record EquipmentShopProduct(String code, String name, String slot, int level, String quality, int price,
		Map<String, Integer> stats) {
}
