package io.github.lishangbu.avalon.keygen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 详情参考<a href=
 * "https://gitee.com/mybatis-flex/mybatis-flex/blob/main/mybatis-flex-core/src/main/java/com/mybatisflex/core/keygen/impl/FlexIDKeyGenerator.java">
 * Mybatis Flex 的flexId生成算法</a>
 *
 * <p>独创的 FlexID 算法（简单、好用）:
 *
 * <p>特点： 1、保证 id 生成的顺序为时间顺序，越往后生成的 ID 值越大； 2、运行时，单台机器并发量在每秒钟 10w 以内； 3、运行时，无视时间回拨； 4、最大支持 99 台机器；
 * 5、够用大概 300 年左右的时间；
 *
 * <p>缺点： 1、每台机器允许最大的并发量为 10w/s。 2、出现时间回拨，重启机器时，在时间回拨未恢复的情况下，可能出现 id 重复。
 *
 * <p>ID组成：时间（7+）| 毫秒内的时间自增 （00~99：2）| 机器ID（00 ~ 99：2）| 随机数（00~99：2）用于分库分表时，通过 id 取模，保证分布均衡。
 *
 * @author lishangbu
 * @since 2025/4/2
 */
public class FlexKeyGenerator {
  /** 起始时间戳，用于生成唯一ID的时间基准 */
  private static final long INITIAL_TIMESTAMP = 1680411660000L;

  /** 毫秒内的最大自增序列号 */
  private static final long MAX_CLOCK_SEQ = 99;

  /** 默认机器ID */
  private static final int DEFAULT_WORK_ID = 1;

  /** 使用 ConcurrentHashMap 来缓存不同机器ID对应的 FlexKeyGenerator 实例 */
  private static final Map<Integer, FlexKeyGenerator> KEY_GENERATOR_CACHE =
      new ConcurrentHashMap<>();

  /** 最后一次生成 ID 的时间戳 */
  private long lastTimeMillis = 0;

  /** 当前毫秒内的自增序列号 */
  private long clockSeq = 0;

  /** 当前实例的机器ID */
  private final int workId;

  private FlexKeyGenerator() {
    this(DEFAULT_WORK_ID);
  }

  private FlexKeyGenerator(int workId) {
    final int minWorkerId = 0;
    final int maxWorkerId = 99;
    if (workId < minWorkerId || workId > maxWorkerId) {
      throw new IllegalArgumentException(
          String.format("workId must be between %d and %d", minWorkerId, maxWorkerId));
    }
    this.workId = workId;
  }

  private synchronized long nextId() {

    // 当前时间
    long currentTimeMillis = System.currentTimeMillis();

    if (currentTimeMillis == lastTimeMillis) {
      clockSeq++;
      if (clockSeq > MAX_CLOCK_SEQ) {
        clockSeq = 0;
        currentTimeMillis++;
      }
    }

    // 出现时间回拨
    else if (currentTimeMillis < lastTimeMillis) {
      currentTimeMillis = lastTimeMillis;
      clockSeq++;

      if (clockSeq > MAX_CLOCK_SEQ) {
        clockSeq = 0;
        currentTimeMillis++;
      }
    } else {
      clockSeq = 0;
    }

    lastTimeMillis = currentTimeMillis;

    long diffTimeMillis = currentTimeMillis - INITIAL_TIMESTAMP;

    // ID组成：时间（7+）| 毫秒内的时间自增 （00~99：2）| 机器ID（00 ~ 99：2）| 随机数（00~99：2）
    return diffTimeMillis * 1000000 + clockSeq * 10000 + workId * 100 + getRandomInt();
  }

  private int getRandomInt() {
    return ThreadLocalRandom.current().nextInt(100);
  }

  /**
   * 生成ID
   *
   * @return 返回FlexID
   */
  public long generate() {
    return getInstance().nextId();
  }

  /**
   * 获取默认工作 ID 的 FlexKeyGenerator 实例
   *
   * @return 默认工作 ID的FlexKeyGenerator实例
   */
  public static FlexKeyGenerator getInstance() {
    return getInstance(DEFAULT_WORK_ID);
  }

  /**
   * 获取指定工作 ID 的 FlexKeyGenerator 实例
   *
   * @return 指定工作 ID的FlexKeyGenerator实例
   */
  public static FlexKeyGenerator getInstance(int workId) {
    // 如果实例已存在，直接返回
    if (!KEY_GENERATOR_CACHE.containsKey(workId)) {
      synchronized (KEY_GENERATOR_CACHE) {
        if (!KEY_GENERATOR_CACHE.containsKey(workId)) {
          // 创建新的实例并缓存
          KEY_GENERATOR_CACHE.put(workId, new FlexKeyGenerator(workId));
        }
      }
    }
    return KEY_GENERATOR_CACHE.get(workId);
  }
}
