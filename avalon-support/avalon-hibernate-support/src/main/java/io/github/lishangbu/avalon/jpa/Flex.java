package io.github.lishangbu.avalon.jpa;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import org.hibernate.annotations.IdGeneratorType;

/**
 * FlexId生成器注解
 *
 * @author lishangbu
 * @since 2025/9/14
 */
@Retention(RUNTIME)
@IdGeneratorType(value = FlexIdentifierGenerator.class)
public @interface Flex {}
