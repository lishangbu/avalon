package io.github.lishangbu.avalon.mybatis.id.generator;

import io.github.lishangbu.avalon.mybatis.id.Id;
import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID生成工厂
 *
 * @author lishangbu
 * @since 2025/8/20
 */
public class IdentifierGeneratorFactory {

  // 使用 ConcurrentHashMap 存储 ID 生成器，线程安全
  private static final Map<IdType, IdentifierGenerator> GENERATOR_MAP = new ConcurrentHashMap<>();

  static {
    // 注册FLEX ID 生成器
    registerIdentifierGenerator(new FlexIdentifierGenerator());
    // 注册UUID 生成器
    registerIdentifierGenerator(new UuidentifierGenerator());
  }

  /**
   * 获取对应 ID 类型的生成器
   *
   * @param idType ID 类型
   * @return 对应的 ID 生成器
   */
  public static IdentifierGenerator getIdentifierGenerator(IdType idType) {
    // 若 ID 类型没有注册对应的生成器，返回 null
    return GENERATOR_MAP.get(idType);
  }

  /**
   * 根据字段和实体生成 ID
   *
   * @param idField ID 注解的字段
   * @param entity 实体对象
   * @return 生成的 ID
   */
  public static Serializable nextId(Field idField, Object entity) {
    // 获取字段的 ID 注解，并获取 ID 类型
    IdType idType = idField.getAnnotation(Id.class).type();

    // 从缓存中获取对应的 ID 生成器
    IdentifierGenerator generator = GENERATOR_MAP.get(idType);
    if (generator == null) {
      throw new IllegalStateException("未注册 ID 生成器，ID 类型:" + idType);
    }

    // 返回生成的 ID
    return generator.nextId(idField, entity);
  }

  /**
   * 注册 ID 生成器
   *
   * @param identifierGenerator ID 生成器
   */
  public static void registerIdentifierGenerator(IdentifierGenerator identifierGenerator) {
    // 如果 ID 类型尚未注册生成器，则注册
    GENERATOR_MAP.computeIfAbsent(identifierGenerator.getIdType(), key -> identifierGenerator);
  }
}
