package io.github.lishangbu.avalon.hibernate;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * FlexId主键生成器
 *
 * <p>基于FlexKeyGenerator实现分布式唯一ID生成，适用于高并发和分布式场景 用于Hibernate实体主键自动生成，保证ID全局唯一且高性能
 * 通过单例模式获取生成器实例，支持多线程安全
 *
 * <p>使用场景： 适用于需要自定义主键生成策略的业务实体，如用户、菜单等
 *
 * <p>步骤： 1. 调用FlexKeyGenerator单例生成ID 2. 返回生成的唯一主键
 *
 * <p>
 *
 * @author lishangbu
 * @see io.github.lishangbu.avalon.keygen.FlexKeyGenerator
 * @see org.hibernate.id.IdentifierGenerator
 * @since 2025/9/14
 */
public class FlexIdentifierGenerator extends AbstractIdentifierGenerator {

  /**
   * 生成分布式唯一主键
   *
   * <p>如实体主键已赋值则直接返回，否则调用FlexKeyGenerator生成全局唯一ID
   *
   * @param session Hibernate会话上下文，提供数据库连接等信息
   * @param object 当前持久化的实体对象
   * @return 生成的唯一主键ID，类型与实体主键一致
   */
  @Override
  protected Serializable doGenerate(SharedSessionContractImplementor session, Object object) {
    return FlexKeyGenerator.getInstance().generate();
  }
}
