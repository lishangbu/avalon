package io.github.lishangbu.avalon.keygen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author lishangbu
 * @since 2025/4/2
 */
class FlexKeyGeneratorTest {

  // 测试默认构造函数和生成的 ID
  @Test
  public void testDefaultConstructorGenerate() {
    FlexKeyGenerator generator = FlexKeyGenerator.getInstance();
    long id1 = generator.generate();
    long id2 = generator.generate();

    assertNotEquals(id1, id2, "IDs generated in subsequent calls should be unique.");
  }

  // 测试带参数构造函数和生成的 ID
  @Test
  public void testConstructorWithWorkIdGenerate() {
    FlexKeyGenerator generator1 = FlexKeyGenerator.getInstance(1);
    FlexKeyGenerator generator2 = FlexKeyGenerator.getInstance(2);

    long id1 = generator1.generate();
    long id2 = generator2.generate();

    assertNotEquals(id1, id2, "IDs generated for different work IDs should be unique.");
  }

  // 测试缓存实例，通过 workId 获取实例
  @Test
  public void testGetInstanceWithWorkId() {
    FlexKeyGenerator generator1 = FlexKeyGenerator.getInstance(1);
    FlexKeyGenerator generator2 = FlexKeyGenerator.getInstance(1);

    assertSame(
        generator1, generator2, "The instances for the same workId should be cached and the same.");
  }

  // 测试不同 workId 获取不同实例
  @Test
  public void testGetInstanceWithDifferentWorkId() {
    FlexKeyGenerator generator1 = FlexKeyGenerator.getInstance(1);
    FlexKeyGenerator generator2 = FlexKeyGenerator.getInstance(2);

    assertNotSame(generator1, generator2, "Instances for different workIds should be different.");
  }

  // 测试同一时间戳内的 ID 自增
  @Test
  public void testClockSeqIncrementWithinSameTimestamp() {
    FlexKeyGenerator generator = FlexKeyGenerator.getInstance();
    long id1 = generator.generate();

    // 模拟时间未发生变化，应该生成一个递增的 ID
    long id2 = generator.generate();

    assertTrue(
        id2 > id1,
        "The second ID should be greater than the first ID if generated within the same"
            + " timestamp.");
  }

  // 测试时间回拨处理
  @Test
  public void testTimeRollbackHandling() {
    FlexKeyGenerator generator = FlexKeyGenerator.getInstance();

    // 记录当前时间戳
    long id1 = generator.generate();

    // 模拟时间回拨，通过调整系统时间来实现
    try {
      // 暂时设置系统时间
      System.setProperty("user.timezone", "GMT-1");
      long id2 = generator.generate();

      assertTrue(id2 > id1, "ID should continue auto increase when time is rolled back.");
    } finally {
      System.setProperty("user.timezone", "GMT");
    }
  }

  // 测试 ID 生成的格式是否正确
  @Test
  public void testIdFormat() {
    long id = FlexKeyGenerator.getInstance().generate();
    String idStr = String.valueOf(id);

    assertTrue(
        idStr.matches("\\d{17,20}"),
        "Generated ID should match the expected format of a 19-digit number.");
  }

  // 测试最大工作 ID 的生成
  @Test
  public void testMaxWorkId() {
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> FlexKeyGenerator.getInstance(100),
        "The ID generated for workId 99 is not available.");
  }
}
