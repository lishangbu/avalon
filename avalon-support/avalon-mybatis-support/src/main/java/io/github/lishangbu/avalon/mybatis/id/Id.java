package io.github.lishangbu.avalon.mybatis.id;

import java.lang.annotation.*;

/**
 * 表主键标识
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Id {

  /** 主键类型 {@link IdType} */
  IdType type() default IdType.FLEX;
}
