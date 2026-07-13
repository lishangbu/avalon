package io.github.lishangbu

import io.github.lishangbu.match.event.PlayerEventMetrics
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

/** 最近五分钟通知发送持续失败时触发健康告警，供部署平台统一采集。 */
@Component("playerEvents")
class PlayerEventHealthIndicator(private val metrics: PlayerEventMetrics) : HealthIndicator {
	override fun health(): Health {
		val failures = metrics.recentDeliveryFailures()
		val details = mapOf(
			"recentDeliveryFailures" to failures,
			"windowSeconds" to PlayerEventMetrics.ALERT_WINDOW.seconds,
			"threshold" to PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD,
		)
		return if (failures >= PlayerEventMetrics.DELIVERY_FAILURE_ALERT_THRESHOLD) {
			Health.outOfService().withDetails(details).build()
		} else Health.up().withDetails(details).build()
	}
}
