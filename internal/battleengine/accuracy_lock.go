package battleengine

// cloneMemberRef 深度复制可选成员稳定引用，避免状态快照、回放摘要或调用方修改同一指针。
func cloneMemberRef(value *MemberRef) *MemberRef {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// accuracyLockedOn 报告成员是否仍对给定具体目标享有命中锁定。
//
// 锁定以 MemberRef 而不是 SlotRef 判断，因此目标换出后，同一个场上槽位的新成员绝不会错误继承必中效果。
func (member MemberSnapshot) accuracyLockedOn(target MemberRef) bool {
	return member.AccuracyLockTurnsRemaining != 0 && member.AccuracyLockTarget != nil && *member.AccuracyLockTarget == target
}

// applyAccuracyLock 在单体变化技能已经命中且完成其它目标向后效后，尝试为使用者建立两阶段命中锁定。
//
// 替身和重复锁定都产生明确失败事件。它们不是普通未命中：技能已经通过命中判定并消费 PP，但无法修改目标本体
// 或刷新既有锁定。不同使用者锁定同一目标互不冲突，因为状态由各使用者独立持有。
func applyAccuracyLock(
	state State,
	actorRef MemberRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	targetHadSubstitute bool,
) (State, []Event) {
	if !skill.LocksAccuracyOnTarget {
		return state, nil
	}
	actor, actorExists := state.member(actorRef.Side, actorRef.Position)
	target, targetExists := state.member(targetRef.Side, targetRef.Position)
	if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 {
		return state, nil
	}
	failure := func(reason SkillFailureReason) (State, []Event) {
		return state, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: reason,
		}}
	}
	if targetHadSubstitute {
		return failure(SkillFailureReasonAccuracyLockTargetBehindSubstitute)
	}
	if actor.accuracyLockedOn(targetRef) {
		return failure(SkillFailureReasonAccuracyLockAlreadyActive)
	}
	lockedTarget := targetRef
	actor.AccuracyLockTarget = &lockedTarget
	// 建立回合也计入生命周期：本回合末会减为 1，下一回合末清除。
	actor.AccuracyLockTurnsRemaining = 2
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{AccuracyLockStartedEvent{
		Type: EventKindAccuracyLockStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
		TurnsRemaining: actor.AccuracyLockTurnsRemaining,
	}}
}

// advanceAccuracyLockAtEndTurn 在使用者仍连续上场时推进命中锁定的固定两阶段生命周期。
//
// 锁定不需要单独结束事件：状态摘要会在每回合后披露剩余阶段，且目标离场的全局清理会使使用者不可能把旧锁定
// 用于新成员。此函数仍防御性地处理指针和时长不一致，避免异常快照留下可被误判为有效的半状态。
func advanceAccuracyLockAtEndTurn(member MemberSnapshot) MemberSnapshot {
	if member.AccuracyLockTarget == nil || member.AccuracyLockTurnsRemaining == 0 {
		member.AccuracyLockTarget = nil
		member.AccuracyLockTurnsRemaining = 0
		return member
	}
	member.AccuracyLockTurnsRemaining--
	if member.AccuracyLockTurnsRemaining == 0 {
		member.AccuracyLockTarget = nil
	}
	return member
}

// clearAccuracyLocksPointingToMember 清除场上与后备成员中所有指向一个即将离场具体成员的命中锁定。
//
// 扫描完整队伍而非仅扫描场上成员，是为了让复杂的连续换人、强制补位和离场前效果不能在任意路径遗留过期
// 指针。后备成员通常不会持有该状态，但这一处理使 State 始终保持可审计的不变量。
func clearAccuracyLocksPointingToMember(state State, departed MemberRef) State {
	for sideIndex := range state.sides {
		for memberIndex := range state.sides[sideIndex].Members {
			member := state.sides[sideIndex].Members[memberIndex]
			if member.AccuracyLockTarget == nil || *member.AccuracyLockTarget != departed {
				continue
			}
			member.AccuracyLockTarget = nil
			member.AccuracyLockTurnsRemaining = 0
			state.sides[sideIndex].Members[memberIndex] = member
		}
	}
	return state
}
