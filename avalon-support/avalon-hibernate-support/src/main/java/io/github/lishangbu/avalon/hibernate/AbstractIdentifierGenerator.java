package io.github.lishangbu.avalon.hibernate;

import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

/// 主键生成器抽象基类
///
/// - 封装通用主键生成流程：实体已有主键直接返回，否则交由 {@link #doGenerate} 生成
/// - 使用 Hibernate {@link EntityPersister} 获取标识符，优于手动反射，支持复合主键
/// - 适用于分布式 ID、雪花 ID 等主键策略，降低子类实现复杂度
///
/// 使用说明：
/// 1. 通过 Hibernate API 判断实体主键是否已赋值
/// 2. 若无则调用 {@link #doGenerate} 生成
///
/// @author lishangbu
/// @since 2025/9/30
@Slf4j
public abstract class AbstractIdentifierGenerator implements IdentifierGenerator {

  /// 生成主键（如已有主键则直接返回）
  ///
  /// 使用Hibernate内置API检查实体主键字段是否已赋值，若无则调用 doGenerate 生成主键
  ///
  /// @param session Hibernate 会话上下文
  /// @param object  当前持久化的实体对象
  /// @return 主键值
  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    if (object == null) {
      log.warn("生成主键时实体对象为 null，直接返回 null");
      return null;
    }
    Object id = getIdValue(session, object);
    if (id != null) {
      log.debug("实体 [{}] 已存在主键: [{}]，直接返回", object.getClass().getSimpleName(), id);
      return (Serializable) id;
    }
    Serializable generatedId = doGenerate(session, object);
    log.debug("实体 [{}] 生成新主键: [{}]", object.getClass().getSimpleName(), generatedId);
    return generatedId;
  }

  /// 实际主键生成逻辑，由子类实现
  ///
  /// @param session Hibernate 会话上下文
  /// @param object  当前持久化的实体对象
  /// @return 生成的主键值
  protected abstract Serializable doGenerate(
      SharedSessionContractImplementor session, Object object);

  /// 使用Hibernate API获取实体主键字段的值
  ///
  /// 通过 EntityPersister 获取实体的标识符值，这是 Hibernate 官方推荐的方式 比手动反射更高效、更准确，且能正确处理复合主键等复杂情况
  ///
  /// @param session Hibernate 会话上下文
  /// @param object  实体对象
  /// @return 主键值或 null
  protected Object getIdValue(SharedSessionContractImplementor session, Object object) {
    if (object == null) return null;

    try {
      // 获取实体的持久化器
      EntityPersister persister = session.getEntityPersister(null, object);
      if (persister != null) {
        // 使用Hibernate API获取实体的标识符值
        Object id = persister.getIdentifier(object, session);
        if (id != null) {
          log.debug("通过Hibernate API获取到实体 [{}] 的主键: [{}]", object.getClass().getSimpleName(), id);
        } else {
          log.debug("实体 [{}] 主键为空", object.getClass().getSimpleName());
        }
        return id;
      }
    } catch (Exception e) {
      log.warn("获取实体 [{}] 主键时发生异常: {}", object.getClass().getSimpleName(), e.getMessage());
    }
    return null;
  }
}
