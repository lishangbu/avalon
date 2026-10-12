package io.github.lishangbu.avalon.creature.application.model;

/**
 * LearningView 使用的独立数据投影，包含id、name、quantity、bound等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param quantity 物品或资源数量
 * @param bound 资产是否绑定
 */
public record LearningViewBook(String id, String name, int quantity, boolean bound) {
}
