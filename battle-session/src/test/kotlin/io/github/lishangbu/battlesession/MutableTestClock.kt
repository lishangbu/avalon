package io.github.lishangbu.battlesession

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal class MutableTestClock(
	private var current: Instant,
	private val currentZone: ZoneId = ZoneOffset.UTC,
) : Clock() {
	override fun getZone(): ZoneId = currentZone

	override fun withZone(zone: ZoneId): Clock = MutableTestClock(current, zone)

	override fun instant(): Instant = current

	fun advance(duration: Duration) {
		current = current.plus(duration)
	}

	fun set(instant: Instant) {
		current = instant
	}
}
