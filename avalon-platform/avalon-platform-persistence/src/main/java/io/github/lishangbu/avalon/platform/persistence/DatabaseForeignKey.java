package io.github.lishangbu.avalon.platform.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标量编号的数据库外键，不引入跨模块实体关联、级联或隐式查询。由 Hibernate 元数据贡献器维护。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DatabaseForeignKey.List.class)
public @interface DatabaseForeignKey {

	String name();

	String[] columns();

	String referencedTable();

	String[] referencedColumns() default { "id" };

	String options() default "";

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {

		DatabaseForeignKey[] value();

	}

}
