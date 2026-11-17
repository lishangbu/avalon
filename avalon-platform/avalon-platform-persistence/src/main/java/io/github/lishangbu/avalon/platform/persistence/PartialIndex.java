package io.github.lishangbu.avalon.platform.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 部分索引由 Hibernate Index 元数据导出；条件唯一索引不能转换成整表唯一约束。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PartialIndex.List.class)
public @interface PartialIndex {

	String name();

	String[] columns();

	String predicate();

	boolean unique() default false;

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {

		PartialIndex[] value();

	}

}
