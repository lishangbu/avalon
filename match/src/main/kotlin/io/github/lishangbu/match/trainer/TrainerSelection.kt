package io.github.lishangbu.match.trainer

/** 进入游戏时经过账户归属和 Active Match 查询后的 Trainer 选择。 */
data class TrainerSelection(val accountId: Long, val trainerId: Long, val activeMatchTrainerId: Long? = null)
