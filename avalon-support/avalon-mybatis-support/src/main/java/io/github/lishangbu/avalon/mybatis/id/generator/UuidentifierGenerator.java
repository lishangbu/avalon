package io.github.lishangbu.avalon.mybatis.id.generator;

import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.UUID;
import org.springframework.util.Assert;

/**
 * uuid 生成器
 *
 * @author lishangbu
 * @since 2025/8/21
 */
public class UuidentifierGenerator implements IdentifierGenerator {
  @Override
  public Serializable nextId(Field field, Object entity) {
    Assert.isTrue(field.getType().isAssignableFrom(String.class), "UUID主键策略对应主键属性类型必须为String");
    return UUID.randomUUID().toString();
  }

  @Override
  public IdType getIdType() {
    return IdType.UUID;
  }
}
