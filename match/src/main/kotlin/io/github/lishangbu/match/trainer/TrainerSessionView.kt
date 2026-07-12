package io.github.lishangbu.match.trainer

/** 已验证 Session 与其当前有效 Trainer 的组合视图。 */
data class TrainerSessionView(val session: TrainerSession, val trainer: TrainerRecord)
