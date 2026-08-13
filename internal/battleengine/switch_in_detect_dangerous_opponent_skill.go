package battleengine

// resolveSwitchInDetectDangerousOpponentSkill 结算成员实际换入后的危险技能侦测规则。
func resolveSwitchInDetectDangerousOpponentSkill(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInDetectDangerousOpponentSkill {
		return state, nil
	}
	return applySwitchInDetectDangerousOpponentSkill(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInDetectDangerousOpponentSkill 在初始入场阶段写入可观察的危险技能侦测事件。
// 当前事件属于初始公开事件，状态本身不因侦测而改变。

func initializeSwitchInDetectDangerousOpponentSkill(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInDetectDangerousOpponentSkill {
				continue
			}
			var detected []Event
			state, detected = applySwitchInDetectDangerousOpponentSkill(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, detected...)
		}
	}
	return state, events
}

// applySwitchInDetectDangerousOpponentSkill 选择稳定第一项对自身危险的对手技能并发布侦测事件。
//
// 一击必杀始终属于危险技能；普通伤害只有在有威力、不是变化技能且最终属性对入场者至少一项属性形成
// 克制时才危险。当前 SkillSnapshot 没有单独的 typeless 标记，所有可执行技能都必须拥有冻结属性 ID，
// 因而空属性不会被错误判为危险。
func applySwitchInDetectDangerousOpponentSkill(state State, actor MemberRef) (State, []Event) {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInDetectDangerousOpponentSkill {
		return state, nil
	}
	selected, found := dangerousOpponentSkill(state, actor)
	if !found {
		return state, nil
	}
	return state, []Event{DangerousOpponentSkillDetectedEvent{
		Type: EventKindDangerousOpponentSkillDetected, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, Target: selected.Target, SkillID: selected.SkillID,
	}}
}

// dangerousOpponentSkill 返回按成员引用与技能稳定 ID 排序后的第一项危险技能。
func dangerousOpponentSkill(state State, actor MemberRef) (dangerousSkill, bool) {
	var selected dangerousSkill
	found := false
	for _, side := range state.sides {
		if side.Side == actor.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			opponent, exists := state.member(side.Side, position)
			if !exists || opponent.CurrentHP == 0 {
				continue
			}
			for _, skill := range opponent.Skills {
				if !skillIsDangerous(state, opponent, skill, receiverForDangerousSkill(state, actor)) {
					continue
				}
				candidate := dangerousSkill{Target: MemberRef{Side: side.Side, Position: opponent.Position}, SkillID: skill.SkillID}
				if !found || candidate.Target.Side < selected.Target.Side ||
					(candidate.Target.Side == selected.Target.Side && candidate.Target.Position < selected.Target.Position) ||
					(candidate.Target == selected.Target && candidate.SkillID < selected.SkillID) {
					selected, found = candidate, true
				}
			}
		}
	}
	return selected, found
}

// receiverForDangerousSkill 读取侦测者的当前属性快照，调用方已确保成员存活且在场。
func receiverForDangerousSkill(state State, actor MemberRef) MemberSnapshot {
	member, _ := state.member(actor.Side, actor.Position)
	return member
}

// skillIsDangerous 判断单项技能是否满足规则中的一击必杀或属性克制条件。
func skillIsDangerous(state State, attacker MemberSnapshot, skill SkillSnapshot, receiver MemberSnapshot) bool {
	if skill.damageMode() == SkillDamageModeOneHitKnockOut {
		return true
	}
	if skill.Power == 0 || skill.DamageClass == DamageClassStatus || skill.ElementID == 0 {
		return false
	}
	elementID := effectiveSkillElementForMember(attacker, skill, effectiveSkillWeather(state, attacker))
	for _, defenseElementID := range receiver.ElementIDs {
		numerator, denominator := state.rules.effectiveness(elementID, defenseElementID)
		if numerator > denominator {
			return true
		}
	}
	return false
}

// dangerousSkill 是危险技能侦测在发布结构化事件前使用的内部候选。
type dangerousSkill struct {
	Target  MemberRef
	SkillID Identifier
}
