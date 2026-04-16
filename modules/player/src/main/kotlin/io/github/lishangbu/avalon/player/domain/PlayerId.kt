package io.github.lishangbu.avalon.player.domain

/**
 * 玩家标识。
 *
 * 该类型为 Player 上下文保留统一的玩家身份封装，避免在领域层长期散落字符串。
 *
 * @property value 玩家原始标识值。
 */
@JvmInline
value class PlayerId(
    val value: String,
)