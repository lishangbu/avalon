package io.github.lishangbu.avalon.jimmer.jackson

import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean

/**
 * Jimmer Jackson 模块自动配置
 *
 * 向 JsonMapper 注册 Jimmer 的不可变对象支持模块
 */
@AutoConfiguration(
    before = [JacksonAutoConfiguration::class],
)
class ImmutableModuleV3AutoConfiguration {
    /** 创建 JsonMapper 构建器定制器 */
    @Bean
    fun jsonMapperBuilderCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.addModule(ImmutableModuleV3())
        }
}
