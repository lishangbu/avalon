package io.github.lishangbu.avalon.orm.id.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.lishangbu.avalon.orm.id.FlexIdentifierGenerator;
import java.lang.annotation.Retention;
import org.hibernate.annotations.IdGeneratorType;

/**
 * FlexId生成器注解
 *
 * @author lishangbu
 * @since 2025/4/7
 */
@Retention(RUNTIME)
@IdGeneratorType(value = FlexIdentifierGenerator.class)
public @interface FlexIdGenerator {}
