package io.github.lishangbu.avalon.hibernate;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import org.hibernate.annotations.IdGeneratorType;

/// FlexId 生成器注解
///
/// - 标记实体主键字段即可启用 {@link FlexIdentifierGenerator} 策略
/// - 适用于需要分布式唯一 ID 或自定义生成逻辑的场景
/// - 通过 {@link IdGeneratorType} 明确指定生成器，确保与 Hibernate 集成
///
/// 示例：
/// {@code
///
/// @author lishangbu
/// @Flex
/// @Id private Long id;
/// }
/// @see org.hibernate.annotations.IdGeneratorType
/// @see io.github.lishangbu.avalon.hibernate.FlexIdentifierGenerator
/// @since 2025/9/14
@Retention(RUNTIME)
@IdGeneratorType(value = FlexIdentifierGenerator.class)
public @interface Flex {}
