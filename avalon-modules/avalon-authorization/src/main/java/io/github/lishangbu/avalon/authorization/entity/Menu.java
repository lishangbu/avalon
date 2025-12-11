package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 菜单
 *
 * @author lishangbu
 * @since 2025/9/17
 */
@Data
public class Menu implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  private Long id;

  /** 父权限ID */
  private Long parentId;

  // region Naive UI Menu 属性

  /** 是否禁用菜单项 */
  private Boolean disabled;

  /** 菜单项的额外部分 */
  private String extra;

  /** 菜单项的图标 */
  private String icon;

  /** 菜单项的标识符 */
  private String key;

  /** 菜单项的内容 */
  private String label;

  /** 是否显示菜单项 */
  private Boolean show;

  // endregion

  // region Vue Router 属性

  /** 路径 */
  private String path;

  /** 名称 */
  private String name;

  /** 重定向路径 */
  private String redirect;

  /** 组件路径 */
  private String component;

  /** 排序顺序 */
  private Integer sortOrder;

  // endregion

  // region 其他的 router metadata

  /** 固定标签页 */
  private Boolean pinned;

  /** 显示标签页 */
  private Boolean showTab;

  /** 多标签页显示 */
  private Boolean enableMultiTab;

  // endregion
}
