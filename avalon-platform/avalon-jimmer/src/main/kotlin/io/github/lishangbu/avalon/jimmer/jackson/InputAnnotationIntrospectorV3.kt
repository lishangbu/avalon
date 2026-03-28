package io.github.lishangbu.avalon.jimmer.jackson

import org.babyfish.jimmer.Input
import tools.jackson.core.Version
import tools.jackson.databind.AnnotationIntrospector
import tools.jackson.databind.annotation.JsonPOJOBuilder
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.AnnotatedClass

/**
 * 为 Jimmer KSP 生成的 Input DTO 补充 Jackson 3 的 Builder 发现能力。
 *
 * 这些 DTO 仍然生成了 `com.fasterxml.jackson.*` 注解，运行在 `tools.jackson.*`
 * 时无法自动识别，因此这里按 Jimmer Input 的约定补齐 Builder 元数据。
 */
class InputAnnotationIntrospectorV3 : AnnotationIntrospector() {
    override fun version(): Version =
        Version(
            1,
            0,
            0,
            null,
            "io.github.lishangbu.avalon",
            "avalon-jimmer",
        )

    override fun findPOJOBuilder(
        config: MapperConfig<*>?,
        ac: AnnotatedClass,
    ): Class<*>? {
        val inputType = ac.annotated
        if (!Input::class.java.isAssignableFrom(inputType)) {
            return super.findPOJOBuilder(config, ac)
        }
        return inputType.declaredClasses.firstOrNull { it.simpleName == "Builder" }
            ?: super.findPOJOBuilder(config, ac)
    }

    override fun findPOJOBuilderConfig(
        config: MapperConfig<*>?,
        ac: AnnotatedClass,
    ): JsonPOJOBuilder.Value? {
        val declaringType = ac.annotated.declaringClass
        if (declaringType != null && Input::class.java.isAssignableFrom(declaringType)) {
            return JsonPOJOBuilder.Value("build", "")
        }
        return super.findPOJOBuilderConfig(config, ac)
    }
}
