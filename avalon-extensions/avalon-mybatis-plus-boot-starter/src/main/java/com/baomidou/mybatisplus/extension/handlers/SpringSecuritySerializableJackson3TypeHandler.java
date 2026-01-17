package com.baomidou.mybatisplus.extension.handlers;

import java.lang.reflect.Field;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

/// # Spring Security Jackson3 TypeHandler
/// 为可序列化的 Spring Security 对象提供 Jackson3 支持
/// - 使用 {@code SecurityJacksonModules} 注册必要模块，保证权限相关类可被序列化
/// - 通过 JsonMapper 构造器复用模块配置，parse/toJson 保持一致行为
///
/// @author lishangbu
/// @since 2025/12/13
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
