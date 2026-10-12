package io.github.lishangbu.avalon.inventory.application.model;

import java.time.Instant;

/**
 * DungeonChestQueryService 使用的独立数据投影，包含id、name、createdAt等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param createdAt 创建时刻
 */
public record DungeonChestView(String id, String name, Instant createdAt) {
}
