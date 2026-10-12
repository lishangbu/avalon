package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/**
 * BookRecyclingQueryService 使用的独立数据投影，包含available、offers等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param available 当前是否满足可用条件
 * @param offers 可用供应项
 */
public record BookRecyclingView(boolean available, List<BookRecyclingOffer> offers) {
}
