package io.github.lishangbu.avalon.inventory.application.model;

/**
 * BookRecyclingQueryService 使用的独立数据投影，包含code、name、unitPrice、total等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param code 内容目录中的稳定代码
 * @param name 展示名称
 * @param unitPrice 单价
 * @param maximum 范围上界
 */
public record BookRecyclingOffer(String code, String name, int unitPrice, long total, long unbound, long maximum) {
}
