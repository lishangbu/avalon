package io.github.lishangbu.avalon.mybatis.id;

import lombok.Getter;

/**
 * 生成ID类型枚举类
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Getter
public enum IdType {

  /** flex ID */
  FLEX,

  /** UUID */
  UUID,

  UNKNOWN
}
