package io.github.lishangbu.avalon.json.util;

import java.io.InputStream;
import java.io.Reader;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON 工具类，提供对象与 JSON 的序列化与反序列化能力
 *
 * <p>所有方法直接抛出 JacksonException，调用方无需捕获
 *
 * @author lishangbu
 * @since 2025/4/8
 * @apiNote JacksonException 为运行时异常，建议由全局异常处理统一处理
 */
public class JsonUtils implements ApplicationContextAware {

  private static JsonMapper JSON_MAPPER;

  /** 获取 JsonMapper 单例实例 */
  public static JsonMapper getInstance() {
    return JSON_MAPPER;
  }

  /**
   * 对象转 JSON 字符串
   *
   * @param value 待序列化对象
   * @return JSON 字符串，空对象返回 null
   */
  @Nullable
  public static String toJson(@Nullable Object value) {
    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
      return null;
    }
    return getInstance().writeValueAsString(value);
  }

  /**
   * 对象转格式化 JSON 字符串
   *
   * @param value 待序列化对象
   * @return 格式化 JSON 字符串，空对象返回 null
   */
  @Nullable
  public static String toPrettyJson(@Nullable Object value) {
    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
      return null;
    }
    return getInstance().writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }

  /**
   * 对象转 JSON 字节数组
   *
   * @param value 待序列化对象
   * @return JSON 字节数组，空对象返回 null
   */
  public static byte[] toJsonAsBytes(@Nullable Object value) {
    if (value == null) {
      return null;
    }
    return getInstance().writeValueAsBytes(value);
  }

  /** JSON 字符串转 JsonNode */
  public static JsonNode readTree(String content) {
    return getInstance().readTree(content);
  }

  /** InputStream 转 JsonNode */
  public static JsonNode readTree(InputStream in) {
    return getInstance().readTree(in);
  }

  /** Reader 转 JsonNode */
  public static JsonNode readTree(Reader r) {
    return getInstance().readTree(r);
  }

  /** 字节数组转 JsonNode */
  public static JsonNode readTree(byte[] content) {
    return getInstance().readTree(content);
  }

  // region 反序列化

  /**
   * JSON 字符串转对象
   *
   * @param content JSON 字符串
   * @param valueType 目标类型
   * @return 目标对象，空字符串返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable String content, Class<T> valueType) {
    if (content == null || content.isEmpty()) {
      return null;
    }
    return getInstance().readValue(content, valueType);
  }

  /**
   * JSON 字符串转泛型对象
   *
   * @param content JSON 字符串
   * @param valueTypeRef 类型引用
   * @return 泛型对象，空字符串返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable String content, TypeReference<T> valueTypeRef) {
    if (content == null || content.isEmpty()) {
      return null;
    }
    return getInstance().readValue(content, valueTypeRef);
  }

  /**
   * 字节数组转对象
   *
   * @param src JSON 字节数组
   * @param valueType 目标类型
   * @return 目标对象，空数组返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, Class<T> valueType) {
    if (src == null || src.length == 0) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  /** 字节数组转泛型对象 */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, TypeReference<T> valueTypeRef) {
    if (src == null || src.length == 0) {
      return null;
    }
    return getInstance().readValue(src, valueTypeRef);
  }

  /** 字节数组转指定类型对象 */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, JavaType javaType) {
    if (src == null || src.length == 0) {
      return null;
    }
    return getInstance().readValue(src, javaType);
  }

  /** InputStream 转对象 */
  @Nullable
  public static <T> T readValue(InputStream src, Class<T> valueType) {
    return getInstance().readValue(src, valueType);
  }

  /** InputStream 转泛型对象 */
  @Nullable
  public static <T> T readValue(InputStream src, TypeReference<T> valueTypeRef) {
    return getInstance().readValue(src, valueTypeRef);
  }

  /** InputStream 转指定类型对象 */
  @Nullable
  public static <T> T readValue(InputStream src, JavaType valueType) {
    return getInstance().readValue(src, valueType);
  }

  /** Reader 转对象 */
  @Nullable
  public static <T> T readValue(Reader src, Class<T> valueType) {
    return getInstance().readValue(src, valueType);
  }

  /** Reader 转泛型对象 */
  @Nullable
  public static <T> T readValue(Reader src, TypeReference<T> valueTypeRef) {
    return getInstance().readValue(src, valueTypeRef);
  }

  /** Reader 转指定类型对象 */
  @Nullable
  public static <T> T readValue(Reader src, JavaType valueType) {
    return getInstance().readValue(src, valueType);
  }

  /** 注入 JsonMapper Bean */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    JSON_MAPPER = applicationContext.getBean(JsonMapper.class);
  }

  // endregion
}
