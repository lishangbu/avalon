package io.github.lishangbu.avalon.authorization.config;

import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将 Map\<String, Object\> 转换为 PostgreSQL 的 jsonb 类型（PGobject）
 *
 * <p>使用 Jackson 将 Map 序列化为 JSON 字符串，并封装为类型为 jsonb 的 PGobject
 *
 * @author lishangbu
 * @since 2025/12/2
 */
@WritingConverter
public class MapToJsonbConverter implements Converter<Map<String, Object>, PGobject> {

  private final JsonMapper jsonMapper =
      JsonMapper.builder()
          .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
          .build();

  @Override
  public PGobject convert(Map<String, Object> source) {
    if (source == null) {
      return null;
    }
    try {
      PGobject pg = new PGobject();
      pg.setType("jsonb");
      pg.setValue(jsonMapper.writeValueAsString(source));
      return pg;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to convert Map to jsonb", e);
    }
  }
}
