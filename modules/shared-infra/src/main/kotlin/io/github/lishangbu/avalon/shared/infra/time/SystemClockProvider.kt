package io.github.lishangbu.avalon.shared.infra.time

import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

/**
 * 系统时钟提供器。
 *
 * 通过显式封装 `Instant.now()`，让需要时间源的代码更容易在测试中替换或控制。
 */
@ApplicationScoped
class SystemClockProvider : ClockProvider {
    /**
     * @return 当前系统时间点。
     */
    override fun currentInstant(): Instant = Instant.now()
}