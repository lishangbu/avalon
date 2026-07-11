package io.github.lishangbu.battlesession

import io.github.lishangbu.battlesession.model.TurnRequirements

/** 表示 Turn Command 没有恰好覆盖当前全部人工选择要求。 */
class IncompleteTurnCommandException(
	val requirements: TurnRequirements,
) : IllegalArgumentException("turn command must contain every required selection and no other actions")
