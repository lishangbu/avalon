package io.github.lishangbu.avalon.shared.application.time

import java.time.Instant

/**
 * 应用层使用的时间源契约。
 *
 * 应用服务只依赖这个纯 Kotlin 契约来获取当前时间，具体系统时钟、测试时钟或其他运行时实现
 * 由外层模块提供，避免用例编排直接依赖 Quarkus bean 或 `Instant.now()`。
 */
interface ClockProvider {
    /**
     * @return 当前应用参考时间点。
     */
    fun currentInstant(): Instant
}