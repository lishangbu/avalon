package io.github.lishangbu.avalon.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

import java.io.Serializable;

/**
 * 主键生成器抽象基类
 *
 * <p>提供主键生成通用逻辑：如实体主键已赋值则直接返回，否则调用生成方法 适用于自定义主键生成策略的场景，简化主键生成器实现 使用Hibernate内置API获取实体ID，性能更优且更准确
 *
 * <p>使用场景： 适用于分布式ID、雪花ID等主键生成需求
 *
 * <ol>
 *   <li>使用Hibernate API检查实体主键字段是否已有值
 *   <li>若无值则生成主键
 * </ol>
 *
 * <p>
 *
 * @author lishangbu
 * @since 2025/9/30
 */
@Slf4j
public abstract class AbstractIdentifierGenerator implements IdentifierGenerator {

  /**
   * 生成主键（如已有主键则直接返回）
   *
   * <p>使用Hibernate内置API检查实体主键字段是否已赋值，若无则调用doGenerate生成主键
   *
   * @param session Hibernate会话上下文
   * @param object 当前持久化的实体对象
   * @return 主键值
   */
  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    if (object == null) {
      log.warn("生成主键时实体对象为 null，直接返回 null");
      return null;
    }
    Object id = getIdValue(session, object);
    if (id != null) {
      log.debug("实体 {} 已存在主键: {}，直接返回", object.getClass().getSimpleName(), id);
      return (Serializable) id;
    }
    Serializable generatedId = doGenerate(session, object);
    log.debug("实体 {} 生成新主键: {}", object.getClass().getSimpleName(), generatedId);
    return generatedId;
  }

  /**
   * 实际主键生成逻辑，由子类实现
   *
   * @param session Hibernate会话上下文
   * @param object 当前持久化的实体对象
   * @return 生成的主键值
   */
  protected abstract Serializable doGenerate(
      SharedSessionContractImplementor session, Object object);

  /**
   * 使用Hibernate API获取实体主键字段的值
   *
   * <p>通过EntityPersister获取实体的标识符值，这是Hibernate官方推荐的方式 比手动反射更高效、更准确，且能正确处理复合主键等复杂情况
   *
   * @param session Hibernate会话上下文
   * @param object 实体对象
   * @return 主键值或null
   */
  protected Object getIdValue(SharedSessionContractImplementor session, Object object) {
    if (object == null) return null;

    try {
      // 获取实体的持久化器
      EntityPersister persister = session.getEntityPersister(null, object);
      if (persister != null) {
        // 使用Hibernate API获取实体的标识符值
        Object id = persister.getIdentifier(object, session);
        if (id != null) {
          log.debug("通过Hibernate API获取到实体 {} 的主键: {}", object.getClass().getSimpleName(), id);
        } else {
          log.debug("实体 {} 主键为空", object.getClass().getSimpleName());
        }
        return id;
      }
    } catch (Exception e) {
      log.warn("获取实体 {} 主键时发生异常: {}", object.getClass().getSimpleName(), e.getMessage());
    }
    return null;
  }
}
