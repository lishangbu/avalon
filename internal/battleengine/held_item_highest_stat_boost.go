package battleengine

// resolveHeldItemHighestStatBoost 结算一名成员实际换入完成后的最高原始能力强化道具。
//
// 调用顺序位于入场天气、场地、强天气、天气形态、特性复制和属性身份处理之后；因此它判断的是成员最终实际
// 特性、当前有效环境和最终冻结战斗画像，不会把随后会被覆盖的中间状态写入持续强化。
func resolveHeldItemHighestStatBoost(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 {
		return state, nil
	}
	return applyHeldItemHighestStatBoost(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeHeldItemHighestStatBoosts 按冻结阵营和槽位顺序结算双方初始上场成员的消耗道具。
//
// 初始事件与后续换入事件使用同一结构，以便 Battle 创建事务和回放都能明确记录道具何时被消耗；环境特性已经
// 在 NewState 的前置流程建立，因此此时可以正确决定“环境生效则不消耗”的互斥关系。
func initializeHeldItemHighestStatBoosts(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 {
				continue
			}
			var applied []Event
			state, applied = applyHeldItemHighestStatBoost(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, applied...)
		}
	}
	return state, events
}

// applyHeldItemHighestStatBoost 消耗成员携带的最高原始能力强化道具并写入持续强化能力项。
//
// 只有当前道具允许成员实际特性触发、尚未消耗过且没有匹配环境强化时才会生效。环境特性与道具强化不叠加：
// 环境一旦生效，道具保持不消耗；环境之后消失也不会倒补消耗。这与“在入场时决定是否消耗”的一次性语义一致。
func applyHeldItemHighestStatBoost(state State, memberRef MemberRef) (State, []Event) {
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.BoosterEnergyStat != "" || member.AbilityID == 0 ||
		!containsString(member.HighestStatBoosterAbilityIDs, member.AbilityID) ||
		environmentHighestStatMultiplierActive(member, effectiveWeather(state), state.environment.Terrain) {
		return state, nil
	}
	itemID := member.ItemID
	member.BoosterEnergyStat = highestRawBattleStat(member.Stats)
	// 消耗道具后所有道具派生运行态必须同步清空。资料层禁止一个道具同时声明属性身份和本效果，但这里仍保持
	// 防御式清理，避免损坏快照在道具已消失后继续伪装为拥有道具属性身份。
	member.ItemID = 0
	member.HeldItemElementID = 0
	member = restoreHeldItemElementIdentity(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemHighestStatBoostActivatedEvent{
		Type: EventKindHeldItemHighestStatBoostActivated, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, AbilityID: member.AbilityID, Stat: member.BoosterEnergyStat,
	}}
}
