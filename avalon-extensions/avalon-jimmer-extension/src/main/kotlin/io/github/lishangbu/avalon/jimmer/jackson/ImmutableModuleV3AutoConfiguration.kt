package io.github.lishangbu.avalon.jimmer.jackson

import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    before = [JacksonAutoConfiguration::class],
)
class ImmutableModuleV3AutoConfiguration {
    @Bean
    fun jsonMapperBuilderCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.addModule(ImmutableModuleV3())
        }
}
