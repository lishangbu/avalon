package io.github.lishangbu.avalon.authorization.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/// Spring Security 属性转换器
///
/// 用于 JPA 实体中 Map<String, Object> 类型属性的数据库存储与读取，
/// 通过 JSON 序列化/反序列化实现，支持 Spring Security 对象的安全转换。
///
/// 配置说明：
/// - 使用 JsonMapper 配置 SecurityJacksonModules，确保 Spring Security 对象正确序列化。
/// - 支持类型信息保留，防止反序列化时类型丢失。
///
/// @author lishangbu
/// @since 2026/2/7
@Converter
public class SpringSecurityAttributeConverter
    implements AttributeConverter<Map<String, Object>, String> {
  private final ClassLoader loader = getClass().getClassLoader();
  private final JsonMapper mapper =
      JsonMapper.builder().addModules(SecurityJacksonModules.getModules(loader)).build();

  /// 将属性 Map 序列化为 JSON 存储到数据库
  ///
  /// @param attribute 要转换的属性 Map
  /// @return 序列化后的 JSON 字符串，如果 attribute 为 null 则返回 null
  /// @throws RuntimeException 如果序列化失败
  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null) {
      return null;
    }
    return mapper.writeValueAsString(attribute);
  }

  /// 从数据库读取 JSON 数据反序列化为属性 Map
  ///
  /// @param json 数据库中的 JSON 字符串
  /// @return 反序列化后的属性 Map，如果 json 为 null 则返回 null
  /// @throws RuntimeException 如果反序列化失败
  @Override
  public Map<String, Object> convertToEntityAttribute(String json) {
    if (json == null) {
      return null;
    }
    return mapper.readValue(json, new TypeReference<>() {});
  }
}
