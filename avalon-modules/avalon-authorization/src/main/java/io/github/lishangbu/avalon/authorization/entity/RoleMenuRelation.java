package io.github.lishangbu.avalon.authorization.entity;

import lombok.Data;

/// 中间表实体，使用 AggregateReference 表示引用 Menu 聚合的 id（不会自动加载 Menu）
///
/// @author lishangbu
/// @since 2025/12/4
@Data
public class RoleMenuRelation {

  private Long roleId;

  private Long menuId;
}
