package io.github.lishangbu.avalon.jimmer.jackson

import org.assertj.core.api.Assertions.assertThat
import org.babyfish.jimmer.Input
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper

class ImmutableModuleV3AutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ImmutableModuleV3AutoConfiguration::class.java,
            ),
        )

    @Test
    fun jsonMapper_canDeserializeJimmerStyleInputDto() {
        contextRunner.run { context ->
            val jsonMapper = context.getBean(JsonMapper::class.java)
            val payload =
                """
                {
                  "id": "1",
                  "name": "hp"
                }
                """.trimIndent()

            val input = jsonMapper.readValue(payload, DemoInput::class.java)

            assertThat(input.id).isEqualTo("1")
            assertThat(input.name).isEqualTo("hp")
        }
    }

    open class DemoInput(
        val id: String,
        val name: String? = null,
    ) : Input<String> {
        override fun toEntity(): String = id

        class Builder {
            private var id: String? = null
            private var name: String? = null

            fun id(id: String): Builder {
                this.id = id
                return this
            }

            fun name(name: String?): Builder {
                this.name = name
                return this
            }

            fun build(): DemoInput =
                DemoInput(
                    id ?: throw Input.unknownNonNullProperty(DemoInput::class.java, "id"),
                    name,
                )
        }
    }
}
