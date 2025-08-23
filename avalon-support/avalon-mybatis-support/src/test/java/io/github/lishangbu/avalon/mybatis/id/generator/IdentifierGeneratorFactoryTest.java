package io.github.lishangbu.avalon.mybatis.id.generator;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.mybatis.id.Id;
import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serializable;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdentifierGeneratorFactoryTest {

  // 初始化时注册一个测试的生成器
  @BeforeEach
  void setUp() {
    // 清空注册的生成器
    IdentifierGeneratorFactory.registerIdentifierGenerator(new FlexIdentifierGenerator());
  }

  // 测试获取 ID 生成器的方法
  @Test
  @Order(-1)
  void testGetIdentifierGenerator() {
    // 获取 FlexIdentifierGenerator
    IdentifierGenerator generator = IdentifierGeneratorFactory.getIdentifierGenerator(IdType.FLEX);
    assertNotNull(generator, "生成器不应为空");
    assertTrue(generator instanceof FlexIdentifierGenerator, "应返回 FlexIdentifierGenerator 类型的生成器");
  }

  // 测试 nextId 方法，确保正确生成 ID
  @Test
  @Order(-2)
  void testNextId() throws NoSuchFieldException {
    // 模拟一个实体类
    class TestEntity {
      @Id(type = IdType.FLEX)
      private Long id;
    }

    // 获取 TestEntity 类中的 id 字段
    Field idField = TestEntity.class.getDeclaredField("id");

    // 调用 nextId 生成 ID
    Serializable generatedId = IdentifierGeneratorFactory.nextId(idField, new TestEntity());
    assertNotNull(generatedId, "生成的 ID 不应为空");
    assertTrue(generatedId instanceof Long, "生成的 ID 应为 Long 类型");
  }

  // 测试未注册的生成器，应该抛出异常
  @Test
  @Order(-4)
  void testNextIdWithUnregisteredGenerator() throws NoSuchFieldException {
    // 模拟一个实体类
    class AnotherEntity {
      @Id(type = IdType.UNKNOWN)
      private Long id;
    }

    // 获取 AnotherEntity 类中的 id 字段
    Field idField = AnotherEntity.class.getDeclaredField("id");

    // 断言未注册生成器时抛出异常
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              IdentifierGeneratorFactory.nextId(idField, new AnotherEntity());
            });
    assertEquals("未注册 ID 生成器，ID 类型:" + IdType.UNKNOWN, exception.getMessage());
  }

  // 测试注册新的生成器
  @Test
  @Order(-3)
  void testRegisterIdentifierGenerator() {
    // 定义一个新的生成器
    UnknownIdentifierGenerator unknownIdentifierGenerator = new UnknownIdentifierGenerator();

    // 注册新的生成器
    IdentifierGeneratorFactory.registerIdentifierGenerator(unknownIdentifierGenerator);

    // 获取并测试新的生成器
    IdentifierGenerator generator =
        IdentifierGeneratorFactory.getIdentifierGenerator(IdType.UNKNOWN);
    assertNotNull(generator, "生成器应被注册");
    assertTrue(
        generator instanceof UnknownIdentifierGenerator, "应返回 UnknownIdentifierGenerator 类型的生成器");
  }

  // 测试重新注册相同类型的生成器，应该不会覆盖原有的生成器
  @Test
  @Order(-5)
  void testRegisterExistingIdentifierGenerator() {
    // 先注册一个生成器
    IdentifierGenerator firstGenerator = new FlexIdentifierGenerator();
    IdentifierGeneratorFactory.registerIdentifierGenerator(firstGenerator);

    // 尝试重新注册相同类型的生成器
    IdentifierGenerator secondGenerator = new FlexIdentifierGenerator();
    IdentifierGeneratorFactory.registerIdentifierGenerator(secondGenerator);

    // 获取生成器并验证，只会有一个实例
    IdentifierGenerator generator = IdentifierGeneratorFactory.getIdentifierGenerator(IdType.FLEX);
    assertTrue(generator instanceof FlexIdentifierGenerator, "应返回唯一的 FlexIdentifierGenerator 实例");
  }
}

class UnknownIdentifierGenerator implements IdentifierGenerator {

  @Override
  public Serializable nextId(Field field, Object entity) {
    throw new UnsupportedOperationException("未知的 ID 生成策略");
  }

  @Override
  public IdType getIdType() {
    return IdType.UNKNOWN;
  }
}
