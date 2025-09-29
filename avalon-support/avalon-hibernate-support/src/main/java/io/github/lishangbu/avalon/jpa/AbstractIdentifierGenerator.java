package io.github.lishangbu.avalon.jpa;

import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

/**
 * 主键生成器抽象基类
 *
 * <p>提供主键生成通用逻辑：如实体主键已赋值则直接返回，否则调用生成方法 适用于自定义主键生成策略的场景，简化主键生成器实现 使用Hibernate内置API获取实体ID，性能更优且更准确
 *
 * <p>使用场景： 适用于分布式ID、雪花ID等主键生成需求
 *
 * <p>
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
    Object id = getIdValue(session, object);
    if (id != null) {
      return (Serializable) id;
    }
    return doGenerate(session, object);
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
        return persister.getIdentifier(object, session);
      }
    } catch (Exception e) {
      // 如果通过Hibernate API获取失败，可以在这里添加日志记录
    }

    return null;
  }
}
