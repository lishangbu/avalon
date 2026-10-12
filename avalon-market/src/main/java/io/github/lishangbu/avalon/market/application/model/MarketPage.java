package io.github.lishangbu.avalon.market.application.model;

import java.util.List;

/**
 * MarketQueryService 使用的独立数据投影，包含page、size、hasMore、listings等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param page 分页序号
 * @param size 每页数量
 * @param hasMore 是否还存在后续数据
 * @param listings 市场挂牌列表
 */
public record MarketPage(int page, int size, boolean hasMore, List<MarketListingView> listings) {
}
