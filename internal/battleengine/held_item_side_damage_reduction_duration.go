package battleengine

// extendReflectDurationByHeldItem 返回反射壁在当前持有道具规则下应写入的初始持续回合。
// 已消费、转移或不存在的道具不会延长；道具只能延长而不能缩短技能资料本身的合法持续回合。
func extendReflectDurationByHeldItem(state State, actor MemberRef, duration uint8) uint8 {
	member, found := state.member(actor.Side, actor.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemReflectTurnsRemaining <= duration {
		return duration
	}
	return member.HeldItemReflectTurnsRemaining
}

// extendLightScreenDurationByHeldItem 返回光墙在当前持有道具规则下应写入的初始持续回合。
// 光墙投影独立于反射壁和极光幕，避免道具的适用屏障范围在运行时被错误扩大。
func extendLightScreenDurationByHeldItem(state State, actor MemberRef, duration uint8) uint8 {
	member, found := state.member(actor.Side, actor.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemLightScreenTurnsRemaining <= duration {
		return duration
	}
	return member.HeldItemLightScreenTurnsRemaining
}

// extendAuroraVeilDurationByHeldItem 返回极光幕在当前持有道具规则下应写入的初始持续回合。
// 它只作用于本次成功建立的极光幕，既不会刷新既有屏障，也不会影响回合末的统一递减。
func extendAuroraVeilDurationByHeldItem(state State, actor MemberRef, duration uint8) uint8 {
	member, found := state.member(actor.Side, actor.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemAuroraVeilTurnsRemaining <= duration {
		return duration
	}
	return member.HeldItemAuroraVeilTurnsRemaining
}
