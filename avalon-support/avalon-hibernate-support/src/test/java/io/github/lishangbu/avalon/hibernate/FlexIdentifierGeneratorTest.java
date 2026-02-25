package io.github.lishangbu.avalon.hibernate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/// FlexIdentifierGenerator测试类
///
/// 测试FlexId主键生成器的核心功能：
/// - 当实体主键已赋值时，直接返回现有主键，不调用FlexKeyGenerator
/// - 当实体主键未赋值时，调用FlexKeyGenerator生成分布式唯一ID
/// - 验证与FlexKeyGenerator的正确集成
///
/// @author lishangbu
/// @since 2025/9/30
@ExtendWith(MockitoExtension.class)
class FlexIdentifierGeneratorTest {

    @Mock private SharedSessionContractImplementor session;

    @Mock private EntityPersister persister;

    @Mock private FlexKeyGenerator flexKeyGenerator;

    private FlexIdentifierGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new FlexIdentifierGenerator();
    }

    /// 测试当实体已有主键时，直接返回现有主键
    /// 输入：实体对象，已有ID
    /// 预期：返回现有ID，不调用FlexKeyGenerator
    @Test
    void testGenerateWhenEntityHasExistingId() {
        // 准备测试数据
        TestEntity entity = new TestEntity();
        Long existingId = 888L;

        // 模拟Hibernate API返回已存在的ID
        when(session.getEntityPersister(null, entity)).thenReturn(persister);
        when(persister.getIdentifier(entity, session)).thenReturn(existingId);

        // 使用MockedStatic来模拟静态方法调用
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);

            // 执行测试
            Serializable result = generator.generate(session, entity);

            // 验证结果
            assertEquals(existingId, result, "当实体已有主键时，应直接返回现有主键");

            // 验证FlexKeyGenerator未被调用
            mockedStatic.verify(FlexKeyGenerator::getInstance, never());
            verify(flexKeyGenerator, never()).generate();

            // 验证Hibernate API被正确调用
            verify(session).getEntityPersister(null, entity);
            verify(persister).getIdentifier(entity, session);
        }
    }

    /// 测试当实体无主键时，调用FlexKeyGenerator生成新主键
    /// 输入：实体对象，无ID
    /// 预期：调用FlexKeyGenerator生成ID
    @Test
    void testGenerateWhenEntityHasNoId() {
        // 准备测试数据
        TestEntity entity = new TestEntity();
        Long generatedId = 12345L;

        // 模拟Hibernate API返回null（无主键）
        when(session.getEntityPersister(null, entity)).thenReturn(persister);
        when(persister.getIdentifier(entity, session)).thenReturn(null);

        // 模拟FlexKeyGenerator生成ID
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);
            when(flexKeyGenerator.generate()).thenReturn(generatedId);

            // 执行测试
            Serializable result = generator.generate(session, entity);

            // 验证结果
            assertEquals(generatedId, result, "当实体无主键时，应调用FlexKeyGenerator生成新主键");

            // 验证FlexKeyGenerator被正确调用
            mockedStatic.verify(FlexKeyGenerator::getInstance, times(1));
            verify(flexKeyGenerator, times(1)).generate();

            // 验证Hibernate API被正确调用
            verify(session).getEntityPersister(null, entity);
            verify(persister).getIdentifier(entity, session);
        }
    }

    /// 当实体对象为 null 时，主键生成器应直接返回 null
    ///
    /// 验证不会调用 FlexKeyGenerator 和 Hibernate EntityPersister
    @Test
    void testGenerateWhenEntityIsNull() {
        // 执行测试
        Serializable result = generator.generate(session, null);

        // 断言返回值为 null
        assertNull(result, "实体为 null 时应直接返回 null");

        // 验证 FlexKeyGenerator 未被调用
        verify(flexKeyGenerator, never()).generate();
        // 验证不会调用 Hibernate EntityPersister
        verify(session, never()).getEntityPersister(any(), any());
    }

    /// 测试当Hibernate API抛出异常时，调用FlexKeyGenerator生成新主键
    /// 输入：实体对象，API抛出异常
    /// 预期：调用FlexKeyGenerator生成ID
    @Test
    void testGenerateWhenHibernateApiThrowsException() {
        // 准备测试数据
        TestEntity entity = new TestEntity();
        Long generatedId = 55555L;

        // 模拟Hibernate API抛出异常
        when(session.getEntityPersister(null, entity))
                .thenThrow(new RuntimeException("模拟Hibernate异常"));

        // 模拟FlexKeyGenerator生成ID
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);
            when(flexKeyGenerator.generate()).thenReturn(generatedId);

            // 执行测试
            Serializable result = generator.generate(session, entity);

            // 验证结果
            assertEquals(generatedId, result, "当Hibernate API抛出异常时，应调用FlexKeyGenerator生成新主键");

            // 验证FlexKeyGenerator被正确调用
            mockedStatic.verify(FlexKeyGenerator::getInstance, times(1));
            verify(flexKeyGenerator, times(1)).generate();
        }
    }

    /// 测试doGenerate方法直接调用FlexKeyGenerator
    /// 输入：实体对象
    /// 预期：返回生成的ID
    @Test
    void testDoGenerateDirectly() {
        // 准备测试数据
        TestEntity entity = new TestEntity();
        Long generatedId = 77777L;

        // 模拟FlexKeyGenerator生成ID
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);
            when(flexKeyGenerator.generate()).thenReturn(generatedId);

            // 直接测试doGenerate方法
            Serializable result = generator.doGenerate(session, entity);

            // 验证结果
            assertEquals(generatedId, result, "doGenerate方法应调用FlexKeyGenerator生成主键");

            // 验证FlexKeyGenerator被正确调用
            mockedStatic.verify(FlexKeyGenerator::getInstance, times(1));
            verify(flexKeyGenerator, times(1)).generate();
        }
    }

    /// 测试FlexKeyGenerator单例使用
    /// 输入：实体对象，无ID
    /// 预期：每次调用生成不同ID
    @Test
    void testFlexKeyGeneratorSingletonUsage() {
        // 准备测试数据
        TestEntity entity = new TestEntity();
        Long firstId = 111L;
        Long secondId = 222L;

        // 模拟Hibernate API返回null（无主键）
        when(session.getEntityPersister(null, entity)).thenReturn(persister);
        when(persister.getIdentifier(entity, session)).thenReturn(null);

        // 模拟FlexKeyGenerator生成不同ID
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);
            when(flexKeyGenerator.generate()).thenReturn(firstId, secondId);

            // 执行两次生成
            Serializable result1 = generator.generate(session, entity);
            Serializable result2 = generator.generate(session, entity);

            // 验证结果
            assertEquals(firstId, result1, "第一次生成应返回第一个ID");
            assertEquals(secondId, result2, "第二次生成应返回第二个ID");

            // 验证FlexKeyGenerator.getInstance()被调用了两次（每次generate调用一次）
            mockedStatic.verify(FlexKeyGenerator::getInstance, times(2));
            verify(flexKeyGenerator, times(2)).generate();
        }
    }

    /// 测试FlexKeyGenerator返回null的情况
    /// 输入：实体对象，无ID
    /// 预期：返回null
    @Test
    void testFlexKeyGeneratorReturnsNull() {
        // 准备测试数据
        TestEntity entity = new TestEntity();

        // 模拟Hibernate API返回null（无主键）
        when(session.getEntityPersister(null, entity)).thenReturn(persister);
        when(persister.getIdentifier(entity, session)).thenReturn(null);

        // 模拟FlexKeyGenerator返回null
        try (MockedStatic<FlexKeyGenerator> mockedStatic = mockStatic(FlexKeyGenerator.class)) {
            mockedStatic.when(FlexKeyGenerator::getInstance).thenReturn(flexKeyGenerator);
            when(flexKeyGenerator.generate()).thenReturn(null);

            // 执行测试
            Serializable result = generator.generate(session, entity);

            // 验证结果
            assertNull(result, "当FlexKeyGenerator返回null时，结果应为null");

            // 验证FlexKeyGenerator被正确调用
            mockedStatic.verify(FlexKeyGenerator::getInstance, times(1));
            verify(flexKeyGenerator, times(1)).generate();
        }
    }

    /// 测试继承关系
    /// 输入：无
    /// 预期：正确继承AbstractIdentifierGenerator
    @Test
    void testInheritanceFromAbstractIdentifierGenerator() {
        // 验证FlexIdentifierGenerator正确继承了AbstractIdentifierGenerator
        assertInstanceOf(
                AbstractIdentifierGenerator.class,
                generator,
                "FlexIdentifierGenerator应该继承AbstractIdentifierGenerator");

        // 验证可以调用父类的方法
        assertNotNull(generator, "生成器实例不应为null");
    }

    /// 测试用的实体类
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestEntity {
        private Long id;
    }
}
