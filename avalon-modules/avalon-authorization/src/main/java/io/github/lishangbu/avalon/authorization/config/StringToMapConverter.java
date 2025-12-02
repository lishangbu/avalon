package io.github.lishangbu.avalon.authorization.config;

import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将 JSON 字符串转换为 Map<String, Object> 的 Converter，适用于非 Postgres 场景（例如 H2）
 *
 * @author lishangbu
 * @since 2025/12/02
 */
@ReadingConverter
public class StringToMapConverter implements Converter<String, Map<String, Object>> {

  private final JsonMapper jsonMapper =
      JsonMapper.builder()
          .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
          .build();

  @Override
  public Map<String, Object> convert(String source) {
    if (source == null) {
      return null;
    }
    try {
      return jsonMapper.readValue(source, new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to convert JSON string to Map", e);
    }
  }
}
