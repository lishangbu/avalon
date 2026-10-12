package io.github.lishangbu.avalon.market.application.model;

import java.time.Instant;

/**
 * MarketQueryService 使用的独立数据投影，包含id、assetId、kind、state等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param assetId 关联资产实例编号
 * @param kind 业务种类
 * @param state 当前规则状态
 * @param unitPrice 单价
 * @param originalQuantity 原始挂牌数量
 * @param remaining 剩余数量或余额
 * @param expiresAt 失效时刻
 * @param name 展示名称
 * @param category 物品分类
 * @param enhancementLevel 装备强化等级
 */
public record MarketListingView(String id, String assetId, String kind, String state, String unitPrice,
		int originalQuantity, int remaining, Instant expiresAt, String name, String category,
		Integer enhancementLevel) {
}
