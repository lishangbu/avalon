package io.github.lishangbu.avalon.authorization.entity;

import lombok.Data;

/**
 * 中间表实体，使用 AggregateReference 表示引用 Role 聚合的 id（不会自动加载 Role）
 *
 * @author lishangbu
 * @since 2025/11/21
 */
@Data
public class UserRoleRelation {

  private Long userId;

  private Long roleId;
}
