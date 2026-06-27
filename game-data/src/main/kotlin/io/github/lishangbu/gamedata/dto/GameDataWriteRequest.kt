package io.github.lishangbu.gamedata.dto

/**
 * 游戏资料独立写入 DTO 的内部转换契约。
 */
interface GameDataWriteRequest {
	fun toFields(): Map<String, Any?>
}
