package io.github.lishangbu.avalon.hibernate;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// FlexId 主键生成器
///
/// - 基于 {@link FlexKeyGenerator} 实现分布式唯一 ID 生成，适用于高并发与分布式场景
/// - 通过单例模式获取生成器实例，支持多线程安全；用于 Hibernate 实体主键自动生成
/// - 适合需要自定义主键生成策略的业务实体，如用户、菜单等，生成的 ID 全局唯一
///
/// @author lishangbu
/// @see io.github.lishangbu.avalon.keygen.FlexKeyGenerator
/// @see org.hibernate.id.IdentifierGenerator
/// @since 2025/9/14
public class FlexIdentifierGenerator extends AbstractIdentifierGenerator {

  /// 生成分布式唯一主键，当实体主键已赋值时直接返回现有值
  ///
  /// @param session Hibernate 会话上下文，提供数据库连接等信息
  /// @param object 当前持久化的实体对象
  /// @return 生成的唯一主键 ID，类型与实体主键一致
  @Override
  protected Serializable doGenerate(SharedSessionContractImplementor session, Object object) {
    return FlexKeyGenerator.getInstance().generate();
  }
}
