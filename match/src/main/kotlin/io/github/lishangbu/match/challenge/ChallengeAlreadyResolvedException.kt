package io.github.lishangbu.match.challenge

/** 已进入终态的 Challenge 被再次处理。 */
class ChallengeAlreadyResolvedException : IllegalStateException("Challenge has already been resolved")
