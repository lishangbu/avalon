package io.github.lishangbu.avalon.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * x
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public class JsonUtils implements ApplicationContextAware {

  private static ObjectMapper OBJECT_MAPPER;

  public static ObjectMapper getInstance() {
    return OBJECT_MAPPER;
  }

  /**
   * 将对象序列化成json字符串
   *
   * @param value javaBean
   * @return jsonString json字符串
   */
  @Nullable
  @SneakyThrows(value = JsonProcessingException.class)
  public static String toJson(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    return getInstance().writeValueAsString(value);
  }

  /**
   * 将对象序列化成 json 字符串，格式美化
   *
   * @param value javaBean
   * @return jsonString json字符串
   */
  @Nullable
  @SneakyThrows(value = JsonProcessingException.class)
  public static String toPrettyJson(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    return getInstance().writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }

  /**
   * 将对象序列化成 json byte 数组
   *
   * @param value
   * @return jsonString json byte array
   */
  @SneakyThrows(value = JsonProcessingException.class)
  public static byte[] toJsonAsBytes(@Nullable Object value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    return getInstance().writeValueAsBytes(value);
  }

  /**
   * 将json字符串转成 JsonNode
   *
   * @param content content
   * @return content json字符串
   */
  @SneakyThrows(value = IOException.class)
  public static JsonNode readTree(String content) {
    return getInstance().readTree(content);
  }

  /**
   * 将InputStream转成 JsonNode
   *
   * @param in InputStream
   * @return jsonString json字符串
   */
  @SneakyThrows(value = IOException.class)
  public static JsonNode readTree(InputStream in) {
    return getInstance().readTree(in);
  }

  /**
   * 将java.io.Reader转成 JsonNode
   *
   * @param r java.io.Reader
   * @return jsonString json字符串
   */
  @SneakyThrows(value = IOException.class)
  public static JsonNode readTree(Reader r) {
    return getInstance().readTree(r);
  }

  /**
   * 将json字符串转成 JsonNode
   *
   * @param content content
   * @return jsonString json字符串
   */
  @SneakyThrows(value = IOException.class)
  public static JsonNode readTree(byte[] content) {
    return getInstance().readTree(content);
  }

  // region json反序列化为对象

  /**
   * 将json反序列化成对象
   *
   * @param content content
   * @param valueType class
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable String content, Class<T> valueType) {
    if (ObjectUtils.isEmpty(content)) {
      return null;
    }
    return getInstance().readValue(content, valueType);
  }

  /**
   * 将json反序列化成对象
   *
   * @param content content
   * @param valueTypeRef 泛型类型
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable String content, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(content)) {
      return null;
    }
    return getInstance().readValue(content, valueTypeRef);
  }

  /**
   * 将json byte 数组反序列化成对象
   *
   * @param src json bytes
   * @param valueType class
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable byte[] src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  /**
   * 将json byte 数组反序列化成对象
   *
   * @param src json bytes
   * @param valueTypeRef 泛型类型
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable byte[] src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueTypeRef);
  }

  /**
   * 将json byte 数组反序列化成对象
   *
   * @param src json bytes
   * @param javaType JavaType
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable byte[] src, JavaType javaType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, javaType);
  }

  /**
   * 将java.io.InputStream反序列化成对象
   *
   * @param src java.io.InputStream
   * @param valueType class
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable InputStream src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  /**
   * 将java.io.InputStream反序列化成对象
   *
   * @param src java.io.InputStream
   * @param valueTypeRef 泛型类型
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable InputStream src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueTypeRef);
  }

  /**
   * 将java.io.InputStream反序列化成对象
   *
   * @param src java.io.InputStream
   * @param valueType JavaType
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable InputStream src, JavaType valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  /**
   * 将java.io.Reader反序列化成对象
   *
   * @param src java.io.Reader
   * @param valueType class
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable Reader src, Class<T> valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  /**
   * 将java.io.Reader反序列化成对象
   *
   * @param src java.io.Reader
   * @param valueTypeRef 泛型类型
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable Reader src, TypeReference<T> valueTypeRef) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueTypeRef);
  }

  /**
   * 将java.io.Reader反序列化成对象
   *
   * @param src java.io.Reader
   * @param valueType JavaType
   * @param <T> T 泛型标记
   * @return Bean
   */
  @Nullable
  @SneakyThrows(value = IOException.class)
  public static <T> T readValue(@Nullable Reader src, JavaType valueType) {
    if (ObjectUtils.isEmpty(src)) {
      return null;
    }
    return getInstance().readValue(src, valueType);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    OBJECT_MAPPER = applicationContext.getBean(ObjectMapper.class);
  }

  // endregion

}
