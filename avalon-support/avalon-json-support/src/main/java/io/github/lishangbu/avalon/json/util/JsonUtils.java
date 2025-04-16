package io.github.lishangbu.avalon.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.json.exception.JsonProcessingRuntimeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * JSON 序列化与反序列化工具类。 提供将 Java 对象转为 JSON 字符串、格式化 JSON 和将 JSON 转换为不同 Java 类型的功能。
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public class JsonUtils implements ApplicationContextAware {

  // 用于 JSON 处理的 ObjectMapper 实例
  private static ObjectMapper OBJECT_MAPPER;

  /**
   * 获取 ObjectMapper 实例。 这是一个单例实例，可用于 JSON 序列化和反序列化。
   *
   * @return ObjectMapper 实例
   */
  public static ObjectMapper getInstance() {
    return OBJECT_MAPPER;
  }

  /**
   * 将 Java 对象序列化为 JSON 字符串。
   *
   * @param value 要序列化的 Java 对象
   * @return 序列化后的 JSON 字符串，如果输入为空，则返回 null
   */
  @Nullable
  public static String toJson(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    try {
      return getInstance().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Java 对象序列化为格式化（漂亮打印）的 JSON 字符串。
   *
   * @param value 要序列化的 Java 对象
   * @return 格式化后的 JSON 字符串，如果输入为空，则返回 null
   */
  @Nullable
  public static String toPrettyJson(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    try {
      return getInstance().writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Java 对象序列化为 JSON 字节数组。
   *
   * @param value 要序列化的 Java 对象
   * @return 序列化后的 JSON 字节数组，如果输入为空，则返回 null
   */
  public static byte[] toJsonAsBytes(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    try {
      return getInstance().writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 JSON 字符串转换为 JsonNode 对象。
   *
   * @param content 要反序列化的 JSON 字符串
   * @return 转换后的 JsonNode 对象
   */
  public static JsonNode readTree(String content) {
    try {
      return getInstance().readTree(content);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 InputStream 转换为 JsonNode 对象。
   *
   * @param in 要读取的 InputStream
   * @return 转换后的 JsonNode 对象
   */
  public static JsonNode readTree(InputStream in) {
    try {
      return getInstance().readTree(in);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Reader 转换为 JsonNode 对象。
   *
   * @param r 要读取的 Reader
   * @return 转换后的 JsonNode 对象
   */
  public static JsonNode readTree(Reader r) {
    try {
      return getInstance().readTree(r);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将字节数组转换为 JsonNode 对象。
   *
   * @param content 包含 JSON 数据的字节数组
   * @return 转换后的 JsonNode 对象
   */
  public static JsonNode readTree(byte[] content) {
    try {
      return getInstance().readTree(content);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  // region JSON 反序列化方法

  /**
   * 将 JSON 字符串转换为指定类的 Java 对象。
   *
   * @param content 要反序列化的 JSON 字符串
   * @param valueType 要转换为的 Java 类
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable String content, Class<T> valueType) {
    if (ObjectUtils.isEmpty(content)) {
      return null;
    }
    try {
      return getInstance().readValue(content, valueType);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 JSON 字符串转换为指定泛型类型的 Java 对象。
   *
   * @param content 要反序列化的 JSON 字符串
   * @param valueTypeRef 泛型类型的 TypeReference
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable String content, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(content)) {
      return null;
    }
    try {
      return getInstance().readValue(content, valueTypeRef);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 JSON 字节数组转换为指定类的 Java 对象。
   *
   * @param src 包含 JSON 数据的字节数组
   * @param valueType 要转换为的 Java 类
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 JSON 字节数组转换为指定泛型类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的字节数组
   * @param valueTypeRef 泛型类型的 TypeReference
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueTypeRef);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 JSON 字节数组转换为指定 JavaType 类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的字节数组
   * @param javaType 指定的 JavaType 类型
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable byte[] src, JavaType javaType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, javaType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 InputStream 转换为指定类的 Java 对象。
   *
   * @param src 包含 JSON 数据的 InputStream
   * @param valueType 要转换为的 Java 类
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable InputStream src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 InputStream 转换为指定泛型类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的 InputStream
   * @param valueTypeRef 泛型类型的 TypeReference
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable InputStream src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueTypeRef);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 InputStream 转换为指定 JavaType 类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的 InputStream
   * @param valueType 指定的 JavaType 类型
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable InputStream src, JavaType valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Reader 转换为指定类的 Java 对象。
   *
   * @param src 包含 JSON 数据的 Reader
   * @param valueType 要转换为的 Java 类
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable Reader src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Reader 转换为指定泛型类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的 Reader
   * @param valueTypeRef 泛型类型的 TypeReference
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable Reader src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueTypeRef);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 将 Reader 转换为指定 JavaType 类型的 Java 对象。
   *
   * @param src 包含 JSON 数据的 Reader
   * @param valueType 指定的 JavaType 类型
   * @param <T> 转换后的 Java 对象类型
   * @return 反序列化后的 Java 对象，如果输入为空，则返回 null
   */
  @Nullable
  public static <T> T readValue(@Nullable Reader src, JavaType valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    try {
      return getInstance().readValue(src, valueType);
    } catch (IOException e) {
      throw new JsonProcessingRuntimeException(e);
    }
  }

  /**
   * 设置 ApplicationContext 以便获取 ObjectMapper Bean。
   *
   * @param applicationContext 包含 ObjectMapper 的应用上下文
   * @throws BeansException 如果在访问应用上下文时发生错误
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    // 从 Spring 上下文获取 ObjectMapper Bean
    OBJECT_MAPPER = applicationContext.getBean(ObjectMapper.class);
  }

  // endregion
}
