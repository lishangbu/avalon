package io.github.lishangbu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class TimeConfig {
	@Bean
	fun applicationClock(): Clock = Clock.systemUTC()
}
