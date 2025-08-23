package io.github.lishangbu.avalon.authorization;

import io.github.lishangbu.avalon.mybatis.id.MybatisIdentifierInterceptorAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ImportAutoConfiguration(MybatisIdentifierInterceptorAutoConfiguration.class)
@MapperScan("io.github.lishangbu.avalon.authorization.mapper")
public class MapperTestEnvironmentAutoConfiguration {}
