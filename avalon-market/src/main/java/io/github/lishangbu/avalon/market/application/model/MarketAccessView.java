package io.github.lishangbu.avalon.market.application.model;

/**
 * MarketQueryService 使用的独立数据投影，包含available、taught、activeListings、pendingReturns等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param available 当前是否满足可用条件
 * @param taught 是否已经学会
 * @param activeListings 有效挂牌数量
 * @param pendingReturns 是否还有待返还资产
 */
public record MarketAccessView(boolean available, boolean taught, long activeListings, boolean pendingReturns,
		int requiredLevel) {
}
