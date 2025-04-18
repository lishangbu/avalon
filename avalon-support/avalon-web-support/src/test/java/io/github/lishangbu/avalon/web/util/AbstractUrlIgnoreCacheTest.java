package io.github.lishangbu.avalon.web.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** AbstractUrlIgnoreCache 单元测试 */
class AbstractUrlIgnoreCacheTest {

  private AbstractUrlIgnoreCache urlIgnoreCache;

  @BeforeEach
  void setUp() {
    // 创建一个匿名子类来实现抽象方法
    urlIgnoreCache =
        new AbstractUrlIgnoreCache() {
          @Override
          protected List<String> getIgnoreUrls() {
            return List.of("/api/ignore/**", "/public/**");
          }
        };
  }

  @Test
  void testShouldIgnoreMatchingUrl() {
    // 测试匹配的 URL
    assertTrue(urlIgnoreCache.shouldIgnore("/api/ignore/test"));
    assertTrue(urlIgnoreCache.shouldIgnore("/public/test"));
  }

  @Test
  void testShouldIgnoreNonMatchingUrl() {
    // 测试不匹配的 URL
    assertFalse(urlIgnoreCache.shouldIgnore("/api/test"));
    assertFalse(urlIgnoreCache.shouldIgnore("/private/test"));
  }
}
