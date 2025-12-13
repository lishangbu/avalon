package com.baomidou.mybatisplus.extension.handlers;

import java.lang.reflect.Field;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

/**
 * 支持spring security 序列化的 Jackson3 TypeHandler
 *
 * @author lishangbu
 * @since 2025/12/13
 */
public class SpringSecuritySerializableJackson3TypeHandler extends AbstractJsonTypeHandler<Object> {

  ClassLoader loader = getClass().getClassLoader();
  private final JsonMapper jsonMapper =
      JsonMapper.builder().addModules(SecurityJacksonModules.getModules(loader)).build();

  public SpringSecuritySerializableJackson3TypeHandler(Class<?> type) {
    super(type);
  }

  public SpringSecuritySerializableJackson3TypeHandler(Class<?> type, Field field) {
    super(type, field);
  }

  @Override
  public Object parse(String json) {
    TypeFactory typeFactory = jsonMapper.getTypeFactory();
    JavaType javaType = typeFactory.constructType(getFieldType());
    return jsonMapper.readValue(json, javaType);
  }

  @Override
  public String toJson(Object obj) {
    return jsonMapper.writeValueAsString(obj);
  }
}
