package io.github.lishangbu.security.event

/** 当前登录 family 主动退出后，通知游戏模块结束该账户的短期 Session/Presence。 */
data class AccountSessionEndedEvent(val accountId: Long, val loginToken: String)
