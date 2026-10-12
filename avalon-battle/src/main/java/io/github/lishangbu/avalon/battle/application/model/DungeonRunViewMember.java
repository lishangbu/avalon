package io.github.lishangbu.avalon.battle.application.model;

/**
 * DungeonRunView 使用的独立数据投影，包含id、name、profession、level等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param profession 职业代码
 * @param level 等级
 * @param location 地点代码
 * @param ready 是否已准备好
 */
public record DungeonRunViewMember(String id, String name, String profession, int level, int location, boolean ready) {
}
