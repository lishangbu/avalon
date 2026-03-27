package io.github.lishangbu.avalon.idempotent.properties

import io.github.lishangbu.avalon.idempotent.support.StoreType
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Idempotent execution properties.
 */
@ConfigurationProperties(prefix = IdempotentProperties.PREFIX)
class IdempotentProperties {
    var enabled: Boolean = true

    var storeType: StoreType = StoreType.REDIS

    var keyPrefix: String = "idempotent"

    var headerName: String = "Idempotency-Key"

    var ttl: Duration = Duration.ofHours(24)

    var processingTtl: Duration = Duration.ofMinutes(5)

    var renewInterval: Duration? = null

    var jdbcTableName: String = "idempotency_record"

    companion object {
        const val PREFIX: String = "idempotent"
    }
}
