package io.github.lishangbu.match.trainer

/** 对外最小 Trainer 资料；刻意不暴露内部 Identifier、账户、队伍与不可挑战原因。 */
data class PublicTrainerProfile(
	val displayName: String,
	val online: Boolean,
	val challengeable: Boolean,
)
