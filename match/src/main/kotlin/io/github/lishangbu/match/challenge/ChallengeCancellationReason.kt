package io.github.lishangbu.match.challenge

/** Challenge 被取消时持久化的业务原因；非取消终态不携带该值。 */
enum class ChallengeCancellationReason { WITHDRAWN, TRAINER_ARCHIVED, ROSTER_INVALIDATED }
