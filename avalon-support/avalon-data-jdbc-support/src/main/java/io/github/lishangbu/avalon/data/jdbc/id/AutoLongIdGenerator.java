package io.github.lishangbu.avalon.data.jdbc.id;

import java.io.Serializable;

/**
 * 标记自动生成Long类型ID的接口
 *
 * @author lishangbu
 * @since 2025/8/10
 */
public interface AutoLongIdGenerator extends Serializable {
  /**
   * 获取主键标识
   *
   * @return 主键标识
   */
  Long getId();

  /**
   * 设置主键标识
   *
   * @param id 主键标识
   */
  void setId(Long id);
}
