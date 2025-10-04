package io.github.lishangbu.avalon.hibernate;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import org.hibernate.annotations.IdGeneratorType;

/**
 * FlexId生成器注解
 *
 * <p>用于标记实体类主键字段，启用自定义的FlexIdentifierGenerator生成策略 适用于需要分布式唯一ID或自定义ID生成逻辑的场景
 * 通过@IdGeneratorType注解指定生成器类型，支持与Hibernate集成
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * @Flex
 * @Id
 * private Long id;
 * }</pre>
 *
 * @author lishangbu
 * @see org.hibernate.annotations.IdGeneratorType
 * @see io.github.lishangbu.avalon.hibernate.FlexIdentifierGenerator
 * @since 2025/9/14
 */
@Retention(RUNTIME)
@IdGeneratorType(value = FlexIdentifierGenerator.class)
public @interface Flex {}
