package io.github.lishangbu.match.trainer

/** 创建 Trainer 的幂等命令载荷。 */
data class CreateTrainerRequest(var commandId: String = "", var displayName: String = "")
