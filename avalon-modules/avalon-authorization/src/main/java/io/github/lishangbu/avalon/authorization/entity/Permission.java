package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 权限(Permission)实体类
 *
 * @author lishangbu
 * @since 2025/08/28
 */
@Data
public class Permission implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  private Long id;

  /** 权限名称 */
  private String name;

  /** 权限编码 */
  private String code;

  /** 权限类型,MENU-菜单,BUTTON-按钮 */
  private String type;

  /** 父权限ID */
  private Long parentId;

  /** 路径 */
  private String path;

  /** 重定向路径 */
  private String redirect;

  /** 图标 */
  private String icon;

  /** 组件路径 */
  private String component;

  /** 布局 */
  private String layout;

  /** 是否保持活动状态 */
  private Boolean keepAlive;

  /** 请求方法 */
  private String method;

  /** 描述 */
  private String description;

  /** 是否展示在页面菜单 */
  private Boolean show;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序 */
  private Integer orderNum;
}
