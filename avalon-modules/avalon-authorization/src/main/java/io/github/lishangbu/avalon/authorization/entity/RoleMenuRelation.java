package io.github.lishangbu.avalon.authorization.entity;

import lombok.Data;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 中间表实体，使用 AggregateReference 表示引用 Menu 聚合的 id（不会自动加载 Menu）
 *
 * @author lishangbu
 * @since 2025/12/4
 */
@Data
@Table
public class RoleMenuRelation {

  @Column("role_id")
  private Integer roleId;

  @Column("menu_id")
  private AggregateReference<Menu, Integer> menuRef;
}
