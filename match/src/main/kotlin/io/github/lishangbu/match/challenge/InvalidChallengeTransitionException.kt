package io.github.lishangbu.match.challenge

/** Challenge 收到了当前状态不允许的迁移命令。 */
class InvalidChallengeTransitionException : IllegalArgumentException("Challenge transition is invalid")
