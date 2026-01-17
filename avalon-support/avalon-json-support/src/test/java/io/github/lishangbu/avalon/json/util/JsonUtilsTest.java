package io.github.lishangbu.avalon.json.util;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/// JsonUtils 工具类单元测试
///
/// 覆盖对象序列化、反序列化、JsonNode 读取等核心功能
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonAutoConfiguration.class)
class JsonUtilsTest {

  /// 测试对象转 JSON 字符串
  @Test
  void testToJson() {
    Map<String, Object> data = Map.of("name", "avalon", "age", 18);
    String json = JsonUtils.toJson(data);
    assertTrue(json.contains("\"name\""));
    assertTrue(json.contains("\"age\""));
  }

  /// 测试对象转格式化 JSON 字符串
  @Test
  void testToPrettyJson() {
    Map<String, Object> data = Map.of("name", "avalon");
    String prettyJson = JsonUtils.toPrettyJson(data);
    assertTrue(prettyJson.contains("\n"));
    assertTrue(prettyJson.contains("avalon"));
  }

  /// 测试对象转 JSON 字节数组
  @Test
  void testToJsonAsBytes() {
    Map<String, Object> data = Map.of("id", 1);
    byte[] bytes = JsonUtils.toJsonAsBytes(data);
    assertNotNull(bytes);
    assertTrue(new String(bytes).contains("id"));
  }

  /// 测试 JSON 字符串转对象
  @Test
  void testReadValueFromString() {
    String json = "{\"name\":\"avalon\",\"age\":18}";
    Map<String, Object> result =
        JsonUtils.readValue(json, new TypeReference<Map<String, Object>>() {});
    assertEquals("avalon", result.get("name"));
    assertEquals(18, result.get("age"));
  }

  /// 测试字节数组转对象
  @Test
  void testReadValueFromBytes() {
    byte[] bytes = "{\"active\":true}".getBytes();
    Map<String, Object> result =
        JsonUtils.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    assertEquals(true, result.get("active"));
  }

  /// 测试 InputStream 转对象
  @Test
  void testReadValueFromInputStream() {
    String json = "{\"score\":100}";
    ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes());
    Map<String, Object> result =
        JsonUtils.readValue(in, new TypeReference<Map<String, Object>>() {});
    assertEquals(100, result.get("score"));
  }

  /// 测试 Reader 转对象
  @Test
  void testReadValueFromReader() {
    String json = "{\"flag\":false}";
    StringReader reader = new StringReader(json);
    Map<String, Object> result =
        JsonUtils.readValue(reader, new TypeReference<Map<String, Object>>() {});
    assertEquals(false, result.get("flag"));
  }

  /// 测试 JSON 字符串转 JsonNode
  @Test
  void testReadTreeFromString() {
    String json = "{\"x\":1}";
    JsonNode node = JsonUtils.readTree(json);
    assertEquals(1, node.get("x").asInt());
  }

  /// 测试空值处理
  @Test
  void testNullAndEmptyHandling() {
    assertNull(JsonUtils.toJson(null));
    assertNull(JsonUtils.toJson(""));
    // 明确指定参数类型，避免重载歧义
    assertNull(JsonUtils.readValue((String) null, Map.class));
    assertNull(JsonUtils.readValue("", Map.class));
    assertNull(JsonUtils.readValue((byte[]) null, Map.class));
    assertNull(JsonUtils.readValue(new byte[0], Map.class));
  }
}
