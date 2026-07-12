package io.github.lishangbu.match.trainer

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 当前 Trainer 按完整 displayName 精确查找另一名 Trainer 的最小公开资料。 */
@RestController
@RequestMapping("/api/player/public-trainers")
class PublicTrainerController(private val service: PublicTrainerService) {
	@GetMapping
	fun find(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestParam displayName: String,
	): PublicTrainerProfile = service.find(authentication.accountId(), credential, displayName)
}
