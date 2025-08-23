package io.github.lishangbu.avalon.mybatis.id.generator;

import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * ID 生成器接口
 *
 * @author lishangbu
 * @since 2025/8/20
 */
public interface IdentifierGenerator {

  /**
   * 生成Id
   *
   * @param field 字段
   * @param entity 实体
   * @return id
   */
  Serializable nextId(Field field, Object entity);

  /**
   * 获取ID类型
   *
   * @return idType id类型
   */
  IdType getIdType();
}
