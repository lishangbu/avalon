package io.github.lishangbu.match.trainer

/** Team 请求不满足完整性、Identifier 或 revision 约束。 */
class TrainerTeamRequestException(val code: String) : RuntimeException(code)
