package io.github.lishangbu.gamedata.catalog

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 登录账户可读取、但不要求 Trainer Session 的玩家资料目录。 */
@RestController
@RequestMapping("/api/player/catalog")
class PlayerCatalogController(private val service: PlayerCatalogService) {
	@GetMapping("/creatures")
	fun creatures(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): PlayerCatalogPage<PlayerCatalogCreatureResponse> = service.creatures(page, size, q)

	@GetMapping("/skills")
	fun skills(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): PlayerCatalogPage<PlayerCatalogSkillResponse> = service.skills(page, size, q)

	@GetMapping("/abilities")
	fun abilities(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): PlayerCatalogPage<PlayerCatalogAbilityResponse> = service.abilities(page, size, q)

	@GetMapping("/items")
	fun items(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): PlayerCatalogPage<PlayerCatalogItemResponse> = service.items(page, size, q)
}
