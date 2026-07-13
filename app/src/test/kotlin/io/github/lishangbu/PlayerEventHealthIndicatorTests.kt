package io.github.lishangbu

import io.github.lishangbu.match.event.PlayerEventMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.health.contributor.Status
import java.time.Instant

/** 验证实时通知发送失败的滑动窗口告警与自动恢复边界。 */
class PlayerEventHealthIndicatorTests {
	@Test
	fun `delivery failure threshold raises alert and old failures age out`() {
		val metrics = PlayerEventMetrics(SimpleMeterRegistry())
		val health = PlayerEventHealthIndicator(metrics)
		repeat(PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD - 1) { metrics.deliveryFailed("MATCH_CHANGED") }
		assertThat(health.health().status).isEqualTo(Status.UP)

		metrics.deliveryFailed("MATCH_CHANGED")
		assertThat(health.health().status).isEqualTo(Status.OUT_OF_SERVICE)

		val recovered = PlayerEventMetrics(SimpleMeterRegistry())
		repeat(PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD) {
			recovered.deliveryFailed("MATCH_CHANGED", Instant.now().minus(PlayerEventMetrics.ALERT_WINDOW).minusSeconds(1))
		}
		assertThat(PlayerEventHealthIndicator(recovered).health().status).isEqualTo(Status.UP)

		val outOfOrder = PlayerEventMetrics(SimpleMeterRegistry())
		repeat(PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD - 1) { outOfOrder.deliveryFailed("MATCH_CHANGED") }
		repeat(PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD) {
			outOfOrder.deliveryFailed("MATCH_CHANGED", Instant.now().minus(PlayerEventMetrics.ALERT_WINDOW).minusSeconds(1))
		}
		assertThat(PlayerEventHealthIndicator(outOfOrder).health().status).isEqualTo(Status.UP)
	}
}
