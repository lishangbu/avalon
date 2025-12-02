package io.github.lishangbu.avalon.authorization.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;

/**
 * 为 Spring Data JDBC 注册自定义类型转换器
 *
 * <p>注册顺序: 支持 Postgres 的 PGobject <-> Map 转换器，以及 H2/通用的 String <-> Map 转换器 Spring Data JDBC
 * 会根据目标数据库的列类型选择合适的转换器
 *
 * @author lishangbu
 * @since 2025/12/2
 */
@Configuration(proxyBeanMethods = false)
public class JdbcCustomConversionsAutoConfiguration {
  @Bean
  public JdbcCustomConversions jdbcCustomConversions() {
    return new JdbcCustomConversions(
        List.of(
            // Postgres jsonb <-> Map
            new JsonbToMapConverter(),
            new MapToJsonbConverter(),
            // H2 或通用的 String <-> Map
            new StringToMapConverter(),
            new MapToStringConverter()));
  }
}
