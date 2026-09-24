package io.github.lishangbu.avalon.platform.persistence;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Long 雪花主键：新实体保持空主键，由 Hibernate persist 时调用 CosId 分配。 仅用于独立生成的主键；自然主键和复用其他记录编号的共享主键不得使用。
 */
@IdGeneratorType(CosIdIdentifierGenerator.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface CosIdGenerated {

}
