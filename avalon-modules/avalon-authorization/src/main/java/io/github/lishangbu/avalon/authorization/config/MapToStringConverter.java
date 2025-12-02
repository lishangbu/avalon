package io.github.lishangbu.avalon.authorization.config;

import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将 Map<String, Object> 转换为 JSON 字符串的 Converter，适用于非 Postgres 场景（例如 H2）
 *
 * @author lishangbu
 * @since 2025/12/02
 */
@WritingConverter
public class MapToStringConverter implements Converter<Map<String, Object>, String> {

  private final JsonMapper jsonMapper =
      JsonMapper.builder()
          .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
          .build();

  @Override
  public String convert(Map<String, Object> source) {
    if (source == null) {
      return null;
    }
    try {
      return jsonMapper.writeValueAsString(source);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to convert Map to JSON string", e);
    }
  }
}
