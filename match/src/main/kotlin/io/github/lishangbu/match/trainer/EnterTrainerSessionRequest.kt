package io.github.lishangbu.match.trainer

/** 选择当前账户某个有效 Trainer 进入游戏的请求。 */
data class EnterTrainerSessionRequest(var trainerId: String = "")
