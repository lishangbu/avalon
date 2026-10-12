package io.github.lishangbu.avalon.inventory.application.model;

/**
 * BagView 使用的独立数据投影，包含key、code、name、category等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param code 内容目录中的稳定代码
 * @param name 展示名称
 * @param category 物品分类
 * @param bound 资产是否绑定
 * @param quantity 物品或资源数量
 */
public record BagViewStack(String key, String code, String name, String category, boolean bound, int quantity) {
}
