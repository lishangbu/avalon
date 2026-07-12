package io.github.lishangbu.match.challenge

/** Challenge 从待处理到各类终态的稳定状态集合。 */
enum class ChallengeStatus { PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED, SUPERSEDED }
