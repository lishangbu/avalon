package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/// 菜单实体
///
/// 表示前端菜单项的属性，包含 Naive UI 与 Vue Router 所需的字段
///
/// @author lishangbu
/// @since 2025/9/17
@Data
@Entity
@Table(comment = "菜单表，存储前端菜单项的属性，包括 Naive UI 和 Vue Router 所需的字段")
public class Menu implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "菜单 ID")
  private Long id;

  /// 父菜单 ID
  @Column(comment = "父菜单 ID")
  private Long parentId;

  // region Naive UI Menu 属性

  /// 是否禁用菜单项
  @Column(comment = "是否禁用菜单项 true: 禁用菜单项，false: 启用菜单项")
  private Boolean disabled;

  /// 菜单项的额外部分
  @Column(comment = "菜单项的额外部分，存储为 JSON 格式")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> extra;

  /// 菜单项的图标
  @Column(comment = "菜单项的图标")
  private String icon;

  /// 菜单项的标识符
  @Column(comment = "菜单项的标识符")
  private String key;

  /// 菜单项的内容
  @Column(comment = "菜单项的内容")
  private String label;

  /// 是否显示菜单项
  @Column(comment = "是否显示菜单项 true: 显示菜单项，false: 隐藏菜单项")
  private Boolean show;

  // endregion

  // region Vue Router 属性

  /// 路径
  @Column(comment = "菜单项的路由路径")
  private String path;

  /// 名称
  @Column(comment = "菜单项的路由名称")
  private String name;

  /// 重定向路径
  @Column(comment = "菜单项的重定向路径")
  private String redirect;

  /// 组件路径
  @Column(comment = "菜单项的组件路径")
  private String component;

  /// 排序顺序
  @Column(comment = "菜单项的排序顺序，数字越小越靠前")
  private Integer sortOrder;

  // endregion

  // region 其他的 router metadata

  /// 固定标签页
  @Column(comment = "是否固定标签页 true: 固定标签页，false: 不固定")
  private Boolean pinned;

  /// 显示标签页
  @Column(comment = "是否显示标签页 true: 显示标签页，false: 不显示")
  private Boolean showTab;

  /// 多标签页显示
  @Column(comment = "是否启用多标签页显示 true: 启用，false: 不启用")
  private Boolean enableMultiTab;

  // endregion
}
