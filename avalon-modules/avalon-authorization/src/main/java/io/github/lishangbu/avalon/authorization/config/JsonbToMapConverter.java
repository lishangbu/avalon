package io.github.lishangbu.avalon.authorization.config;

import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将 PostgreSQL 的 jsonb（PGobject）转换为 Map\<String, Object\>
 *
 * <p>使用 Jackson 将 json 字符串反序列化为 Map
 *
 * @author lishangbu
 * @since 2025/12/2
 */
@ReadingConverter
public class JsonbToMapConverter implements Converter<PGobject, Map<String, Object>> {

  private final JsonMapper jsonMapper =
      JsonMapper.builder()
          .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
          .build();

  @Override
  public Map<String, Object> convert(PGobject source) {
    if (source == null || source.getValue() == null) {
      return null;
    }
    try {
      return jsonMapper.readValue(source.getValue(), new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to convert jsonb to Map", e);
    }
  }
}
