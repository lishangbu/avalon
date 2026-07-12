package io.github.lishangbu.match.game

/** Match 未产生正常赛果便终止时记录的运行时故障原因。 */
enum class MatchInterruptionReason { START_FAILED, RUNTIME_LOST, RUNTIME_FAILED }
