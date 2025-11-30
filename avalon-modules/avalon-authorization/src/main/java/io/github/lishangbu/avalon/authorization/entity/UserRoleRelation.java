package io.github.lishangbu.avalon.authorization.entity;

import lombok.Data;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 中间表实体，使用 AggregateReference 表示引用 Role 聚合的 id（不会自动加载 Role）
 *
 * @author lishangbu
 * @since 2025/11/21
 */
@Data
@Table
public class UserRoleRelation {

  @Column("user_id")
  private Long userId;

  @Column("role_id")
  private AggregateReference<Role, Long> roleRef;
}
