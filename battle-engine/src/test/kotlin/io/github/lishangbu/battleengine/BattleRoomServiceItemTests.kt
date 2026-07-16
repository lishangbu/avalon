package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** 验证客房服务只在戏法空间成功建立时降低场上持有者速度并消费。 */
class BattleRoomServiceItemTests {
	private val fieldEffects = BattleFieldEffects()
	private val trickRoomSkill = damagingSkill(
		skillId = 91,
		name = "戏法空间",
		damageClass = BattleDamageClass.STATUS,
		power = null,
	)
	private val application = BattleFieldSpeedOrderApplication(
		speedOrderEffect = BattleFieldSpeedOrderEffect(BattleFieldSpeedOrderKind.TRICK_ROOM, turnsRemaining = 5),
	)

	@Test
	fun `room service lowers active holder speed and is consumed when trick room starts`() {
		val holder = participant(
			actorId = "holder",
			speed = 80,
			itemId = 1180,
			itemEffects = listOf(roomServiceEffect()),
		)
		val state = BattleEngine().start(initialState(first = holder, second = participant("setter", speed = 60)))

		val resolved = fieldEffects.applyFieldSpeedOrder(state, "setter", trickRoomSkill, application)

		val resolvedHolder = assertNotNull(resolved.participant("holder"))
		assertEquals(-1, resolvedHolder.statStage(BattleStat.SPEED))
		assertEquals(null, resolvedHolder.itemId)
		assertEquals(emptyList(), resolvedHolder.itemEffects)
		assertEquals(
			-1,
			resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single().currentStage,
		)
	}

	@Test
	fun `room service does not activate when trick room is ended`() {
		val holder = participant(
			actorId = "holder",
			speed = 80,
			itemId = 1180,
			itemEffects = listOf(roomServiceEffect()),
		)
		val state = BattleEngine().start(initialState(
			first = holder,
			second = participant("setter", speed = 60),
			environment = BattleEnvironment(fieldSpeedOrderEffect = application.speedOrderEffect),
		))

		val resolved = fieldEffects.applyFieldSpeedOrder(state, "setter", trickRoomSkill, application)

		val resolvedHolder = assertNotNull(resolved.participant("holder"))
		assertEquals(0, resolvedHolder.statStage(BattleStat.SPEED))
		assertEquals(1180, resolvedHolder.itemId)
	}

	@Test
	fun `room service stays held when speed is already at minimum stage`() {
		val holder = participant(
			actorId = "holder",
			speed = 80,
			itemId = 1180,
			itemEffects = listOf(roomServiceEffect()),
		).copy(statStages = mapOf(BattleStat.SPEED to -6))
		val state = BattleEngine().start(initialState(first = holder, second = participant("setter", speed = 60)))

		val resolved = fieldEffects.applyFieldSpeedOrder(state, "setter", trickRoomSkill, application)

		val resolvedHolder = assertNotNull(resolved.participant("holder"))
		assertEquals(-6, resolvedHolder.statStage(BattleStat.SPEED))
		assertEquals(1180, resolvedHolder.itemId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	private fun roomServiceEffect() = BattleItemEffect.FieldSpeedOrderActivatedStatStageChange(
		kind = BattleFieldSpeedOrderKind.TRICK_ROOM,
		stat = BattleStat.SPEED,
		stageDelta = -1,
	)
}
