package io.github.lishangbu.avalon.battle.domain

/**
 * 对战会话标识。
 *
 * 该标识当前保持为轻量字符串封装，方便后续在不泄露底层生成策略的前提下，
 * 为 Battle 上下文的会话、日志和外部引用提供统一类型。
 *
 * @property value 对战会话的原始字符串值。
 */
@JvmInline
value class BattleSessionId(
    val value: String,
)