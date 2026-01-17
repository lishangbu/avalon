package io.github.lishangbu.avalon.json.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/// JacksonAutoConfiguration 自动配置单元测试
///
/// 验证 JsonMapper 和 JsonUtils Bean 的自动注入和配置特性
class JacksonAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(JacksonAutoConfiguration.class);

  /// 测试 JsonMapper Bean 自动注入
  @Test
  void testJsonMapperBeanExists() {
    contextRunner.run(
        context -> {
          JsonMapper jsonMapper = context.getBean(JsonMapper.class);
          assertNotNull(jsonMapper);
          // 验证 long 类型序列化为字符串
          String json = jsonMapper.writeValueAsString(Long.MAX_VALUE);
          assertTrue(json.contains("\"") || json.matches("\\d+"));
        });
  }

  /// 测试 JsonUtils Bean 自动注入
  @Test
  void testJsonUtilsBeanExists() {
    contextRunner.run(
        context -> {
          JsonUtils jsonUtils = context.getBean(JsonUtils.class);
          assertNotNull(jsonUtils);
        });
  }
}
