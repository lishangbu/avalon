package io.github.lishangbu.avalon.hibernate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AbstractIdentifierGenerator测试类
 *
 * <p>测试主键生成器的核心逻辑： - 当实体主键已赋值时，直接返回现有主键 - 当实体主键未赋值时，调用子类生成新主键
 *
 * @author lishangbu
 * @since 2025/9/30
 */
@ExtendWith(MockitoExtension.class)
class AbstractIdentifierGeneratorTest {

  @Mock private SharedSessionContractImplementor session;

  @Mock private EntityPersister persister;

  private TestIdentifierGenerator generator;

  /** 测试用的具体主键生成器实现 */
  private static class TestIdentifierGenerator extends AbstractIdentifierGenerator {
    @Override
    protected Serializable doGenerate(SharedSessionContractImplementor session, Object object) {
      return 12345L; // 返回固定的测试ID
    }
  }

  /** 测试用的实体类 */
  private static class TestEntity {
    private Long id;

    public TestEntity() {}

    public TestEntity(Long id) {
      this.id = id;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }
  }

  @BeforeEach
  void setUp() {
    generator = new TestIdentifierGenerator();
  }

  @Test
  void testGenerateWhenIdAlreadyExists() {
    // 准备测试数据
    TestEntity entity = new TestEntity();
    Long existingId = 999L;

    // 模拟Hibernate API返回已存在的ID
    when(session.getEntityPersister(null, entity)).thenReturn(persister);
    when(persister.getIdentifier(entity, session)).thenReturn(existingId);

    // 执行测试
    Serializable result = generator.generate(session, entity);

    // 验证结果
    assertEquals(existingId, result, "当实体已有主键时，应直接返回现有主键");
    verify(session).getEntityPersister(null, entity);
    verify(persister).getIdentifier(entity, session);
  }

  @Test
  void testGenerateWhenIdIsNull() {
    // 准备测试数据
    TestEntity entity = new TestEntity();

    // 模拟Hibernate API返回null（无主键）
    when(session.getEntityPersister(null, entity)).thenReturn(persister);
    when(persister.getIdentifier(entity, session)).thenReturn(null);

    // 执行测试
    Serializable result = generator.generate(session, entity);

    // 验证结果
    assertEquals(12345L, result, "当实体无主键时，应调用doGenerate生成新主键");
    verify(session).getEntityPersister(null, entity);
    verify(persister).getIdentifier(entity, session);
  }

  @Test
  void testGenerateWhenEntityIsNull() {
    // 执行测试
    Serializable result = generator.generate(session, null);

    // 验证结果
    assertNull(result, "当实体为null时，应生成null");
    verify(session, never()).getEntityPersister(any(), any());
  }

  @Test
  void testGenerateWhenPersisterIsNull() {
    // 准备测试数据
    TestEntity entity = new TestEntity();

    // 模拟获取不到EntityPersister
    when(session.getEntityPersister(null, entity)).thenReturn(null);

    // 执行测试
    Serializable result = generator.generate(session, entity);

    // 验证结果
    assertEquals(12345L, result, "当获取不到EntityPersister时，应调用doGenerate生成新主键");
    verify(session).getEntityPersister(null, entity);
  }

  @Test
  void testGenerateWhenHibernateApiThrowsException() {
    // 准备测试数据
    TestEntity entity = new TestEntity();

    // 模拟Hibernate API抛出异常
    when(session.getEntityPersister(null, entity)).thenThrow(new RuntimeException("模拟异常"));

    // 执行测试
    Serializable result = generator.generate(session, entity);

    // 验证结果
    assertEquals(12345L, result, "当Hibernate API抛出异常时，应调用doGenerate生成新主键");
    verify(session).getEntityPersister(null, entity);
  }

  @Test
  void testGetIdValueWithValidEntity() {
    // 准备测试数据
    TestEntity entity = new TestEntity();
    Long expectedId = 888L;

    // 模拟Hibernate API返回ID
    when(session.getEntityPersister(null, entity)).thenReturn(persister);
    when(persister.getIdentifier(entity, session)).thenReturn(expectedId);

    // 执行测试
    Object result = generator.getIdValue(session, entity);

    // 验证结果
    assertEquals(expectedId, result, "应正确返回实体的主键值");
  }

  @Test
  void testGetIdValueWithNullEntity() {
    // 执行测试
    Object result = generator.getIdValue(session, null);

    // 验证结果
    assertNull(result, "当实体为null时，应返回null");
    verify(session, never()).getEntityPersister(any(), any());
  }

  @Test
  void testGetIdValueWhenExceptionOccurs() {
    // 准备测试数据
    TestEntity entity = new TestEntity();

    // 模拟异常
    when(session.getEntityPersister(null, entity)).thenThrow(new RuntimeException("模拟异常"));

    // 执行测试
    Object result = generator.getIdValue(session, entity);

    // 验证结果
    assertNull(result, "当发生异常时，应返回null");
  }

  @Test
  void testDoGenerateIsAbstract() {
    // 验证doGenerate是抽象方法，通过TestIdentifierGenerator的实现来测试
    TestIdentifierGenerator testGenerator = new TestIdentifierGenerator();
    Serializable result = testGenerator.doGenerate(session, new TestEntity());

    assertEquals(12345L, result, "doGenerate方法应由子类实现并返回生成的主键");
  }
}
