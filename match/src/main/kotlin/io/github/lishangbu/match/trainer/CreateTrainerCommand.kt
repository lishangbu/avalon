package io.github.lishangbu.match.trainer

import java.util.UUID

/** 已解析为领域类型的 Trainer 创建命令。 */
data class CreateTrainerCommand(val commandId: UUID, val displayName: String)
