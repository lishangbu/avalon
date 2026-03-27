package io.github.lishangbu.avalon.idempotent

import io.github.lishangbu.avalon.idempotent.properties.IdempotentProperties
import io.github.lishangbu.avalon.idempotent.support.StoreType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.time.Duration

class IdempotentSupportTypesTest {
    @Test
    fun exposesDefaultProperties() {
        val properties = IdempotentProperties()

        assertThat(properties.enabled).isTrue()
        assertThat(properties.storeType).isEqualTo(StoreType.REDIS)
        assertThat(properties.keyPrefix).isEqualTo("idempotent")
        assertThat(properties.headerName).isEqualTo("Idempotency-Key")
        assertThat(properties.ttl).isEqualTo(Duration.ofHours(24))
        assertThat(properties.processingTtl).isEqualTo(Duration.ofMinutes(5))
        assertThat(properties.renewInterval).isNull()
        assertThat(properties.jdbcTableName).isEqualTo("idempotency_record")
    }

    @Test
    fun registersAutoConfigurationImport() {
        val imports =
            ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .inputStream
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }

        assertThat(imports).containsExactly(
            "io.github.lishangbu.avalon.idempotent.autoconfigure.IdempotentAutoConfiguration",
        )
    }
}
