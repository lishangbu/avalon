package io.github.lishangbu.avalon.character.application.model;

/**
 * FriendshipQueryService 使用的独立数据投影，包含id、character、name、profession等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param character 角色数据或角色引用
 * @param name 展示名称
 * @param profession 职业代码
 * @param status 当前业务状态
 */
public record FriendshipEntry(String id, String character, String name, String profession, String status) {
}
