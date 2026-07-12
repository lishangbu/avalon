package io.github.lishangbu.security.event

/** 密码变化或账户禁用后的全局安全失效信号；监听器必须同步撤销该账户的所有认证状态。 */
data class AccountSecurityRevokedEvent(val accountId: Long, val principalName: String)
