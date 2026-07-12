package io.github.lishangbu.match.game

/** 已完成或中断的 Match 被再次提交终态迁移。 */
class MatchAlreadyTerminalException : IllegalStateException("Match is already terminal")
