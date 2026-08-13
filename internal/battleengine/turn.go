package battleengine

import (
	"fmt"
	"math"
	"sort"
)

// TurnResult 是一次纯回合结算产生的完整候选结果。
//
// Battle Runtime 必须先持久化整个候选结果，事务提交成功后才能用 State 和 RandomSource
// 替换内存中的权威值；持久化失败时直接丢弃本对象即可安全重试。
type TurnResult struct {
	// State 是本回合全部状态转换完成后的新权威候选状态。
	State State
	// Events 是本回合按真实发生顺序产生的版本化结构化事件。
	Events []Event
	// RandomTrace 是本回合从序号 1 开始记录的全部随机消费。
	RandomTrace []RandomTraceEntry
	// RandomSource 是完成本回合消费后的下一确定性随机源状态。
	// 使用 TracedRandom 重放时该字段只保存已消费回放适配器，不得作为下一回合生成源持久化。
	RandomSource RandomSource
}

// BattleResultReason 是纯引擎可以确认的稳定终局原因。
type BattleResultReason string

const (
	// BattleResultReasonAllMembersFainted 表示败方已经没有任何生命值大于 0 的成员。
	BattleResultReasonAllMembersFainted BattleResultReason = "allMembersFainted"
	// BattleResultReasonMaxTurnsReached 表示赛制回合上限已到且双方仍有可战斗成员。
	BattleResultReasonMaxTurnsReached BattleResultReason = "maxTurnsReached"
)

// BattleResult 是战斗引擎已经确认的终局事实。
type BattleResult struct {
	// WinningSide 是胜方阵营位置；0 表示没有胜方的引擎平局。
	WinningSide Side `json:"winningSide,omitempty"`
	// Reason 是产生终局结果的稳定原因代码。
	Reason BattleResultReason `json:"reason"`
}

// actionPlan 保存一个已经通过完整命令校验的可排序行动。
type actionPlan struct {
	// action 是玩家为当前场上槽位提交的原始结构化行动。
	action Action
	// member 是回合开始时占用该槽位的行动成员快照。
	member MemberSnapshot
	// priority 是行动排序使用的有效优先度；主动替换高于普通技能行动。
	priority int16
	// orderBracket 在相同有效优先度内施加确定性前后置排序；0 为普通行动，负值表示后置行动。
	// 它独立于 priority，确保“变化技能后置”不会错误跨越不同技能优先度的基础规则。
	orderBracket int8
	// speed 是冻结当前状态和主要异常修正后用于行动排序的有效速度。
	speed uint32
	// tieBreak 是速度和优先度相同时使用的显式随机排序值。
	tieBreak int32
}

// ResolveTurn 校验并结算双方对一个完整回合提交的全部行动。
//
// 当前垂直切片支持单打和双打中的普通物理/特殊单目标技能；尚未注册的行动或
// 效果会显式返回 ErrInvalidTurnCommand，不会静默降级为普通攻击。
func ResolveTurn(state State, command TurnCommand, input RandomInput) (TurnResult, error) {
	plans, err := prepareActionPlans(state, command)
	if err != nil {
		return TurnResult{}, err
	}
	var random RandomSource
	switch value := input.(type) {
	case RandomSource:
		random = value
	case TracedRandom:
		if value.position != 0 {
			return TurnResult{}, fmt.Errorf("%w: 回放必须从轨迹第一项开始", ErrInvalidRandomTrace)
		}
		random = RandomSource{replaying: true, replay: value}
	default:
		return TurnResult{}, ErrUnsupportedRandomInput
	}

	// 随机算法内部状态跨回合连续推进，但持久化轨迹序号按契约在每回合从 1 重新开始。
	random.sequence = 0
	trace := make([]RandomTraceEntry, 0, len(plans)*2)
	var heldItemOrderEvents []Event
	state, plans, random, heldItemOrderEvents, heldItemOrderTrace, err := applyHeldItemActionOrderEffects(state, plans, random, command.TurnNumber)
	if err != nil {
		return TurnResult{}, err
	}
	trace = append(trace, heldItemOrderTrace...)
	reverseSpeedOrder := state.environment.FieldSpeedOrder != nil && state.environment.FieldSpeedOrder.Kind.reversesSpeedOrder()
	// 先按不依赖客户端数组顺序的稳定键排列，再为每个完整同速组逐一消费随机值。
	sort.SliceStable(plans, func(left, right int) bool {
		if plans[left].priority != plans[right].priority {
			return plans[left].priority > plans[right].priority
		}
		if plans[left].orderBracket != plans[right].orderBracket {
			return plans[left].orderBracket > plans[right].orderBracket
		}
		if plans[left].speed != plans[right].speed {
			if reverseSpeedOrder {
				return plans[left].speed < plans[right].speed
			}
			return plans[left].speed > plans[right].speed
		}
		if plans[left].action.Actor.Side != plans[right].action.Actor.Side {
			return plans[left].action.Actor.Side < plans[right].action.Actor.Side
		}
		return plans[left].action.Actor.Position < plans[right].action.Actor.Position
	})
	for groupStart := 0; groupStart < len(plans); {
		groupEnd := groupStart + 1
		for groupEnd < len(plans) && plans[groupEnd].priority == plans[groupStart].priority && plans[groupEnd].orderBracket == plans[groupStart].orderBracket &&
			plans[groupEnd].speed == plans[groupStart].speed {
			groupEnd++
		}
		if groupEnd-groupStart > 1 {
			for index := groupStart; index < groupEnd; index++ {
				reason := fmt.Sprintf(
					"speed tie for side %d member %d", plans[index].action.Actor.Side, plans[index].member.Position,
				)
				value, nextRandom, entry, nextErr := random.Next(1_000_000, reason)
				if nextErr != nil {
					return TurnResult{}, nextErr
				}
				random = nextRandom
				plans[index].tieBreak = value
				trace = append(trace, entry)
			}
		}
		groupStart = groupEnd
	}
	sort.SliceStable(plans, func(left, right int) bool {
		if plans[left].priority != plans[right].priority {
			return plans[left].priority > plans[right].priority
		}
		if plans[left].orderBracket != plans[right].orderBracket {
			return plans[left].orderBracket > plans[right].orderBracket
		}
		if plans[left].speed != plans[right].speed {
			if reverseSpeedOrder {
				return plans[left].speed < plans[right].speed
			}
			return plans[left].speed > plans[right].speed
		}
		return plans[left].tieBreak > plans[right].tieBreak
	})

	nextState := State{
		format:      state.format,
		rules:       state.rules,
		environment: cloneEnvironment(state.environment),
		sides:       cloneSides(state.sides),
		turnNumber:  command.TurnNumber,
		result:      cloneBattleResult(state.result),
	}
	events := []Event{TurnStartedEvent{
		Type: EventKindTurnStarted, SchemaVersion: 1, TurnNumber: command.TurnNumber,
	}}
	events = append(events, heldItemOrderEvents...)
	for _, plan := range plans {
		var actionEvents []Event
		var actionTrace []RandomTraceEntry
		// 将本回合已经追加的事件显式传给行动结算。伤害记忆这类时序规则只从事件流读取事实，不能在 State 中
		// 维护可被换人、重放或跨回合误用的隐藏缓存。
		nextState, random, actionEvents, actionTrace, err = resolveAction(nextState, plan.action, random, events)
		if err != nil {
			return TurnResult{}, err
		}
		var reactiveEvents []Event
		nextState, reactiveEvents = resolveReactiveAbilityAfterAction(nextState, actionEvents)
		actionEvents = append(actionEvents, reactiveEvents...)
		// 伤害、反作用或强制换人都可能使当前强天气来源在本次行动内离场或倒下。必须在下一项行动前
		// 同步来源，避免后续命中、伤害与属性相性继续读取已经失效的强天气。
		var strongWeatherEvents []Event
		nextState, strongWeatherEvents = synchronizeStrongWeather(nextState)
		actionEvents = append(actionEvents, strongWeatherEvents...)
		var weatherFormEvents []Event
		nextState, weatherFormEvents = synchronizeWeatherForms(nextState)
		actionEvents = append(actionEvents, weatherFormEvents...)
		events = append(events, actionEvents...)
		trace = append(trace, actionTrace...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
			break
		}
	}
	if nextState.result == nil {
		var statusEvents []Event
		nextState, statusEvents = resolveEndTurnMajorStatusDamage(nextState)
		events = append(events, statusEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var volatileEvents []Event
		nextState, volatileEvents = resolveEndTurnVolatileStatusEffects(nextState)
		events = append(events, volatileEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var leechSeedEvents []Event
		nextState, leechSeedEvents = resolveEndTurnLeechSeedEffects(nextState)
		events = append(events, leechSeedEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var abilityEvents []Event
		var abilityTrace []RandomTraceEntry
		nextState, random, abilityEvents, abilityTrace, err = resolveEndTurnReactiveAbilities(nextState, random)
		if err != nil {
			return TurnResult{}, err
		}
		events = append(events, abilityEvents...)
		trace = append(trace, abilityTrace...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber, WinningSide: result.WinningSide, Reason: result.Reason})
		}
	}
	if nextState.result == nil {
		var environmentEvents []Event
		nextState, environmentEvents = resolveEndTurnEnvironmentEffects(nextState)
		events = append(events, environmentEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var heldItemHealingEvents []Event
		nextState, heldItemHealingEvents = resolveEndTurnHeldItemHealing(nextState)
		events = append(events, heldItemHealingEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var heldItemDamageEvents []Event
		nextState, heldItemDamageEvents = resolveEndTurnHeldItemDamage(nextState)
		events = append(events, heldItemDamageEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil {
		var sideConditionEvents []Event
		nextState, sideConditionEvents = resolveEndTurnSideConditions(nextState)
		events = append(events, sideConditionEvents...)
		if result, ended := detectBattleResult(nextState); ended {
			nextState.result = &result
			events = append(events, BattleEndedEvent{
				Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
				WinningSide: result.WinningSide, Reason: result.Reason,
			})
		}
	}
	if nextState.result == nil && nextState.format.MaxTurns > 0 && command.TurnNumber >= nextState.format.MaxTurns {
		result := BattleResult{Reason: BattleResultReasonMaxTurnsReached}
		nextState.result = &result
		events = append(events, BattleEndedEvent{
			Type: EventKindBattleEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
			WinningSide: result.WinningSide, Reason: result.Reason,
		})
	}
	events = append(events, TurnEndedEvent{
		Type: EventKindTurnEnded, SchemaVersion: 1, TurnNumber: command.TurnNumber,
	})
	if random.replaying && !random.replay.FullyConsumed() {
		return TurnResult{}, fmt.Errorf("%w: 回合结算后仍有未消费轨迹", ErrRandomTraceDiverged)
	}
	return TurnResult{State: nextState, Events: events, RandomTrace: trace, RandomSource: random}, nil
}

func prepareActionPlans(state State, command TurnCommand) ([]actionPlan, error) {
	if state.result != nil {
		return nil, commandError(TurnCommandErrorBattleEnded, "/turnNumber", "战斗已经结束")
	}
	if command.SchemaVersion != 1 {
		return nil, commandError(
			TurnCommandErrorInvalidSchemaVersion, "/schemaVersion", "仅支持 schemaVersion=1",
		)
	}
	if command.TurnNumber != state.turnNumber+1 {
		return nil, commandError(
			TurnCommandErrorUnexpectedTurnNumber, "/turnNumber",
			fmt.Sprintf("回合号为 %d，要求 %d", command.TurnNumber, state.turnNumber+1),
		)
	}
	if command.Time.ElapsedMilliseconds < 0 {
		return nil, commandError(
			TurnCommandErrorInvalidTimeInput, "/time/elapsedMilliseconds", "经过毫秒数不能为负数",
		)
	}
	liveActors, faintedSwitchCandidates, requiredFaintedSwitches := pendingTurnActors(state)
	expectedActions := len(liveActors) + requiredFaintedSwitches
	if len(command.Actions) != expectedActions {
		return nil, commandError(
			TurnCommandErrorIncompleteActions, "/actions",
			fmt.Sprintf("行动数量为 %d，要求 %d", len(command.Actions), expectedActions),
		)
	}

	seenActors := make(map[SlotRef]struct{}, len(command.Actions))
	seenLiveActors := make(map[SlotRef]struct{}, len(liveActors))
	faintedSwitches := 0
	seenSwitchTargets := make(map[MemberRef]struct{}, len(command.Actions))
	plans := make([]actionPlan, 0, len(command.Actions))
	for index, action := range command.Actions {
		actionField := fmt.Sprintf("/actions/%d", index)
		if _, duplicate := seenActors[action.Actor]; duplicate {
			return nil, commandError(TurnCommandErrorDuplicateActor, actionField+"/actor", "场上槽位重复提交行动")
		}
		seenActors[action.Actor] = struct{}{}
		member, ok := state.ActiveMember(action.Actor)
		if !ok {
			return nil, commandError(TurnCommandErrorInvalidActor, actionField+"/actor", "不是有效场上成员")
		}
		if member.CurrentHP == 0 {
			if _, candidate := faintedSwitchCandidates[action.Actor]; !candidate {
				return nil, commandError(TurnCommandErrorInvalidActor, actionField+"/actor", "倒下槽位当前没有可补位成员")
			}
		} else if _, required := liveActors[action.Actor]; !required {
			return nil, commandError(TurnCommandErrorInvalidActor, actionField+"/actor", "当前槽位不需要提交行动")
		} else {
			seenLiveActors[action.Actor] = struct{}{}
		}
		forcedSkillPosition := member.forcedSkillPosition()
		switch action.Kind {
		case ActionKindUseSkill:
			if member.CurrentHP == 0 {
				return nil, commandError(TurnCommandErrorFaintedActor, actionField+"/actor", "倒下成员不能使用技能")
			}
			if action.UseSkill == nil || action.Switch != nil {
				return nil, commandError(TurnCommandErrorInvalidActionShape, actionField, "技能行动载荷无效")
			}
			if !action.UseSkill.SkillPosition.Valid() || int(action.UseSkill.SkillPosition) > len(member.Skills) {
				return nil, commandError(
					TurnCommandErrorInvalidSkillPosition, actionField+"/useSkill/skillPosition", "技能位置无效",
				)
			}
			if forcedSkillPosition != 0 && action.UseSkill.SkillPosition != forcedSkillPosition {
				return nil, commandError(TurnCommandErrorForcedSkill, actionField+"/useSkill/skillPosition", "当前易变状态要求重复使用指定技能")
			}
			if action.UseSkill.Terastallize {
				if !state.rules.TerastallizationEnabled {
					return nil, commandError(TurnCommandErrorTerastallizationDisabled, actionField+"/useSkill/terastallize", "当前赛制不允许太晶化")
				}
				if state.terastallizationUsed(action.Actor.Side) {
					return nil, commandError(TurnCommandErrorTerastallizationAlreadyUsed, actionField+"/useSkill/terastallize", "本方已经使用太晶化")
				}
				if member.Terastallized {
					return nil, commandError(TurnCommandErrorActorAlreadyTerastallized, actionField+"/useSkill/terastallize", "成员已经太晶化")
				}
				if member.TeraElementID == 0 {
					return nil, commandError(TurnCommandErrorTeraElementUnavailable, actionField+"/useSkill/terastallize", "成员没有太晶属性")
				}
			}
			skill := member.Skills[action.UseSkill.SkillPosition-1]
			if member.ItemID != 0 && member.HeldItemChoiceSkillLock && member.HeldItemChoiceLockedSkillPosition != 0 &&
				action.UseSkill.SkillPosition != member.HeldItemChoiceLockedSkillPosition {
				return nil, commandError(TurnCommandErrorForcedSkill, actionField+"/useSkill/skillPosition", "当前持有道具要求继续使用首次宣告的技能")
			}
			if member.ItemID != 0 && member.HeldItemStatusSkillRestriction && skill.DamageClass == DamageClassStatus {
				return nil, commandError(TurnCommandErrorSkillUnavailable, actionField+"/useSkill/skillPosition", "当前持有道具禁止选择变化技能")
			}
			if skill.RemainingPP == 0 ||
				(skill.damageMode() == SkillDamageModeFormula && skill.DamageClass == DamageClassStatus &&
					len(skill.StatusApplications) == 0 && len(skill.StatStageEffects) == 0 &&
					!skill.CuresUserSideMajorStatuses && !skill.CuresUserMajorStatus && !skill.CuresUserSideActiveMajorStatuses &&
					skill.HealingPercent == 0 && skill.TargetHealingNumerator == 0 && skill.FlinchChancePercent == 0 && len(skill.VolatileStatusApplications) == 0 &&
					skill.FieldSpeedOrderApplication == nil && skill.LeechSeedApplication == nil && skill.WeatherApplication == nil && skill.TerrainApplication == nil && skill.TailwindApplication == nil && skill.ReflectApplication == nil && skill.LightScreenApplication == nil && skill.AuroraVeilApplication == nil && skill.SpikesApplication == nil && skill.StealthRockApplication == nil && skill.ToxicSpikesApplication == nil && skill.StickyWebApplication == nil && skill.RapidSpinApplication == nil && skill.DefogApplication == nil && !skill.ForceTargetSwitch && !skill.LocksAccuracyOnTarget) ||
				(skill.damageMode() == SkillDamageModeFormula && skill.DamageClass != DamageClassStatus && skill.Power == 0 && !skill.DynamicPower.active()) {
				return nil, commandError(
					TurnCommandErrorSkillUnavailable, actionField+"/useSkill/skillPosition", "技能当前没有可执行效果",
				)
			}
			// 只有单体目标依赖客户端提交的槽位。自身、范围和随机范围会在技能实际执行时
			// 根据最新场上站位解析目标；提前校验占位 target 会错误拒绝这些合法行动。
			if skill.targetScope() == SkillTargetScopeSelectedTarget {
				if action.UseSkill.Target.Side == action.Actor.Side {
					return nil, commandError(
						TurnCommandErrorInvalidTarget, actionField+"/useSkill/target", "单体技能目标必须属于对方",
					)
				}
				if _, targetExists := state.ActiveMember(action.UseSkill.Target); !targetExists {
					return nil, commandError(TurnCommandErrorInvalidTarget, actionField+"/useSkill/target", "目标槽位不存在")
				}
			}
			plans = append(plans, actionPlan{
				action: action, member: member, priority: int16(skill.Priority),
				orderBracket: skillActionOrderBracket(member, skill), speed: state.effectiveActionSpeed(action.Actor.Side, member),
			})
		case ActionKindSwitch:
			if action.Switch == nil || action.UseSkill != nil || !action.Switch.MemberPosition.Valid() {
				return nil, commandError(TurnCommandErrorInvalidActionShape, actionField, "替换行动载荷无效")
			}
			if member.CurrentHP > 0 && (forcedSkillPosition != 0 || member.RechargeTurnsRemaining != 0 || (member.BindingTurnsRemaining != 0 && !member.SwitchRestrictionImmunity)) {
				return nil, commandError(TurnCommandErrorSwitchPrevented, actionField+"/kind", "当前易变状态禁止主动换人")
			}
			if member.CurrentHP > 0 && opponentSwitchRestrictionPreventsSwitch(state, action.Actor.Side, member) {
				return nil, commandError(TurnCommandErrorSwitchPrevented, actionField+"/kind", "对手特性禁止当前成员主动换人")
			}
			target, targetExists := state.member(action.Actor.Side, action.Switch.MemberPosition)
			if !targetExists || target.CurrentHP == 0 || state.isActive(action.Actor.Side, target.Position) {
				return nil, commandError(
					TurnCommandErrorInvalidTarget, actionField+"/switch/memberPosition", "替换目标不可上场",
				)
			}
			targetRef := MemberRef{Side: action.Actor.Side, Position: target.Position}
			if _, duplicate := seenSwitchTargets[targetRef]; duplicate {
				return nil, commandError(
					TurnCommandErrorDuplicateSwitchTarget, actionField+"/switch/memberPosition", "与其它槽位选择了同一替换目标",
				)
			}
			seenSwitchTargets[targetRef] = struct{}{}
			if member.CurrentHP == 0 {
				faintedSwitches++
			}
			plans = append(plans, actionPlan{action: action, member: member, priority: 128, speed: state.effectiveActionSpeed(action.Actor.Side, member)})
		default:
			return nil, commandError(TurnCommandErrorUnsupportedActionKind, actionField+"/kind", "行动种类不受支持")
		}
	}
	if len(seenLiveActors) != len(liveActors) {
		return nil, commandError(TurnCommandErrorIncompleteActions, "/actions", "仍有可战斗场上成员未提交行动")
	}
	if faintedSwitches != requiredFaintedSwitches {
		return nil, commandError(TurnCommandErrorIncompleteActions, "/actions", "倒下槽位未完成所需补位")
	}
	return plans, nil
}

// skillActionOrderBracket 返回技能在相同有效优先度内应使用的确定性排序层级。
//
// 变化技能后置特性只降低同优先度内的顺序，因此返回 -1 而不是篡改 SkillSnapshot.Priority。这样正负基础优先度
// 仍由 priority 主排序键决定，速度、戏法空间和同速随机也只会在同一排序层级内参与结算。
func skillActionOrderBracket(member MemberSnapshot, skill SkillSnapshot) int8 {
	if member.ItemID != 0 && member.HeldItemForcedLastActionOrder {
		return -1
	}
	if member.StatusSkillMovesLastAndIgnoresTargetAbility && skill.DamageClass == DamageClassStatus {
		return -1
	}
	return 0
}

// pendingTurnActors 返回本回合必须提交行动的可战斗槽位、可选择补位的倒下槽位，以及必须实际完成的补位数量。
//
// 双打可能同时出现多个倒下槽位但后备成员数量不足：此时只要求与可用后备数相同数量的补位，其余空槽不再
// 强制提交虚假的技能或换人行动。活着的场上成员始终必须提交行动；倒下但没有后备可换的槽位不会阻塞回合。
func pendingTurnActors(state State) (map[SlotRef]struct{}, map[SlotRef]struct{}, int) {
	liveActors := make(map[SlotRef]struct{}, int(state.format.ActiveSlotsPerSide)*2)
	faintedCandidates := make(map[SlotRef]struct{}, int(state.format.ActiveSlotsPerSide)*2)
	requiredFaintedSwitches := 0
	for _, side := range state.sides {
		availableReserves := 0
		for _, member := range side.Members {
			if member.CurrentHP > 0 && !state.isActive(side.Side, member.Position) {
				availableReserves++
			}
		}
		faintedSlots := 0
		for index, position := range side.ActiveMembers {
			slot := SlotRef{Side: side.Side, Position: SlotPosition(index + 1)}
			member, exists := state.member(side.Side, position)
			if !exists || member.CurrentHP == 0 {
				faintedCandidates[slot] = struct{}{}
				faintedSlots++
				continue
			}
			liveActors[slot] = struct{}{}
		}
		requiredFaintedSwitches += min(faintedSlots, availableReserves)
	}
	return liveActors, faintedCandidates, requiredFaintedSwitches
}

// forcedSkillPosition 返回成员因蓄力或锁招必须选择的技能槽。蓄力优先于锁招，因为一项已经开始的
// 两段技能不能被随后写入的锁招状态覆盖；状态写入代码也不会制造二者的冲突组合。
func (member MemberSnapshot) forcedSkillPosition() SkillPosition {
	if member.ChargingTurnsRemaining != 0 {
		return member.ChargingSkillPosition
	}
	if member.LockedTurnsRemaining != 0 {
		return member.LockedSkillPosition
	}
	return 0
}

// chargingApplication 返回技能声明的唯一蓄力控制规则。State 在创建时已经拒绝重复状态声明，因而
// 该查询不需要依赖名称、说明文本或资料数组位置。
func (skill SkillSnapshot) chargingApplication() (VolatileStatusApplication, bool) {
	for _, application := range skill.VolatileStatusApplications {
		if application.Status == VolatileStatusCharging {
			return application, true
		}
	}
	return VolatileStatusApplication{}, false
}

// withoutRepeatedControlApplications 在完成蓄力或锁招时移除会把同一控制状态重新写入自己的资料项。
// 它只返回本次结算使用的临时值，不改变冻结技能快照，因而离线重放仍可从原始 State 精确重建。
func withoutRepeatedControlApplications(skill SkillSnapshot, charge, lock bool) SkillSnapshot {
	filtered := make([]VolatileStatusApplication, 0, len(skill.VolatileStatusApplications))
	for _, application := range skill.VolatileStatusApplications {
		if charge && application.Status == VolatileStatusCharging || lock && application.Status == VolatileStatusLockedMove {
			continue
		}
		filtered = append(filtered, application)
	}
	skill.VolatileStatusApplications = filtered
	return skill
}

// resolvePreMoveVolatileStatuses 在主要异常判定之后、PP 消耗之前处理易变状态的行动时机。
//
// 混乱、挑衅和定身生成 SkillPreventedEvent 时均不消耗 PP；蓄力和锁招则在此处解除上一回合写入的
// 强制动作记录，使后续普通技能路径能够继续结算伤害。持续时间只在持有者实际获得行动机会时递减，
// 因而倒下、换出或因更高优先级直接终局不会悄悄消耗状态。
func resolvePreMoveVolatileStatuses(
	state State,
	action Action,
	actor MemberSnapshot,
	skill SkillSnapshot,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, bool, bool, bool, error) {
	actorRef := MemberRef{Side: action.Actor.Side, Position: actor.Position}
	events := make([]Event, 0, 4)
	trace := make([]RandomTraceEntry, 0, 1)
	clear := func(status VolatileStatus, position SkillPosition) {
		events = append(events, VolatileStatusClearedEvent{
			Type: EventKindVolatileStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: actorRef, Status: status, SkillPosition: position,
		})
	}

	if actor.ConfusionTurnsRemaining != 0 {
		before := actor.ConfusionTurnsRemaining
		actor.ConfusionTurnsRemaining--
		if actor.ConfusionTurnsRemaining == 0 {
			clear(VolatileStatusConfusion, 0)
		}
		roll, nextRandom, entry, err := random.Next(3, fmt.Sprintf("confusion chance for side %d member %d", action.Actor.Side, actor.Position))
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, false, false, err
		}
		random = nextRandom
		trace = append(trace, entry)
		state.replaceMember(action.Actor.Side, actor)
		if roll == 0 {
			// 混乱仍会阻止本次行动并保留已消费的随机轨迹；间接伤害免疫只取消随后的自伤与倒下事件。
			if actor.IndirectDamageImmunity {
				events = append(events, SkillPreventedEvent{
					Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: actorRef, Reason: SkillPreventionReasonConfusion, TurnsRemainingBefore: int32(before),
				})
				return state, random, events, trace, true, false, false, nil
			}
			damage := min(max(actor.MaxHP/8, 1), actor.CurrentHP)
			actor.CurrentHP -= damage
			state.replaceMember(action.Actor.Side, actor)
			events = append(events, SkillPreventedEvent{
				Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Reason: SkillPreventionReasonConfusion, TurnsRemainingBefore: int32(before),
			})
			events = append(events, VolatileStatusDamageAppliedEvent{
				Type: EventKindVolatileStatusDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: actorRef, Status: VolatileStatusConfusion, Amount: damage, CurrentHP: actor.CurrentHP,
			})
			if actor.CurrentHP == 0 {
				events = append(events, ParticipantFaintedEvent{
					Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: actorRef, Cause: FaintCauseVolatileStatusDamage, VolatileStatus: VolatileStatusConfusion,
				})
			}
			return state, random, events, trace, true, false, false, nil
		}
	}

	if actor.DisabledTurnsRemaining != 0 {
		before := actor.DisabledTurnsRemaining
		position := actor.DisabledSkillPosition
		actor.DisabledTurnsRemaining--
		if actor.DisabledTurnsRemaining == 0 {
			actor.DisabledSkillPosition = 0
			clear(VolatileStatusDisable, position)
		}
		state.replaceMember(action.Actor.Side, actor)
		if skill.Position == position {
			events = append(events, SkillPreventedEvent{
				Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Reason: SkillPreventionReasonDisable, TurnsRemainingBefore: int32(before),
			})
			return state, random, events, trace, true, false, false, nil
		}
	}

	if actor.TauntTurnsRemaining != 0 {
		before := actor.TauntTurnsRemaining
		actor.TauntTurnsRemaining--
		if actor.TauntTurnsRemaining == 0 {
			clear(VolatileStatusTaunt, 0)
		}
		state.replaceMember(action.Actor.Side, actor)
		if skill.DamageClass == DamageClassStatus {
			events = append(events, SkillPreventedEvent{
				Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Reason: SkillPreventionReasonTaunt, TurnsRemainingBefore: int32(before),
			})
			return state, random, events, trace, true, false, false, nil
		}
	}

	completingCharge := false
	if actor.ChargingTurnsRemaining != 0 {
		position := actor.ChargingSkillPosition
		actor.ChargingTurnsRemaining--
		if actor.ChargingTurnsRemaining == 0 {
			actor.ChargingSkillPosition = 0
			clear(VolatileStatusCharging, position)
		}
		state.replaceMember(action.Actor.Side, actor)
		completingCharge = true
	}
	completingLock := false
	if actor.LockedTurnsRemaining != 0 {
		position := actor.LockedSkillPosition
		actor.LockedTurnsRemaining--
		if actor.LockedTurnsRemaining == 0 {
			actor.LockedSkillPosition = 0
			clear(VolatileStatusLockedMove, position)
		}
		state.replaceMember(action.Actor.Side, actor)
		completingLock = true
	}
	return state, random, events, trace, false, completingCharge, completingLock, nil
}

// effectiveSpeed 返回成员当前主要异常修正后的行动排序速度。
// 麻痹会把速度向下取整为原值的一半，但有效速度始终至少为 1；其它状态不修改速度。
func effectiveSpeed(member MemberSnapshot) uint32 {
	speed := modifiedBattleStat(member.Stats.Speed, member.StatStages[StatSpeed])
	if member.MajorStatus == MajorStatusParalysis {
		speed = max(speed/2, 1)
	}
	if member.ItemID != 0 && member.HeldItemSpeedHalf {
		speed = max(speed/2, 1)
	}
	if member.ItemID != 0 && member.HeldItemSpeedBoost50 {
		speed = saturatingMulDiv(speed, 3, 2)
	}
	return speed
}

// saturatingMulDiv 对无符号运行态能力值执行先乘后除，并在超出 uint32 时饱和，避免道具倍率产生回绕。
func saturatingMulDiv(value uint32, numerator uint64, denominator uint64) uint32 {
	if denominator == 0 {
		return math.MaxUint32
	}
	result := uint64(value) * numerator / denominator
	if result > math.MaxUint32 {
		return math.MaxUint32
	}
	return uint32(result)
}

// effectiveActionSpeed 返回成员用于本回合行动排序的最终有效速度。
//
// 主要异常和能力阶级先由 effectiveSpeed 处理；匹配天气的特性倍率、环境最高原始能力倍率随后依次生效，
// 顺风最后只对成员所属方施加两倍修正。天气封锁时不会读取天气相关的两类规则；所有乘法使用显式饱和逻辑，
// 避免将异常大的输入速度溢出为较小数值而改变确定性行动顺序。
func (state State) effectiveActionSpeed(sideID Side, member MemberSnapshot) uint32 {
	speed := effectiveSpeed(member)
	weather := effectiveWeather(state)
	if weather != nil {
		if multiplier, found := weatherSpeedMultiplier(member, weather.Kind); found {
			speed = applySpeedMultiplier(speed, multiplier)
		}
	}
	numerator, denominator := highestStatMultiplier(member, weather, state.environment.Terrain, StatSpeed)
	speed = applyHighestStatMultiplier(speed, numerator, denominator)
	for _, side := range state.sides {
		if side.Side != sideID || side.Conditions.Tailwind == nil {
			continue
		}
		if speed > ^uint32(0)/2 {
			return ^uint32(0)
		}
		return speed * 2
	}
	return speed
}

// sideConditions 返回指定阵营当前的侧状态只读快照。State 已在每次回合结算开始时深复制全部侧状态；这里仍返回
// 值副本，避免伤害计算等纯函数持有可变阵营切片中的结构体地址。
func (state State) sideConditions(sideID Side) SideConditionSnapshot {
	for _, side := range state.sides {
		if side.Side == sideID {
			return cloneSideConditions(side.Conditions)
		}
	}
	return SideConditionSnapshot{}
}

// modifiedBattleStat 按现代规则的普通能力阶级曲线修正基础能力值，结果向下取整且至少为 1。
func modifiedBattleStat(base uint32, stage int8) uint32 {
	numerator := uint64(2)
	denominator := uint64(2)
	if stage >= 0 {
		numerator += uint64(stage)
	} else {
		denominator += uint64(-stage)
	}
	return max(uint32(uint64(base)*numerator/denominator), 1)
}

func commandError(code TurnCommandErrorCode, field, message string) error {
	return &TurnCommandError{Code: code, Field: field, Message: message}
}

func resolveAction(
	state State,
	action Action,
	random RandomSource,
	turnEvents []Event,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	if action.Kind == ActionKindSwitch {
		return resolveSwitch(state, action, random)
	}
	return resolveUseSkill(state, action, random, turnEvents)
}

func resolveSwitch(state State, action Action, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	return resolveMemberSwitch(state, action.Actor, action.Switch.MemberPosition, random, false)
}

// resolveMemberSwitch 让指定场上槽位完成一次已经选定后备成员的替换，并统一结算换出清理、入场危害、入场特性、
// 强天气、普通天气/场地、形态与道具身份同步。主动换人、倒下补位和技能强制换人都必须经过本函数，避免其中任一
// 路径遗漏换入生命周期；forced 只描述事件来源，不改变成员、环境或特性结算规则。
func resolveMemberSwitch(
	state State,
	slot SlotRef,
	nextPosition MemberPosition,
	random RandomSource,
	forced bool,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	previous, ok := state.ActiveMember(slot)
	if !ok {
		return state, random, nil, nil, nil
	}
	next, ok := state.member(slot.Side, nextPosition)
	if !ok || next.CurrentHP == 0 {
		return State{}, RandomSource{}, nil, nil, commandError(
			TurnCommandErrorInvalidTarget, "/actions/*/switch/memberPosition", "替换目标当前不可上场",
		)
	}
	var switchOutEvents []Event
	if previous.CurrentHP > 0 {
		state, switchOutEvents = resolveSwitchOutAbilities(state, slot)
		previous, _ = state.ActiveMember(slot)
	}
	previousRef := MemberRef{Side: slot.Side, Position: previous.Position}
	state = clearAccuracyLocksPointingToMember(state, previousRef)
	previous = leaveBattlefield(previous)
	state.replaceMember(slot.Side, previous)
	state.switchActive(slot, next.Position)
	event := ParticipantSwitchedEvent{
		Type: EventKindParticipantSwitched, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Slot:           slot,
		PreviousMember: MemberRef{Side: slot.Side, Position: previous.Position},
		NextMember:     MemberRef{Side: slot.Side, Position: next.Position},
		Forced:         forced || previous.CurrentHP == 0,
	}
	state, entryHazardEvents := resolveEntryHazardsOnSwitchIn(state, slot)
	// 换出成员可能是当前强天气的最后来源。先同步旧强天气，才能让本次换入成员的普通天气特性在
	// 强天气已经结束时生效；随后再处理新成员的强天气，二者不会在同一成员资料上同时声明。
	state, previousStrongWeatherEvents := synchronizeStrongWeather(state)
	state, switchInFormEvents := resolveSwitchInFormChange(state, slot)
	state, abilityStatStageEvents := resolveSwitchInStatStageChange(state, slot)
	state, abilityAllyHealingEvents := resolveSwitchInAllyHeal(state, slot)
	state, abilityDefenseComparisonEvents := resolveSwitchInOpponentDefenseComparisonBoost(state, slot)
	state, abilityAllyStatStageCopyEvents := resolveSwitchInAllyStatStageCopy(state, slot)
	state, abilityAllyStatStageResetEvents := resolveSwitchInAllyStatStageReset(state, slot)
	state, abilitySideDamageReductionEvents := resolveSwitchInClearAllSideDamageReductions(state, slot)
	state, abilityCopiedEvents := resolveSwitchInCopyOpponentAbility(state, slot)
	state, opponentHeldItemEvents := resolveSwitchInRevealOpponentHeldItems(state, slot)
	state, opponentHighestPowerSkillEvents := resolveSwitchInRevealOpponentHighestPowerSkill(state, slot)
	state, transformEvents := resolveSwitchInTransformIntoOpponent(state, slot)
	state, dangerousSkillEvents := resolveSwitchInDetectDangerousOpponentSkill(state, slot)
	state = resolveSwitchInDisguiseAsLastHealthyAlly(state, slot)
	state, abilityWeatherEvents := resolveSwitchInWeather(state, slot)
	state, abilityTerrainEvents := resolveSwitchInTerrain(state, slot)
	state, strongWeatherEvents := resolveStrongWeatherOnSwitchIn(state, slot)
	state, weatherFormEvents := synchronizeWeatherForms(state)
	state, heldItemElementIdentityEvents := resolveSwitchInHeldItemElementIdentity(state, slot)
	state, heldItemHighestStatBoostEvents := resolveHeldItemHighestStatBoost(state, slot)
	events := append(switchOutEvents, event)
	events = append(events, entryHazardEvents...)
	events = append(events, previousStrongWeatherEvents...)
	events = append(events, switchInFormEvents...)
	events = append(events, abilityStatStageEvents...)
	events = append(events, abilityAllyHealingEvents...)
	events = append(events, abilityDefenseComparisonEvents...)
	events = append(events, abilityAllyStatStageCopyEvents...)
	events = append(events, abilityAllyStatStageResetEvents...)
	events = append(events, abilitySideDamageReductionEvents...)
	events = append(events, abilityCopiedEvents...)
	events = append(events, opponentHeldItemEvents...)
	events = append(events, opponentHighestPowerSkillEvents...)
	events = append(events, transformEvents...)
	events = append(events, dangerousSkillEvents...)
	events = append(events, abilityWeatherEvents...)
	events = append(events, abilityTerrainEvents...)
	events = append(events, strongWeatherEvents...)
	events = append(events, weatherFormEvents...)
	events = append(events, heldItemElementIdentityEvents...)
	events = append(events, heldItemHighestStatBoostEvents...)
	return state, random, events, nil, nil
}

// resolveEntryHazardsOnSwitchIn 结算一名成员已经实际占用场上槽位后触发的入场危害。
//
// 危害读取换入方的侧状态，而不是换出成员、技能使用者或全场环境；因此主动换人、倒下补位和未来强制换人会共享
// 完全相同的规则。隐形岩、撒菱、毒菱和黏黏网按各自的属性倍率、层数、异常或能力阶级语义独立结算，不能收敛
// 为“通用入场效果”循环；任一伤害危害令成员倒下后，后续危害不再继续作用于该成员。
func resolveEntryHazardsOnSwitchIn(state State, slot SlotRef) (State, []Event) {
	member, exists := state.ActiveMember(slot)
	if !exists || member.CurrentHP == 0 {
		return state, nil
	}
	if member.ItemID != 0 && member.HeldItemEntryHazardImmunity {
		return state, nil
	}
	conditions := state.sideConditions(slot.Side)
	ref := MemberRef{Side: slot.Side, Position: member.Position}
	events := make([]Event, 0, 6)
	appendEntryHazardFaint := func(current MemberSnapshot) bool {
		if current.CurrentHP != 0 {
			return false
		}
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref, Cause: FaintCauseEntryHazard,
		})
		return true
	}

	if conditions.StealthRock && !member.IndirectDamageImmunity {
		rockElementID := state.rules.ElementIDs["rock"]
		// NewState 已拒绝缺少岩石属性 ID 的隐形岩状态，因此这里的稳定 ID 一定可用于读取冻结相性表。
		effectivenessNumerator, effectivenessDenominator := uint64(1), uint64(1)
		for _, defenseElementID := range member.ElementIDs {
			numerator, denominator := state.rules.effectiveness(rockElementID, defenseElementID)
			effectivenessNumerator *= uint64(numerator)
			effectivenessDenominator *= uint64(denominator)
		}
		if effectivenessNumerator != 0 {
			damage := uint32(uint64(member.MaxHP) * effectivenessNumerator / (uint64(8) * effectivenessDenominator))
			damage = min(max(damage, 1), member.CurrentHP)
			member.CurrentHP -= damage
			state.replaceMember(slot.Side, member)
			events = append(events, StealthRockDamageAppliedEvent{
				Type: EventKindStealthRockDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref,
				EffectivenessNumerator: uint32(effectivenessNumerator), EffectivenessDenominator: uint32(effectivenessDenominator),
				Amount: damage, CurrentHP: member.CurrentHP,
			})
			if appendEntryHazardFaint(member) {
				return state, events
			}
		}
	}

	grounded := memberGrounded(state.rules, member)
	if conditions.SpikesLayers != 0 && grounded && !member.IndirectDamageImmunity {
		denominator := uint32(8)
		switch conditions.SpikesLayers {
		case 2:
			denominator = 6
		case 3:
			denominator = 4
		}
		damage := min(max(member.MaxHP/denominator, 1), member.CurrentHP)
		member.CurrentHP -= damage
		state.replaceMember(slot.Side, member)
		events = append(events, SpikesDamageAppliedEvent{
			Type: EventKindSpikesDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref,
			Layers: conditions.SpikesLayers, Amount: damage, CurrentHP: member.CurrentHP,
		})
		if appendEntryHazardFaint(member) {
			return state, events
		}
	}

	if conditions.ToxicSpikesLayers != 0 && grounded {
		poisonElementID := state.rules.ElementIDs["poison"]
		if poisonElementID != 0 && containsString(member.ElementIDs, poisonElementID) {
			for index := range state.sides {
				if state.sides[index].Side == slot.Side {
					state.sides[index].Conditions.ToxicSpikesLayers = 0
					break
				}
			}
			events = append(events, ToxicSpikesAbsorbedEvent{
				Type: EventKindToxicSpikesAbsorbed, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref, Layers: conditions.ToxicSpikesLayers,
			})
		} else {
			status := MajorStatusPoison
			if conditions.ToxicSpikesLayers >= 2 {
				status = MajorStatusBadPoison
			}
			if member.MajorStatus == "" && !majorStatusBlockedByElement(state.rules, member, status) &&
				!terrainBlocksMajorStatus(state.environment.Terrain, state.rules, member, status) {
				member.MajorStatus = status
				if status == MajorStatusBadPoison {
					member.BadPoisonCounter = 1
				}
				state.replaceMember(slot.Side, member)
				events = append(events, ToxicSpikesStatusAppliedEvent{
					Type: EventKindToxicSpikesStatusApplied, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref,
					Layers: conditions.ToxicSpikesLayers, Status: status,
				})
			}
		}
	}

	if conditions.StickyWeb && grounded {
		before := member.StatStages[StatSpeed]
		after := max(int8(-6), before-1)
		if before != after {
			stages := make(map[Stat]int8, len(member.StatStages)+1)
			for stat, stage := range member.StatStages {
				stages[stat] = stage
			}
			stages[StatSpeed] = after
			member.StatStages = stages
			state.replaceMember(slot.Side, member)
			events = append(events, StickyWebSpeedLoweredEvent{
				Type: EventKindStickyWebSpeedLowered, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref,
				Delta: after - before, CurrentStage: after,
			})
		}
	}
	return state, events
}

func detectBattleResult(state State) (BattleResult, bool) {
	remaining := make(map[Side]bool, 2)
	for _, side := range state.sides {
		for _, member := range side.Members {
			if member.CurrentHP > 0 {
				remaining[side.Side] = true
				break
			}
		}
	}
	if remaining[SideOne] && remaining[SideTwo] {
		return BattleResult{}, false
	}
	if remaining[SideOne] {
		return BattleResult{WinningSide: SideOne, Reason: BattleResultReasonAllMembersFainted}, true
	}
	if remaining[SideTwo] {
		return BattleResult{WinningSide: SideTwo, Reason: BattleResultReasonAllMembersFainted}, true
	}
	return BattleResult{Reason: BattleResultReasonAllMembersFainted}, true
}

func cloneBattleResult(result *BattleResult) *BattleResult {
	if result == nil {
		return nil
	}
	cloned := *result
	return &cloned
}

func resolveUseSkill(
	state State,
	action Action,
	random RandomSource,
	turnEvents []Event,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	actor, ok := state.ActiveMember(action.Actor)
	if !ok || actor.CurrentHP == 0 {
		return state, random, nil, nil, nil
	}
	skillIndex := int(action.UseSkill.SkillPosition - 1)
	skill := actor.Skills[skillIndex]
	trace := make([]RandomTraceEntry, 0, 4)
	preMoveEvents := make([]Event, 0, 1)
	// 休整必须优先于所有其它行动前判定。这样即使成员同时睡眠、冰冻、畏缩或混乱，也只消费休整且不消耗
	// 任何随机数或本次提交技能的 PP，完全保留下一次行动的其它状态语义。
	if actor.RechargeTurnsRemaining != 0 {
		before := actor.RechargeTurnsRemaining
		actor.RechargeTurnsRemaining--
		state.replaceMember(action.Actor.Side, actor)
		return state, random, []Event{SkillPreventedEvent{
			Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor:  MemberRef{Side: action.Actor.Side, Position: actor.Position},
			Reason: SkillPreventionReasonRecharge, TurnsRemainingBefore: int32(before),
		}}, trace, nil
	}
	if actor.FlinchedTurn == state.turnNumber {
		return state, random, []Event{SkillPreventedEvent{
			Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: MemberRef{Side: action.Actor.Side, Position: actor.Position}, Reason: SkillPreventionReasonFlinch,
		}}, trace, nil
	}
	if actor.MajorStatus == MajorStatusSleep {
		before := actor.SleepTurnsRemaining
		actor.SleepTurnsRemaining--
		cleared := actor.SleepTurnsRemaining == 0
		if cleared {
			actor.MajorStatus = ""
		}
		state.replaceMember(action.Actor.Side, actor)
		actorRef := MemberRef{Side: action.Actor.Side, Position: actor.Position}
		events := []Event{SkillPreventedEvent{
			Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Reason: SkillPreventionReasonSleep, TurnsRemainingBefore: before,
		}}
		if cleared {
			events = append(events, MajorStatusClearedEvent{
				Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: actorRef, Status: MajorStatusSleep,
			})
		}
		return state, random, events, trace, nil
	}
	if actor.MajorStatus == MajorStatusFreeze {
		reason := fmt.Sprintf("freeze thaw chance for side %d member %d", action.Actor.Side, actor.Position)
		roll, nextRandom, entry, err := random.Next(100, reason)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		actorRef := MemberRef{Side: action.Actor.Side, Position: actor.Position}
		if roll+1 > 20 {
			event := SkillPreventedEvent{
				Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Reason: SkillPreventionReasonFreeze,
			}
			return state, random, []Event{event}, trace, nil
		}
		actor.MajorStatus = ""
		state.replaceMember(action.Actor.Side, actor)
		preMoveEvents = append(preMoveEvents, MajorStatusClearedEvent{
			Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: actorRef, Status: MajorStatusFreeze,
		})
	}
	if actor.MajorStatus == MajorStatusParalysis {
		reason := fmt.Sprintf("paralysis chance for side %d member %d", action.Actor.Side, actor.Position)
		roll, nextRandom, entry, err := random.Next(100, reason)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 <= 25 {
			event := SkillPreventedEvent{
				Type: EventKindSkillPrevented, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor:  MemberRef{Side: action.Actor.Side, Position: actor.Position},
				Reason: SkillPreventionReasonParalysis,
			}
			return state, random, []Event{event}, trace, nil
		}
	}
	var volatileEvents []Event
	var volatileTrace []RandomTraceEntry
	var preventedByVolatile bool
	var completingCharge bool
	var completingLock bool
	state, random, volatileEvents, volatileTrace, preventedByVolatile, completingCharge, completingLock, err := resolvePreMoveVolatileStatuses(
		state, action, actor, skill, random,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, err
	}
	preMoveEvents = append(preMoveEvents, volatileEvents...)
	trace = append(trace, volatileTrace...)
	if preventedByVolatile {
		return state, random, preMoveEvents, trace, nil
	}
	// 易变状态处理会更新持续时间和关联技能槽；重新读取权威成员，避免随后 PP 写入用处理前的局部副本
	// 覆盖刚刚解除的蓄力、锁招或定身状态。
	actor, ok = state.ActiveMember(action.Actor)
	if !ok || actor.CurrentHP == 0 {
		return state, random, preMoveEvents, trace, nil
	}
	skill = actor.Skills[skillIndex]
	// 蓄力与锁招的第二次使用不应把同一控制状态重新写回自身；其余伤害、目标与附加效果仍按原技能
	// 完整结算。复制技能快照而不修改 State，保证历史回放输入保持不可变。
	if completingCharge || completingLock {
		skill = withoutRepeatedControlApplications(skill, completingCharge, completingLock)
	}
	if action.UseSkill.Terastallize {
		var terastallizationEvents []Event
		state, terastallizationEvents = applyTerastallization(state, action.Actor)
		preMoveEvents = append(preMoveEvents, terastallizationEvents...)
		// 太晶化会改写成员属性，但不会改变技能槽、PP 或当前生命；仍重新读取权威成员，避免后续局部副本
		// 在写入 PP 时覆盖刚刚建立的太晶运行态。
		actor, ok = state.ActiveMember(action.Actor)
		if !ok || actor.CurrentHP == 0 {
			return state, random, preMoveEvents, trace, nil
		}
		skill = actor.Skills[skillIndex]
	}
	skippedChargeWithItem := false
	if !completingCharge && !weatherSkipsCharge(skill, effectiveSkillWeather(state, actor)) {
		if charge, found := skill.chargingApplication(); found {
			actor.Skills[skillIndex].RemainingPP--
			actor.LastUsedSkillPosition = skill.Position
			actor.LastSkillActionTurn = state.turnNumber
			actor = recordDeclaredSkillUse(actor, skill.SkillID)
			if actor.ItemID != 0 && actor.HeldItemChoiceSkillLock && actor.HeldItemChoiceLockedSkillPosition == 0 {
				actor.HeldItemChoiceLockedSkillPosition = skill.Position
			}
			state.replaceMember(action.Actor.Side, actor)
			actorRef := MemberRef{Side: action.Actor.Side, Position: actor.Position}
			events := append(preMoveEvents, SkillUsedEvent{
				Type: EventKindSkillUsed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: action.UseSkill.Target, SkillPosition: skill.Position,
				SkillID: skill.SkillID, RemainingPP: actor.Skills[skillIndex].RemainingPP,
			})
			var chargeSkipEvents []Event
			var skipped bool
			state, chargeSkipEvents, skipped = applyChargeSkipOnceItem(state, actorRef, skill)
			events = append(events, chargeSkipEvents...)
			if skipped {
				// PP 已在上方消费，且跳过事件已经明确记录；继续走同一套命中和伤害流程，避免伪造蓄力状态。
				preMoveEvents = events
				skippedChargeWithItem = true
				actor, ok = state.ActiveMember(action.Actor)
				if !ok || actor.CurrentHP == 0 {
					return state, random, preMoveEvents, trace, nil
				}
			} else {
				actor.ChargingSkillPosition = skill.Position
				actor.ChargingTurnsRemaining = charge.MinTurns
				state.replaceMember(action.Actor.Side, actor)
				events = append(events, VolatileStatusAppliedEvent{
					Type: EventKindVolatileStatusApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: actorRef, Target: actorRef, SkillID: skill.SkillID, Status: VolatileStatusCharging,
					TurnsRemaining: charge.MinTurns, SkillPosition: skill.Position,
				})
				return state, random, events, trace, nil
			}
		}
	}
	// 两段蓄力技能的 PP 在准备回合已经消费；完成段仍会宣告并结算完整技能，但不能重复扣除。
	if !completingCharge && !skippedChargeWithItem {
		actor.Skills[skillIndex].RemainingPP--
	}
	actor.LastUsedSkillPosition = skill.Position
	actor.LastSkillActionTurn = state.turnNumber
	actor = recordDeclaredSkillUse(actor, skill.SkillID)
	if actor.ItemID != 0 && actor.HeldItemChoiceSkillLock && actor.HeldItemChoiceLockedSkillPosition == 0 {
		actor.HeldItemChoiceLockedSkillPosition = skill.Position
	}
	state.replaceMember(action.Actor.Side, actor)
	actorRef := MemberRef{Side: action.Actor.Side, Position: actor.Position}
	events := append([]Event(nil), preMoveEvents...)
	if !skippedChargeWithItem {
		events = append(events, SkillUsedEvent{
			Type: EventKindSkillUsed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: action.UseSkill.Target, SkillPosition: skill.Position,
			SkillID: skill.SkillID, RemainingPP: actor.Skills[skillIndex].RemainingPP,
		})
	}

	var receivedDamage *receivedDamageHit
	var targets []SlotRef
	if skill.damageMode() == SkillDamageModeReceivedDamage {
		// 反打技能不接受客户端提交的普通目标：它必须命中最近一段合格伤害的实际来源。此处在保护和命中
		// 判定之前完成重定向，确保后续结算对真实来源读取当前状态。
		memory, found := latestReceivedDamage(state, turnEvents, action.Actor, skill)
		if !found {
			return state, random, append(events, SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonReceivedDamageMemoryUnavailable,
			}), trace, nil
		}
		receivedDamage = &memory
		targets = []SlotRef{memory.sourceSlot}
	} else {
		nextTargets, nextRandom, targetTrace, err := resolveSkillTargets(
			state, action.Actor, action.UseSkill.Target, skill, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		targets = nextTargets
		random = nextRandom
		trace = append(trace, targetTrace...)
	}
	// 范围伤害是否使用 0.75 修正只取决于本次技能开始时的完整目标集合。后续某一目标
	// 未命中或倒下不能反向改变其它目标的伤害倍率。
	multiTargetDamage := skill.targetScope().canAffectMultipleTargets() && len(targets) > 1
	// 单目标和范围中只剩一名目标时保持已有附加效果声明顺序；真正多目标时把 user
	// 效果延后到所有 selectedTarget 效果之后，防止同一项自身效果因逐目标循环重复触发。
	includeUserEffects := len(targets) <= 1
	processedTarget := false
	for _, targetSlot := range targets {
		var targetEvents []Event
		var targetRandomTrace []RandomTraceEntry
		var processed bool
		state, random, targetEvents, targetRandomTrace, processed, err = resolveSkillTarget(
			state, action.Actor, targetSlot, skill, random, multiTargetDamage, includeUserEffects, receivedDamage,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		processedTarget = processedTarget || processed
		events = append(events, targetEvents...)
		trace = append(trace, targetRandomTrace...)
	}
	if !includeUserEffects && processedTarget {
		var statusEvents []Event
		var statusTrace []RandomTraceEntry
		state, random, statusEvents, statusTrace, err = resolveMajorStatusApplications(
			state, actorRef, actorRef, skill, random, false, true,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, statusEvents...)
		trace = append(trace, statusTrace...)
		var stageEvents []Event
		var stageTrace []RandomTraceEntry
		state, random, stageEvents, stageTrace, err = resolveStatStageEffects(
			state, actorRef, actorRef, skill, random, false, true,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, stageEvents...)
		trace = append(trace, stageTrace...)
		var volatileEvents []Event
		var volatileTrace []RandomTraceEntry
		state, random, volatileEvents, volatileTrace, err = resolveVolatileStatusApplications(
			state, actorRef, actorRef, skill, random, false, true,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, volatileEvents...)
		trace = append(trace, volatileTrace...)
	}
	if processedTarget {
		var statusCureEvents []Event
		state, statusCureEvents = resolveMajorStatusCures(state, actorRef, skill)
		events = append(events, statusCureEvents...)
	}
	if processedTarget && skill.HealingPercent != 0 {
		var healthEvents []Event
		state, healthEvents = applyFixedSkillHealthEffect(state, actorRef, skill)
		events = append(events, healthEvents...)
	}
	if processedTarget && skill.FieldSpeedOrderApplication != nil {
		var fieldEvents []Event
		var fieldTrace []RandomTraceEntry
		state, random, fieldEvents, fieldTrace, err = applyFieldSpeedOrderApplication(
			state, actorRef, skill, *skill.FieldSpeedOrderApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, fieldEvents...)
		trace = append(trace, fieldTrace...)
		for _, event := range fieldEvents {
			started, ok := event.(FieldSpeedOrderStartedEvent)
			if !ok {
				continue
			}
			var heldItemEvents []Event
			state, heldItemEvents = applyHeldItemFieldSpeedOrderStatDrop(state, started.FieldSpeedOrderKind, skill.SkillID)
			events = append(events, heldItemEvents...)
		}
	}
	if processedTarget && skill.WeatherApplication != nil {
		var weatherEvents []Event
		var weatherTrace []RandomTraceEntry
		state, random, weatherEvents, weatherTrace, err = applyWeatherApplication(
			state, actorRef, skill, *skill.WeatherApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, weatherEvents...)
		trace = append(trace, weatherTrace...)
	}
	if processedTarget && skill.TerrainApplication != nil {
		var terrainEvents []Event
		var terrainTrace []RandomTraceEntry
		state, random, terrainEvents, terrainTrace, err = applyTerrainApplication(
			state, actorRef, skill, *skill.TerrainApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, terrainEvents...)
		trace = append(trace, terrainTrace...)
	}
	if processedTarget && skill.TailwindApplication != nil {
		var tailwindEvents []Event
		var tailwindTrace []RandomTraceEntry
		state, random, tailwindEvents, tailwindTrace, err = applyTailwindApplication(
			state, actorRef, skill, *skill.TailwindApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, tailwindEvents...)
		trace = append(trace, tailwindTrace...)
	}
	if processedTarget && skill.ReflectApplication != nil {
		var reflectEvents []Event
		var reflectTrace []RandomTraceEntry
		state, random, reflectEvents, reflectTrace, err = applyReflectApplication(
			state, actorRef, skill, *skill.ReflectApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, reflectEvents...)
		trace = append(trace, reflectTrace...)
	}
	if processedTarget && skill.LightScreenApplication != nil {
		var lightScreenEvents []Event
		var lightScreenTrace []RandomTraceEntry
		state, random, lightScreenEvents, lightScreenTrace, err = applyLightScreenApplication(
			state, actorRef, skill, *skill.LightScreenApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, lightScreenEvents...)
		trace = append(trace, lightScreenTrace...)
	}
	if processedTarget && skill.AuroraVeilApplication != nil {
		var auroraVeilEvents []Event
		var auroraVeilTrace []RandomTraceEntry
		state, random, auroraVeilEvents, auroraVeilTrace, err = applyAuroraVeilApplication(
			state, actorRef, skill, *skill.AuroraVeilApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, auroraVeilEvents...)
		trace = append(trace, auroraVeilTrace...)
	}
	if processedTarget && skill.SpikesApplication != nil {
		var spikesEvents []Event
		var spikesTrace []RandomTraceEntry
		state, random, spikesEvents, spikesTrace, err = applySpikesApplication(
			state, actorRef, action.UseSkill.Target.Side, skill, *skill.SpikesApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, spikesEvents...)
		trace = append(trace, spikesTrace...)
	}
	if processedTarget && skill.StealthRockApplication != nil {
		var stealthRockEvents []Event
		var stealthRockTrace []RandomTraceEntry
		state, random, stealthRockEvents, stealthRockTrace, err = applyStealthRockApplication(
			state, actorRef, action.UseSkill.Target.Side, skill, *skill.StealthRockApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, stealthRockEvents...)
		trace = append(trace, stealthRockTrace...)
	}
	if processedTarget && skill.ToxicSpikesApplication != nil {
		var toxicSpikesEvents []Event
		var toxicSpikesTrace []RandomTraceEntry
		state, random, toxicSpikesEvents, toxicSpikesTrace, err = applyToxicSpikesApplication(
			state, actorRef, action.UseSkill.Target.Side, skill, *skill.ToxicSpikesApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, toxicSpikesEvents...)
		trace = append(trace, toxicSpikesTrace...)
	}
	if processedTarget && skill.StickyWebApplication != nil {
		var stickyWebEvents []Event
		var stickyWebTrace []RandomTraceEntry
		state, random, stickyWebEvents, stickyWebTrace, err = applyStickyWebApplication(
			state, actorRef, action.UseSkill.Target.Side, skill, *skill.StickyWebApplication, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		events = append(events, stickyWebEvents...)
		trace = append(trace, stickyWebTrace...)
	}
	if processedTarget && skill.RapidSpinApplication != nil {
		var rapidSpinEvents []Event
		state, rapidSpinEvents = applyRapidSpinApplication(state, actorRef, skill)
		events = append(events, rapidSpinEvents...)
	}
	if processedTarget && skill.DefogApplication != nil {
		var defogEvents []Event
		state, defogEvents = applyDefogApplication(state, actorRef, action.UseSkill.Target.Side, skill)
		events = append(events, defogEvents...)
	}
	return state, random, events, trace, nil
}

// applyReflectApplication 在自身范围技能成功执行后尝试为使用者一方建立反射壁。
//
// 已存在反射壁时不能刷新回合数，必须记录稳定失败事件；概率小于 100 时只在此处消费一次随机数。反射壁写入
// 阵营侧状态而不是成员属性，因此己方成员换下后仍会保护新上场成员的普通物理伤害。
func applyReflectApplication(state State, actor MemberRef, skill SkillSnapshot, application ReflectApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "reflect chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != actor.Side {
			continue
		}
		if state.sides[index].Conditions.Reflect != nil {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor,
				SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonReflectAlreadyActive,
			}}, trace, nil
		}
		effect := application.Effect
		effect.TurnsRemaining = extendReflectDurationByHeldItem(state, actor, effect.TurnsRemaining)
		state.sides[index].Conditions.Reflect = &effect
		return state, random, []Event{ReflectStartedEvent{
			Type: EventKindReflectStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: actor.Side, TurnsRemaining: effect.TurnsRemaining,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 反射壁使用者阵营不存在", ErrInvalidInitialState)
}

// applyLightScreenApplication 在自身范围技能成功执行后尝试为使用者一方建立光墙。
//
// 光墙已存在时不能刷新持续回合，必须写入光墙专用失败原因；概率不足 100 时仅在这里消费一次随机数。光墙
// 写入阵营侧状态而不是成员属性，确保成员换下后仍保护同侧后续换入成员承受的普通特殊伤害。
func applyLightScreenApplication(state State, actor MemberRef, skill SkillSnapshot, application LightScreenApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "light screen chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != actor.Side {
			continue
		}
		if state.sides[index].Conditions.LightScreen != nil {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor,
				SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonLightScreenAlreadyActive,
			}}, trace, nil
		}
		effect := application.Effect
		effect.TurnsRemaining = extendLightScreenDurationByHeldItem(state, actor, effect.TurnsRemaining)
		state.sides[index].Conditions.LightScreen = &effect
		return state, random, []Event{LightScreenStartedEvent{
			Type: EventKindLightScreenStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: actor.Side, TurnsRemaining: effect.TurnsRemaining,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 光墙使用者阵营不存在", ErrInvalidInitialState)
}

// applyAuroraVeilApplication 在自身范围技能成功执行后尝试为使用者一方建立极光幕。
//
// 极光幕与单独物理或特殊屏障的减伤范围不同，因此使用独立状态、随机轨迹说明、失败原因和事件类型；它不能
// 通过“通用屏障”标签降级表达。已有效果同样不能被重用技能刷新。
func applyAuroraVeilApplication(state State, actor MemberRef, skill SkillSnapshot, application AuroraVeilApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "aurora veil chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != actor.Side {
			continue
		}
		if state.sides[index].Conditions.AuroraVeil != nil {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor,
				SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonAuroraVeilAlreadyActive,
			}}, trace, nil
		}
		effect := application.Effect
		effect.TurnsRemaining = extendAuroraVeilDurationByHeldItem(state, actor, effect.TurnsRemaining)
		state.sides[index].Conditions.AuroraVeil = &effect
		return state, random, []Event{AuroraVeilStartedEvent{
			Type: EventKindAuroraVeilStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: actor.Side, TurnsRemaining: effect.TurnsRemaining,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 极光幕使用者阵营不存在", ErrInvalidInitialState)
}

// applySpikesApplication 在单体目标变化技能成功执行后尝试在目标一方场地增加一层撒菱。
//
// 目标成员只用于确定其所属阵营；撒菱不附着在该成员身上，目标换下后仍留在同一方场地。达到三层上限后
// 不能刷新或产生第四层，必须写入明确失败事件。
func applySpikesApplication(state State, actor MemberRef, targetSide Side, skill SkillSnapshot, application SpikesApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "spikes chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != targetSide {
			continue
		}
		if state.sides[index].Conditions.SpikesLayers >= 3 {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
				Target: MemberRef{Side: targetSide}, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonSpikesAtMaximumLayers,
			}}, trace, nil
		}
		state.sides[index].Conditions.SpikesLayers++
		return state, random, []Event{SpikesLayerAddedEvent{
			Type: EventKindSpikesLayerAdded, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: targetSide, Layers: state.sides[index].Conditions.SpikesLayers,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 撒菱目标阵营不存在", ErrInvalidInitialState)
}

// applyStealthRockApplication 在单体目标变化技能成功执行后尝试在目标一方场地布置隐形岩。
//
// 隐形岩只有有无两种状态，不能像撒菱那样叠层或刷新；后续换入伤害始终从 RuleSnapshot 中冻结的岩石属性
// 克制关系读取，而不是从本函数保留任何可变资料引用。
func applyStealthRockApplication(state State, actor MemberRef, targetSide Side, skill SkillSnapshot, application StealthRockApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "stealth rock chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != targetSide {
			continue
		}
		if state.sides[index].Conditions.StealthRock {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
				Target: MemberRef{Side: targetSide}, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonStealthRockAlreadyActive,
			}}, trace, nil
		}
		state.sides[index].Conditions.StealthRock = true
		return state, random, []Event{StealthRockStartedEvent{
			Type: EventKindStealthRockStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: targetSide,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 隐形岩目标阵营不存在", ErrInvalidInitialState)
}

// applyToxicSpikesApplication 在单体目标变化技能成功执行后尝试在目标一方场地增加一层毒菱。
//
// 毒菱最多两层，第一层和第二层分别决定换入时的普通中毒和剧毒。它不与撒菱共用上限，以保证资料变更和
// 战斗事件能够准确区分伤害层数与异常层数。
func applyToxicSpikesApplication(state State, actor MemberRef, targetSide Side, skill SkillSnapshot, application ToxicSpikesApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "toxic spikes chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != targetSide {
			continue
		}
		if state.sides[index].Conditions.ToxicSpikesLayers >= 2 {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
				Target: MemberRef{Side: targetSide}, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonToxicSpikesAtMaximumLayers,
			}}, trace, nil
		}
		state.sides[index].Conditions.ToxicSpikesLayers++
		return state, random, []Event{ToxicSpikesLayerAddedEvent{
			Type: EventKindToxicSpikesLayerAdded, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: targetSide, Layers: state.sides[index].Conditions.ToxicSpikesLayers,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 毒菱目标阵营不存在", ErrInvalidInitialState)
}

// applyStickyWebApplication 在单体目标变化技能成功执行后尝试在目标一方场地布置黏黏网。
//
// 黏黏网仅有有无状态，重复布置不能刷新或叠加速度下降；实际能力阶级变化留到成员换入时依据接地规则结算。
func applyStickyWebApplication(state State, actor MemberRef, targetSide Side, skill SkillSnapshot, application StickyWebApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "sticky web chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != targetSide {
			continue
		}
		if state.sides[index].Conditions.StickyWeb {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
				Target: MemberRef{Side: targetSide}, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonStickyWebAlreadyActive,
			}}, trace, nil
		}
		state.sides[index].Conditions.StickyWeb = true
		return state, random, []Event{StickyWebStartedEvent{
			Type: EventKindStickyWebStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: targetSide,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 黏黏网目标阵营不存在", ErrInvalidInitialState)
}

// applyRapidSpinApplication 在快速旋转已成功处理目标后，清除使用者一方的全部入场危害。
//
// 反射壁、光墙、极光幕和顺风不是快速旋转的清除范围；即使当前不存在任何危害，也会写入事件以记录已经成功
// 执行该固定后效并提供重放所需的清除前精确快照。
func applyRapidSpinApplication(state State, actor MemberRef, skill SkillSnapshot) (State, []Event) {
	for index := range state.sides {
		if state.sides[index].Side != actor.Side {
			continue
		}
		conditions := &state.sides[index].Conditions
		event := RapidSpinHazardsClearedEvent{
			Type: EventKindRapidSpinHazardsCleared, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: actor.Side,
			ClearedSpikesLayers: conditions.SpikesLayers, ClearedStealthRock: conditions.StealthRock,
			ClearedToxicSpikesLayers: conditions.ToxicSpikesLayers, ClearedStickyWeb: conditions.StickyWeb,
		}
		conditions.SpikesLayers = 0
		conditions.StealthRock = false
		conditions.ToxicSpikesLayers = 0
		conditions.StickyWeb = false
		return state, []Event{event}
	}
	return state, nil
}

// applyDefogApplication 在清除浓雾成功处理目标后，清除目标一方的屏障、入场危害以及当前普通场地。
//
// 顺风是行动排序效果而非屏障或危害，故刻意不在这里移除。普通场地是全场单例状态，清除时以单独事件记录，
// 不能伪装为某一方的侧状态删除。
func applyDefogApplication(state State, actor MemberRef, targetSide Side, skill SkillSnapshot) (State, []Event) {
	events := make([]Event, 0, 2)
	for index := range state.sides {
		if state.sides[index].Side != targetSide {
			continue
		}
		conditions := &state.sides[index].Conditions
		events = append(events, DefogSideConditionsClearedEvent{
			Type: EventKindDefogSideConditionsCleared, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: targetSide,
			ClearedReflect: conditions.Reflect != nil, ClearedLightScreen: conditions.LightScreen != nil, ClearedAuroraVeil: conditions.AuroraVeil != nil,
			ClearedSpikesLayers: conditions.SpikesLayers, ClearedStealthRock: conditions.StealthRock,
			ClearedToxicSpikesLayers: conditions.ToxicSpikesLayers, ClearedStickyWeb: conditions.StickyWeb,
		})
		conditions.Reflect = nil
		conditions.LightScreen = nil
		conditions.AuroraVeil = nil
		conditions.SpikesLayers = 0
		conditions.StealthRock = false
		conditions.ToxicSpikesLayers = 0
		conditions.StickyWeb = false
		break
	}
	if terrain := state.environment.Terrain; terrain != nil {
		state.environment.Terrain = nil
		events = append(events, DefogTerrainClearedEvent{
			Type: EventKindDefogTerrainCleared, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Terrain: terrain.Kind,
		})
	}
	return state, events
}

// applyTailwindApplication 在自身范围技能成功执行后尝试为使用者一方建立顺风。
//
// 已存在顺风时不能刷新回合数，必须写入明确失败事件；概率小于 100 时只在此处消费一次随机数，确保重放轨迹
// 与其它技能后效遵循同一顺序。顺风状态写入阵营而非成员，因此成员换下后仍会影响同侧后续上场成员。
func applyTailwindApplication(state State, actor MemberRef, skill SkillSnapshot, application TailwindApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "tailwind chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	for index := range state.sides {
		if state.sides[index].Side != actor.Side {
			continue
		}
		if state.sides[index].Conditions.Tailwind != nil {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor,
				SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonTailwindAlreadyActive,
			}}, trace, nil
		}
		effect := application.Effect
		state.sides[index].Conditions.Tailwind = &effect
		return state, random, []Event{TailwindStartedEvent{
			Type: EventKindTailwindStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Side: actor.Side, TurnsRemaining: effect.TurnsRemaining,
		}}, trace, nil
	}
	return State{}, RandomSource{}, nil, nil, fmt.Errorf("%w: 顺风使用者阵营不存在", ErrInvalidInitialState)
}

// applyWeatherApplication 在自身范围技能成功执行后尝试建立普通全场天气。
func applyWeatherApplication(state State, actor MemberRef, skill SkillSnapshot, application WeatherApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "weather chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random, trace = nextRandom, append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	if state.environment.StrongWeather != nil {
		return state, random, []Event{SkillFailedEvent{Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actor, Target: actor, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonStrongWeatherActive,
		}}, trace, nil
	}
	if current := state.environment.Weather; current != nil && current.Kind == application.Effect.Kind {
		return state, random, []Event{SkillFailedEvent{Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonWeatherAlreadyActive}}, trace, nil
	}
	effect := extendWeatherDurationByHeldItem(state, actor, application.Effect)
	state.environment.Weather = &effect
	return state, random, []Event{WeatherStartedEvent{Type: EventKindWeatherStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, SkillPosition: skill.Position, SkillID: skill.SkillID, Weather: effect.Kind, TurnsRemaining: effect.TurnsRemaining}}, trace, nil
}

// applyTerrainApplication 在自身范围技能成功执行后尝试建立普通全场场地。
func applyTerrainApplication(state State, actor MemberRef, skill SkillSnapshot, application TerrainApplication, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "terrain chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	if current := state.environment.Terrain; current != nil && current.Kind == application.Effect.Kind {
		return state, random, []Event{SkillFailedEvent{Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, Target: actor, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonTerrainAlreadyActive}}, trace, nil
	}
	effect := extendTerrainDurationByHeldItem(state, actor, application.Effect)
	state.environment.Terrain = &effect
	return state, random, []Event{TerrainStartedEvent{Type: EventKindTerrainStarted, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actor, SkillPosition: skill.Position, SkillID: skill.SkillID, Terrain: effect.Kind, TurnsRemaining: effect.TurnsRemaining}}, trace, nil
}

// applyFieldSpeedOrderApplication 在技能已成功命中其自身范围后尝试修改全场速度顺序环境。
//
// 同 kind 的效果再次成功建立时会解除原效果，而不会刷新时长。若未来加入另一种互斥速度顺序效果，已有
// 效果保持不变，防止一次技能执行制造两个来源不明的重叠排序规则。
func applyFieldSpeedOrderApplication(
	state State,
	actor MemberRef,
	skill SkillSnapshot,
	application FieldSpeedOrderApplication,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "field speed order chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	current := state.environment.FieldSpeedOrder
	if current != nil && current.Kind == application.Effect.Kind {
		state.environment.FieldSpeedOrder = nil
		return state, random, []Event{FieldSpeedOrderEndedEvent{
			Type: EventKindFieldSpeedOrderEnded, SchemaVersion: 1, TurnNumber: state.turnNumber,
			FieldSpeedOrderKind: current.Kind, Actor: actor, SkillPosition: skill.Position, SkillID: skill.SkillID,
		}}, trace, nil
	}
	if current != nil {
		return state, random, nil, trace, nil
	}
	effect := application.Effect
	state.environment.FieldSpeedOrder = &effect
	return state, random, []Event{FieldSpeedOrderStartedEvent{
		Type: EventKindFieldSpeedOrderStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, SkillPosition: skill.Position, SkillID: skill.SkillID,
		FieldSpeedOrderKind: effect.Kind, TurnsRemaining: effect.TurnsRemaining,
	}}, trace, nil
}

// resolveSkillTargets 在技能真正执行时按冻结范围收集仍可战斗的场上目标。
//
// 单体目标保留“槽位而不是成员”的语义：目标在较早行动中换人后，会命中该槽位的新成员。范围目标不读取
// 客户端占位 target；随机对手只有候选大于一名时才消费随机数，避免单一候选污染离线重放轨迹。
func resolveSkillTargets(
	state State,
	actor SlotRef,
	selectedTarget SlotRef,
	skill SkillSnapshot,
	random RandomSource,
) ([]SlotRef, RandomSource, []RandomTraceEntry, error) {
	activeTarget := func(slot SlotRef) bool {
		member, exists := state.ActiveMember(slot)
		return exists && member.CurrentHP > 0
	}
	activeSlotsForSide := func(sideID Side, exclude SlotRef) []SlotRef {
		result := make([]SlotRef, 0, 2)
		for _, side := range state.sides {
			if side.Side != sideID {
				continue
			}
			for index := range side.ActiveMembers {
				slot := SlotRef{Side: sideID, Position: SlotPosition(index + 1)}
				if slot == exclude || !activeTarget(slot) {
					continue
				}
				result = append(result, slot)
			}
			return result
		}
		return result
	}
	opponents := func() []SlotRef {
		result := make([]SlotRef, 0, 2)
		for _, side := range state.sides {
			if side.Side == actor.Side {
				continue
			}
			result = append(result, activeSlotsForSide(side.Side, SlotRef{})...)
		}
		return result
	}

	switch skill.targetScope() {
	case SkillTargetScopeSelectedTarget:
		if activeTarget(selectedTarget) {
			return []SlotRef{selectedTarget}, random, nil, nil
		}
		return nil, random, nil, nil
	case SkillTargetScopeSelf:
		if activeTarget(actor) {
			return []SlotRef{actor}, random, nil, nil
		}
		return nil, random, nil, nil
	case SkillTargetScopeUserSideActive:
		if !activeTarget(actor) {
			return nil, random, nil, nil
		}
		return append([]SlotRef{actor}, activeSlotsForSide(actor.Side, actor)...), random, nil, nil
	case SkillTargetScopeAllAdjacentOpponents:
		return opponents(), random, nil, nil
	case SkillTargetScopeAllAdjacentParticipants:
		result := make([]SlotRef, 0, 3)
		for _, side := range state.sides {
			result = append(result, activeSlotsForSide(side.Side, actor)...)
		}
		return result, random, nil, nil
	case SkillTargetScopeRandomAdjacentOpponent:
		candidates := opponents()
		switch len(candidates) {
		case 0:
			return nil, random, nil, nil
		case 1:
			return candidates, random, nil, nil
		default:
			value, nextRandom, entry, err := random.Next(
				int32(len(candidates)), "random adjacent opponent target for "+skill.SkillID.String(),
			)
			if err != nil {
				return nil, RandomSource{}, nil, err
			}
			return []SlotRef{candidates[value]}, nextRandom, []RandomTraceEntry{entry}, nil
		}
	default:
		return nil, RandomSource{}, nil, ErrInvalidInitialState
	}
}

// canAffectMultipleTargets 报告指定范围是否在双打中会触发现代 0.75 范围伤害修正。
func (scope SkillTargetScope) canAffectMultipleTargets() bool {
	return scope == SkillTargetScopeAllAdjacentOpponents || scope == SkillTargetScopeAllAdjacentParticipants
}

// resolveSkillTarget 结算一次已经宣告的技能对一个实际目标的命中、伤害和目标向附加效果。
//
// 使用者向附加效果只会在单目标路径中一并执行；真正多目标技能在外层全部逐目标处理完成后才执行一次，
// 以保持“同一行动只触发一次自身强化或治疗”的规则边界。
func resolveSkillTarget(
	state State,
	actorSlot SlotRef,
	targetSlot SlotRef,
	skill SkillSnapshot,
	random RandomSource,
	multiTargetDamage bool,
	includeUserEffects bool,
	receivedDamage *receivedDamageHit,
) (State, RandomSource, []Event, []RandomTraceEntry, bool, error) {
	actor, actorExists := state.ActiveMember(actorSlot)
	target, targetExists := state.ActiveMember(targetSlot)
	if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 {
		return state, random, nil, nil, false, nil
	}
	actorRef := MemberRef{Side: actorSlot.Side, Position: actor.Position}
	trace := make([]RandomTraceEntry, 0, 4)
	events := make([]Event, 0, 4)
	if psychicTerrainBlocksPrioritySkill(state.environment.Terrain, state.rules, actorSlot, targetSlot, target, skill) {
		return state, random, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonPsychicTerrainTargetGrounded,
		}}, trace, false, nil
	}
	if strongWeatherSkillBlocked(state, actor, skill) {
		return state, random, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillFailureReasonStrongWeatherNegatesDamagingSkill,
		}}, trace, false, nil
	}
	// 先制技能侧免疫在精神场地 gate 之后、命中与伤害随机数之前判定。它必须遍历目标侧仍在场上且可战斗的
	// 成员，才能让双打中“伙伴保护目标”与“目标自身保护”具有完全相同的阻止和随机轨迹语义。
	if blocker, blocked := priorityMoveImmunityForSideBlocker(state, actorSlot, targetSlot, actor, skill); blocked {
		return state, random, []Event{SkillBlockedEvent{
			Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position}, Blocker: &blocker,
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillBlockReasonPriorityMoveImmunityForSide,
		}}, trace, false, nil
	}
	// 粉末免疫道具是命中前的持有者防护 gate：只读取冻结的粉末标签和当前道具投影，不会因攻击方拥有
	// 无视目标特性而失效。阻止后技能已经宣告并扣除 PP，但不会消费命中、异常概率或异常持续时间随机数。
	if target.ItemID != 0 && target.HeldItemPowderSkillImmunity && skill.PowderBased {
		return state, random, []Event{SkillBlockedEvent{
			Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillBlockReasonPowderSkillImmunity,
		}}, trace, false, nil
	}
	// 对手变化技能免疫是命中前的目标侧特性 gate：它阻止整条目标结算，但不阻止同侧辅助或自身目标。
	// 攻击方拥有无视目标特性规则时，必须在这里跳过防守方开关，不能等到附加效果阶段才局部绕过。
	if skill.DamageClass == DamageClassStatus && actorSlot.Side != targetSlot.Side &&
		target.OpponentStatusSkillImmunity && !ignoresTargetAbilityEffects(actor, skill) {
		return state, random, []Event{SkillBlockedEvent{
			Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillBlockReasonOpponentStatusSkillImmunity,
		}}, trace, false, nil
	}
	// 非克制伤害免疫同样是命中前的目标侧特性 gate。它必须使用技能当下的有效属性和强风修正后的冻结相性，
	// 不能在普通伤害公式已经消费要害或伤害浮动随机数后才撤销结果。
	if skill.DamageClass != DamageClassStatus && actorSlot.Side != targetSlot.Side &&
		target.NonSuperEffectiveDamageImmunity && !ignoresTargetAbilityEffects(actor, skill) &&
		!skillIsSuperEffectiveAgainstTarget(state, actor, target, skill) {
		return state, random, []Event{SkillBlockedEvent{
			Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillBlockReasonNonSuperEffectiveDamageImmunity,
		}}, trace, false, nil
	}
	// 目标在本次技能开始时已有替身时，对方技能的目标向附加效果必须始终被阻止，即使本次伤害
	// 恰好打破替身。这样不会因多段伤害或事件顺序使同一次命中的后续效果穿透替身。
	targetHadSubstitute := actorSlot.Side != targetSlot.Side && target.SubstituteHP != 0
	// 伤害技能的追加效果免疫只过滤落在目标身上的状态、能力阶级、畏缩和易变状态。它不回退已经命中的
	// 本体伤害，也不吞掉使用者自身的后续效果；无视目标特性的使用者可显式绕过该目标侧防守规则。
	targetSecondaryEffectsBlocked := actorSlot.Side != targetSlot.Side && skill.DamageClass != DamageClassStatus &&
		(target.ItemID != 0 && target.HeldItemDamagingSkillSecondaryEffectImmunity ||
			target.DamagingSkillSecondaryEffectImmunity && !ignoresTargetAbilityEffects(actor, skill))
	// 强行类特性只在技能实际携带异常、易变状态或能力变化时提供增伤，并同时移除这些目标向与使用者向
	// 附加效果。该判断不会移除技能本体伤害、吸取/反作用、强制换人或其它拥有独立生命周期的规则。
	secondaryStatusAndStatEffectsSuppressed := actor.SecondaryEffectsSuppressedDamageBoost != nil &&
		skillHasSecondaryStatusOrStatEffects(skill)
	// 保护只拦截对方对本成员的技能影响。它不把行动伪装成未命中，也不消耗命中、要害或伤害随机数，
	// 使重放轨迹准确表达“规则阻挡”而不是一次概率失败。
	if actorSlot != targetSlot && target.ProtectionTurnsRemaining != 0 &&
		!bypassesPersonalProtectionByContactAbility(actorSlot, targetSlot, actor, skill) {
		return state, random, []Event{SkillBlockedEvent{
			Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
			SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: SkillBlockReasonProtection,
		}}, trace, false, nil
	}
	if skill.damageMode() == SkillDamageModeOneHitKnockOut {
		if target.Level > actor.Level {
			return state, random, []Event{SkillFailedEvent{
				Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
				SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillFailureReasonOneHitKnockOutTargetLevelHigher,
			}}, trace, false, nil
		}
		if skill.OneHitKnockOutBlocksSameElementTarget && containsString(target.ElementIDs, effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))) {
			return state, random, []Event{SkillBlockedEvent{
				Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: MemberRef{Side: targetSlot.Side, Position: target.Position},
				SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillBlockReasonOneHitKnockOutSameElementTarget,
			}}, trace, false, nil
		}
	}
	effectiveAccuracy := skillAccuracy(effectiveSkillWeather(state, actor), actor, target, skill)
	targetRef := MemberRef{Side: targetSlot.Side, Position: target.Position}
	anyActualDamage := false
	if effectiveAccuracy > 0 && effectiveAccuracy < 100 && !actor.accuracyLockedOn(targetRef) {
		roll, nextRandom, entry, err := random.Next(100, "accuracy for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 > int32(effectiveAccuracy) {
			events = append(events, SkillMissedEvent{
				Type: EventKindSkillMissed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetSlot, SkillPosition: skill.Position,
				SkillID: skill.SkillID, Accuracy: effectiveAccuracy, Roll: uint8(roll + 1),
			})
			var heldItemEvents []Event
			state, heldItemEvents = applyHeldItemAccuracyMissStatStageBoost(state, actorRef, skill.SkillID)
			events = append(events, heldItemEvents...)
			return state, random, events, trace, false, nil
		}
	}

	if skill.damageMode() == SkillDamageModeAverageUserAndTargetCurrentHP {
		var averageEvents []Event
		var succeeded bool
		state, averageEvents, succeeded = resolveAverageCurrentHP(state, actorSlot, targetSlot, skill)
		events = append(events, averageEvents...)
		if !succeeded {
			return state, random, events, trace, false, nil
		}
	} else if skill.damageMode() != SkillDamageModeFormula {
		var directDamageEvents []Event
		var succeeded bool
		state, directDamageEvents, succeeded = resolveDirectSkillDamage(
			state, actorSlot, targetSlot, skill, receivedDamage,
		)
		events = append(events, directDamageEvents...)
		if !succeeded {
			return state, random, events, trace, false, nil
		}
	} else if skill.DamageClass != DamageClassStatus {
		hitCount, nextRandom, hitCountTrace, err := determineSkillHitCount(actor, skill, random)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, err
		}
		random = nextRandom
		trace = append(trace, hitCountTrace...)
		// 每一段都会重新读取双方权威状态。这样目标被前一段击倒、或使用者因反作用倒下后，
		// 后续段不会继续扣除已经不能战斗成员的生命，也不会额外消耗随机轨迹。
		for hitIndex := uint8(0); hitIndex < hitCount; hitIndex++ {
			actor, actorExists = state.ActiveMember(actorSlot)
			target, targetExists = state.ActiveMember(targetSlot)
			if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 {
				break
			}
			targetRef := MemberRef{Side: targetSlot.Side, Position: target.Position}
			criticalHit, nextRandom, criticalTrace, err := resolveCriticalHit(actor, skill, random)
			if err != nil {
				return State{}, RandomSource{}, nil, nil, false, err
			}
			random = nextRandom
			trace = append(trace, criticalTrace...)
			// 目标特性只否决已经完成的要害结果，绝不回退必定要害或随机要害的随机源推进；这样随机轨迹可与
			// 没有免疫时逐项比对，同时伤害公式、要害绕过屏障和公开事件均会看到普通命中。
			if target.CriticalHitImmunity && !ignoresTargetAbilityEffects(actor, skill) {
				criticalHit = false
			}
			damageRoll, nextRandom, entry, err := random.Next(16, "damage random for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, false, err
			}
			random = nextRandom
			trace = append(trace, entry)
			randomPercent := uint8(85 + damageRoll)
			allyModifiers := activeAllyDamageModifiers(state, actorSlot, targetSlot, skill)
			damage := calculateDamage(state.rules, effectiveStrongWeather(state), effectiveSkillWeather(state, actor), state.environment.Terrain, state.sideConditions(targetSlot.Side), state.format, actor, target, skill, randomPercent, criticalHit, multiTargetDamage, allyModifiers)
			var damageEvents []Event
			var actualDamage uint32
			state, damageEvents, actualDamage = applySkillDamage(
				state, actorRef, targetRef, skill, damage, criticalHit, randomPercent,
			)
			anyActualDamage = anyActualDamage || actualDamage > 0
			events = append(events, damageEvents...)
			bodyDamage := skillBodyDamageDealt(damageEvents, actorRef, targetRef, skill)
			var chargeEvents []Event
			state, chargeEvents = consumeReactiveAbilityCharge(state, actorRef, skill, bodyDamage)
			events = append(events, chargeEvents...)
			var airborneEvents []Event
			state, airborneEvents = clearHeldItemAirborneAfterBodyDamage(state, targetRef, bodyDamage)
			events = append(events, airborneEvents...)
			// 一次性抗性道具先完成本体伤害减免，再在伤害事件成为事实后消费。消费发生在接触转移之前，避免
			// 同一段接触把本应消失的抗性道具交给攻击者。
			var elementDamageReductionEvents []Event
			state, elementDamageReductionEvents = applyElementDamageReductionAfterBodyDamage(state, actorRef, targetRef, skill, bodyDamage)
			events = append(events, elementDamageReductionEvents...)
			var receivedDamageItemEvents []Event
			state, receivedDamageItemEvents = applyHeldItemReceivedDamageStatBoost(
				state, targetRef, skill.SkillID, effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor)),
				skillIsSuperEffective(state, actorRef, targetRef, skill), bodyDamage,
			)
			events = append(events, receivedDamageItemEvents...)
			// 一次性属性威力强化只有匹配技能已经造成真实本体伤害时才消费。必须先于道具转移，防止同一段接触
			// 伤害把本应消失的道具转交给攻击者；替身、未命中和免疫会因 bodyDamage 为零自然跳过。
			var consumableElementDamageBoostEvents []Event
			state, consumableElementDamageBoostEvents = applyConsumableElementDamageBoostAfterBodyDamage(state, actorRef, skill, bodyDamage)
			events = append(events, consumableElementDamageBoostEvents...)
			// 道具接触转移先于接触反伤：目标交出道具后，本段后续不能再从已移走的道具读取反伤规则。
			var transferEvents []Event
			state, transferEvents = applyContactItemTransferToAttacker(state, actorRef, targetRef, skill, bodyDamage)
			events = append(events, transferEvents...)
			// 接触反制紧随本段本体伤害。这样多段技能逐段触发，且攻击者被反制击倒后会在下一段开始前停止。
			var contactEvents []Event
			state, contactEvents = applyContactDamageToAttacker(state, actorRef, targetRef, skill, bodyDamage)
			events = append(events, contactEvents...)
			var healthEvents []Event
			state, healthEvents = applyDamageBasedSkillHealthEffect(state, actorRef, skill, actualDamage)
			events = append(events, healthEvents...)
			var heldItemHealingEvents []Event
			state, heldItemHealingEvents = applyHeldItemDamageDealtHealing(state, actorRef, skill, actualDamage)
			events = append(events, heldItemHealingEvents...)
			var heldItemRecoilEvents []Event
			state, heldItemRecoilEvents = applyHeldItemDamageBoostRecoil(state, actorRef, skill, actualDamage)
			events = append(events, heldItemRecoilEvents...)
			var rechargeEvents []Event
			state, rechargeEvents = applyRechargeAfterBodyDamage(
				state, actorRef, skill, bodyDamage,
			)
			events = append(events, rechargeEvents...)
		}
	}
	if !anyActualDamage {
		anyActualDamage = hasActualSkillDamage(events, actorRef, targetRef, skill.SkillID)
	}
	target, targetExists = state.ActiveMember(targetSlot)
	if !targetExists {
		return state, random, events, trace, true, nil
	}
	targetRef = MemberRef{Side: targetSlot.Side, Position: target.Position}
	var targetHealingEvents []Event
	state, targetHealingEvents = applyTargetSkillHealing(state, actorRef, targetRef, skill)
	events = append(events, targetHealingEvents...)
	var err error
	var statusEvents []Event
	var statusTrace []RandomTraceEntry
	state, random, statusEvents, statusTrace, err = resolveMajorStatusApplications(
		state, actorRef, targetRef, skill, random,
		!targetHadSubstitute && !targetSecondaryEffectsBlocked && !secondaryStatusAndStatEffectsSuppressed,
		includeUserEffects && !secondaryStatusAndStatEffectsSuppressed,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, statusEvents...)
	trace = append(trace, statusTrace...)
	var volatileEvents []Event
	var volatileTrace []RandomTraceEntry
	state, random, volatileEvents, volatileTrace, err = resolveVolatileStatusApplications(
		state, actorRef, targetRef, skill, random,
		!targetHadSubstitute && !targetSecondaryEffectsBlocked && !secondaryStatusAndStatEffectsSuppressed,
		includeUserEffects && !secondaryStatusAndStatEffectsSuppressed,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, volatileEvents...)
	trace = append(trace, volatileTrace...)
	// 追加效果顺序固定为主要异常、易变状态、道具追加畏缩、能力阶级变化。王者之证类规则
	// 因而只能在前两类效果完成后消费自己的独立概率随机；替身、追加效果免疫、强行抑制和零实际伤害
	// 都在进入该接点前短路，不能留下伪随机轨迹。
	var flinchEvents []Event
	var flinchTrace []RandomTraceEntry
	if !targetHadSubstitute && !targetSecondaryEffectsBlocked && !secondaryStatusAndStatEffectsSuppressed {
		flinchSkill := skill
		if currentActor, exists := state.member(actorRef.Side, actorRef.Position); exists {
			flinchSkill.FlinchChancePercent = effectiveFlinchChance(currentActor, skill)
			if skill.FlinchChancePercent == 0 && !anyActualDamage {
				flinchSkill.FlinchChancePercent = 0
			}
		}
		state, random, flinchEvents, flinchTrace, err = resolveFlinchApplication(state, actorRef, targetRef, flinchSkill, random)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, err
		}
		events = append(events, flinchEvents...)
		trace = append(trace, flinchTrace...)
	}
	var stageEvents []Event
	var stageTrace []RandomTraceEntry
	state, random, stageEvents, stageTrace, err = resolveStatStageEffects(
		state, actorRef, targetRef, skill, random,
		!targetHadSubstitute && !targetSecondaryEffectsBlocked && !secondaryStatusAndStatEffectsSuppressed,
		includeUserEffects && !secondaryStatusAndStatEffectsSuppressed,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, stageEvents...)
	trace = append(trace, stageTrace...)
	var accuracyLockEvents []Event
	state, accuracyLockEvents = applyAccuracyLock(
		state, actorRef, targetRef, skill, targetHadSubstitute,
	)
	events = append(events, accuracyLockEvents...)
	if skill.LeechSeedApplication != nil {
		var leechSeedEvents []Event
		var leechSeedTrace []RandomTraceEntry
		state, random, leechSeedEvents, leechSeedTrace, err = applyLeechSeedApplication(
			state, actorSlot, actorRef, targetRef, skill, *skill.LeechSeedApplication, targetHadSubstitute, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, err
		}
		events = append(events, leechSeedEvents...)
		trace = append(trace, leechSeedTrace...)
	}
	var negativeStageItemEvents []Event
	var negativeStageItemTrace []RandomTraceEntry
	state, random, negativeStageItemEvents, negativeStageItemTrace, err = resolveNegativeStatStageForcedSwitchItems(
		state, stageEvents, random,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, negativeStageItemEvents...)
	trace = append(trace, negativeStageItemTrace...)
	if skill.ForceTargetSwitch {
		var forcedSwitchEvents []Event
		var forcedSwitchTrace []RandomTraceEntry
		state, random, forcedSwitchEvents, forcedSwitchTrace, err = resolveForcedTargetSwitch(
			state, actorRef, targetSlot, targetRef, skill, targetHadSubstitute, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, false, err
		}
		events = append(events, forcedSwitchEvents...)
		trace = append(trace, forcedSwitchTrace...)
	}
	var thresholdSwitchEvents []Event
	var thresholdSwitchTrace []RandomTraceEntry
	state, random, thresholdSwitchEvents, thresholdSwitchTrace, err = resolveDamageCrossedHalfHPForcedSwitch(
		state, targetSlot, targetRef, skillBodyDamageDealt(events, actorRef, targetRef, skill), random,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, thresholdSwitchEvents...)
	trace = append(trace, thresholdSwitchTrace...)
	var damagedItemEvents []Event
	var damagedItemTrace []RandomTraceEntry
	state, random, damagedItemEvents, damagedItemTrace, err = resolveDamagedForcedSwitchItem(
		state, actorSlot, targetSlot, actorRef, targetRef, skillDamageDealt(events, actorRef, targetRef, skill), random,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, false, err
	}
	events = append(events, damagedItemEvents...)
	trace = append(trace, damagedItemTrace...)
	return state, random, events, trace, true, nil
}

// applyLeechSeedApplication 在已经通过命中判定的单体技能路径中尝试把寄生种子写入目标本体。
//
// 寄生种子使用自己的命中后概率，不与主要异常或易变状态共用随机项。替身、草属性和已有种子均是技能
// 已经命中但不能产生此效果的显式失败；这样重放与事件消费者不会把规则免疫误判为普通未命中。
func applyLeechSeedApplication(
	state State,
	actorSlot SlotRef,
	actorRef MemberRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	application LeechSeedApplication,
	targetHadSubstitute bool,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, 1)
	target, exists := state.member(targetRef.Side, targetRef.Position)
	if !exists || target.CurrentHP == 0 {
		return state, random, nil, trace, nil
	}
	if application.ChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "leech seed chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 > int32(application.ChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	failure := func(reason SkillFailureReason) (State, RandomSource, []Event, []RandomTraceEntry, error) {
		return state, random, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: reason,
		}}, trace, nil
	}
	if targetHadSubstitute {
		return failure(SkillFailureReasonLeechSeedTargetBehindSubstitute)
	}
	if target.LeechSeedSourceSlot != nil {
		return failure(SkillFailureReasonLeechSeedTargetAlreadySeeded)
	}
	if leechSeedBlockedByElement(state.rules, target) {
		return failure(SkillFailureReasonLeechSeedGrassTarget)
	}
	sourceSlot := actorSlot
	target.LeechSeedSourceSlot = &sourceSlot
	state.replaceMember(targetRef.Side, target)
	return state, random, []Event{LeechSeedPlantedEvent{
		Type: EventKindLeechSeedPlanted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, Target: targetRef, SourceSlot: sourceSlot, SkillPosition: skill.Position, SkillID: skill.SkillID,
	}}, trace, nil
}

// resolveFlinchApplication 按技能冻结概率为仍可战斗的目标写入当前回合畏缩。该状态不占用主要异常，
// 也不消费 PP；目标轮到行动时才会生成 SkillPreventedEvent，因而可以正确表示“后手被畏缩、先手已行动
// 则自然失效”的回合时序。
func resolveFlinchApplication(
	state State,
	actorRef MemberRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	if skill.FlinchChancePercent == 0 {
		return state, random, nil, nil, nil
	}
	target, exists := state.member(targetRef.Side, targetRef.Position)
	if !exists || target.CurrentHP == 0 {
		return state, random, nil, nil, nil
	}
	trace := make([]RandomTraceEntry, 0, 1)
	if skill.FlinchChancePercent < 100 {
		roll, nextRandom, entry, err := random.Next(100, "flinch chance for "+skill.SkillID.String())
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		if roll+1 > int32(skill.FlinchChancePercent) {
			return state, random, nil, trace, nil
		}
	}
	if target.FlinchedTurn == state.turnNumber {
		return state, random, nil, trace, nil
	}
	target.FlinchedTurn = state.turnNumber
	state.replaceMember(targetRef.Side, target)
	return state, random, []Event{FlinchAppliedEvent{
		Type: EventKindFlinchApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, Target: targetRef, SkillID: skill.SkillID, ChancePercent: skill.FlinchChancePercent,
	}}, trace, nil
}

// resolveDirectSkillDamage 结算不进入普通伤害公式的单段直接伤害，并保留与公式伤害一致的结构化扣血、倒下
// 和既有 drain/recoil 后效边界。直接伤害不会消耗要害或伤害浮动随机数，也不会读取攻防能力；伤害记忆
// 是唯一显式声明读取属性相性的一类直接伤害，其余模式保持纯数值规则。
func resolveDirectSkillDamage(
	state State,
	actorSlot SlotRef,
	targetSlot SlotRef,
	skill SkillSnapshot,
	receivedDamage *receivedDamageHit,
) (State, []Event, bool) {
	actor, actorExists := state.ActiveMember(actorSlot)
	target, targetExists := state.ActiveMember(targetSlot)
	if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 {
		return state, nil, false
	}
	actorRef := MemberRef{Side: actorSlot.Side, Position: actor.Position}
	targetRef := MemberRef{Side: targetSlot.Side, Position: target.Position}
	damage, selfSacrifice, failureReason, succeeded := directDamageAmount(actor, target, skill, receivedDamage)
	if !succeeded {
		return state, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID, Reason: failureReason,
		}}, false
	}
	if skill.damageMode() == SkillDamageModeReceivedDamage {
		// 伤害记忆可选择忽略“非免疫”的相性倍率，但完全免疫仍是目标当前状态提供的确定性阻止。
		// 该判断发生在任何扣血、吸取或反作用之前，因此免疫时不会产生零伤害事件或错误触发后效。
		var immune bool
		damage, immune = receivedDamageAfterElementEffectiveness(state.rules, effectiveSkillWeather(state, actor), actor, target, skill, damage)
		if immune {
			return state, []Event{SkillBlockedEvent{
				Type: EventKindSkillBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
				Reason: SkillBlockReasonElementImmunity,
			}}, false
		}
	}
	state, events, actualDamage := applySkillDamage(state, actorRef, targetRef, skill, damage, false, 0)
	bodyDamage := skillBodyDamageDealt(events, actorRef, targetRef, skill)
	var chargeEvents []Event
	state, chargeEvents = consumeReactiveAbilityCharge(state, actorRef, skill, bodyDamage)
	events = append(events, chargeEvents...)
	// 直接伤害技能同样可能声明接触，因而必须先完成目标道具的接触转移，再读取剩余目标侧反制来源。
	var transferEvents []Event
	state, transferEvents = applyContactItemTransferToAttacker(state, actorRef, targetRef, skill, bodyDamage)
	events = append(events, transferEvents...)
	// 直接伤害技能同样可能声明接触；反制仍只以真正写入目标本体的 DamageAppliedEvent 为前提。
	var contactEvents []Event
	state, contactEvents = applyContactDamageToAttacker(state, actorRef, targetRef, skill, bodyDamage)
	events = append(events, contactEvents...)
	if selfSacrifice {
		// 自我牺牲取行动开始时仍然有效的使用者当前生命；目标扣血和倒下事件必须先写入，
		// 使事件流清晰表达“命中后支付全部生命”的因果顺序。
		selfDamage := actor.CurrentHP
		actor.CurrentHP = 0
		state.replaceMember(actorSlot.Side, actor)
		events = append(events,
			SkillSelfSacrificeDamageAppliedEvent{
				Type: EventKindSkillSelfSacrificeDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, SkillID: skill.SkillID, Amount: selfDamage, CurrentHP: 0,
			},
			ParticipantFaintedEvent{
				Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: actorRef, Cause: FaintCauseSkillSelfSacrifice, SkillID: skill.SkillID,
			},
		)
		return state, events, true
	}
	var healthEvents []Event
	state, healthEvents = applyDamageBasedSkillHealthEffect(state, actorRef, skill, actualDamage)
	events = append(events, healthEvents...)
	var rechargeEvents []Event
	state, rechargeEvents = applyRechargeAfterBodyDamage(
		state, actorRef, skill, bodyDamage,
	)
	events = append(events, rechargeEvents...)
	return state, events, true
}

// receivedDamageAfterElementEffectiveness 把伤害记忆已经按资料倍率放大的伤害与目标当前属性相性合并。
//
// 返回的 immune 为 true 时表示任一防守属性给出了 0 倍完全免疫。此时调用方必须完整阻止技能；若资料开启
// IgnoreNonImmuneElementEffectiveness，则所有非零倍率都不会参与数值计算。双属性至多两个 16 位倍率，结合
// uint32 的直接伤害上限仍可安全放入 uint64，因而可以在最终统一向下取整而不引入逐属性截断误差。
func receivedDamageAfterElementEffectiveness(
	rules RuleSnapshot,
	weather *WeatherEffect,
	actor MemberSnapshot,
	target MemberSnapshot,
	skill SkillSnapshot,
	damage uint32,
) (adjusted uint32, immune bool) {
	numerator := uint64(damage)
	denominator := uint64(1)
	skillElementID := effectiveSkillElementForMember(actor, skill, weather)
	for _, defenseElementID := range target.ElementIDs {
		effectivenessNumerator, effectivenessDenominator := rules.effectiveness(skillElementID, defenseElementID)
		if effectivenessNumerator == 0 {
			if target.ItemID != 0 && target.HeldItemTypeImmunitySuppression {
				continue
			}
			return 0, true
		}
		if skill.ReceivedDamageIgnoreNonImmuneElementEffectiveness {
			continue
		}
		numerator *= uint64(effectivenessNumerator)
		denominator *= uint64(effectivenessDenominator)
	}
	if skill.ReceivedDamageIgnoreNonImmuneElementEffectiveness {
		return damage, false
	}
	// latestReceivedDamage 已保证反打原始伤害至少为 1。非免疫属性的分数乘积即使小于 1，仍遵循
	// 伤害记忆的“最低 1 点”约束，不能把一次成功反打静默变成零伤害。
	return max(uint32(numerator/denominator), 1), false
}

// resolveAverageCurrentHP 结算把使用者与目标本体当前生命重设为二者平均值的非伤害规则。
//
// 替身存在时该规则不能穿透到目标本体，因此技能已宣告后会明确失败；成功时两个成员分别按各自最大生命
// 夹取平均值，且不产生普通伤害、回复、吸取、反作用或倒下事件。
func resolveAverageCurrentHP(
	state State,
	actorSlot SlotRef,
	targetSlot SlotRef,
	skill SkillSnapshot,
) (State, []Event, bool) {
	actor, actorExists := state.ActiveMember(actorSlot)
	target, targetExists := state.ActiveMember(targetSlot)
	if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 {
		return state, nil, false
	}
	actorRef := MemberRef{Side: actorSlot.Side, Position: actor.Position}
	targetRef := MemberRef{Side: targetSlot.Side, Position: target.Position}
	if actorSlot.Side != targetSlot.Side && target.SubstituteHP != 0 {
		return state, []Event{SkillFailedEvent{
			Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
			Reason: SkillFailureReasonTargetBehindSubstitute,
		}}, false
	}
	average := uint32((uint64(actor.CurrentHP) + uint64(target.CurrentHP)) / 2)
	actor.CurrentHP = min(average, actor.MaxHP)
	target.CurrentHP = min(average, target.MaxHP)
	state.replaceMember(actorSlot.Side, actor)
	state.replaceMember(targetSlot.Side, target)
	return state, []Event{HPAveragedBySkillEvent{
		Type: EventKindHPAveragedBySkill, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
		ActorCurrentHP: actor.CurrentHP, TargetCurrentHP: target.CurrentHP,
	}}, true
}

// applySkillDamage 把已计算出的单段技能伤害写入替身或目标本体，并返回实际扣除的伤害量。
//
// 对方拥有替身时，替身优先承受全部伤害且绝不把溢出伤害传递给本体；替身破裂后同一技能的后续多段
// 才会直接命中本体。返回值统一作为吸取和反作用的基数，因此两条路径不会错误读取未夹取的理论伤害。
func applySkillDamage(
	state State,
	actorRef MemberRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	damage uint32,
	criticalHit bool,
	randomPercent uint8,
) (State, []Event, uint32) {
	target, exists := state.member(targetRef.Side, targetRef.Position)
	if !exists || target.CurrentHP == 0 {
		return state, nil, 0
	}
	if actorRef.Side != targetRef.Side && target.SubstituteHP != 0 {
		actualDamage := min(damage, target.SubstituteHP)
		target.SubstituteHP -= actualDamage
		state.replaceMember(targetRef.Side, target)
		events := []Event{SubstituteDamageAppliedEvent{
			Type: EventKindSubstituteDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
			Amount: actualDamage, SubstituteHPRemaining: target.SubstituteHP,
		}}
		if target.SubstituteHP == 0 {
			events = append(events, SubstituteBrokenEvent{
				Type: EventKindSubstituteBroken, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetRef, SkillID: skill.SkillID,
			})
		}
		return state, events, actualDamage
	}
	// 满生命保命规则只处理对手技能将本体生命降为 0 的本段伤害。替身已在上方提前返回，天气、异常、
	// 陷阱和反作用不经过本函数；无视目标特性只跳过特性来源，不能越过独立的道具来源。
	survivedFatalDamage := false
	var survivalSourceAbilityID Identifier
	var survivalSourceItemID Identifier
	actor, actorExists := state.member(actorRef.Side, actorRef.Position)
	if actorRef.Side != targetRef.Side && actorExists && target.CurrentHP == target.MaxHP && damage >= target.CurrentHP {
		if !ignoresTargetAbilityEffects(actor, skill) && target.SurviveFatalDamageAtFullHP {
			survivedFatalDamage = true
			survivalSourceAbilityID = target.AbilityID
		} else if target.ItemID != 0 && target.HeldItemSurviveFatalDamageAtFullHP {
			// 道具来源保命在特性来源不生效后才尝试，并在伤害写入前消费全部道具运行态，确保后续多段伤害
			// 不能重复触发。clearHeldItemRuntimeState 同时处理道具属性身份的自然属性恢复。
			survivedFatalDamage = true
			survivalSourceItemID = target.ItemID
			target = clearHeldItemRuntimeState(target)
		}
	}
	actualDamage := min(damage, target.CurrentHP)
	if survivedFatalDamage {
		actualDamage = target.CurrentHP - 1
	}
	target.CurrentHP -= actualDamage
	state.replaceMember(targetRef.Side, target)
	events := []Event{DamageAppliedEvent{
		Type: EventKindDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID, Amount: actualDamage,
		CurrentHP: target.CurrentHP, CriticalHit: criticalHit, RandomPercent: randomPercent,
	}}
	if survivedFatalDamage {
		events = append(events, FatalDamageSurvivedEvent{
			Type: EventKindFatalDamageSurvived, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
			SourceAbilityID: survivalSourceAbilityID, SourceItemID: survivalSourceItemID,
			IncomingDamage: damage, PreventedDamage: damage - actualDamage,
			CurrentHP: target.CurrentHP,
		})
	}
	if target.CurrentHP == 0 {
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: targetRef, Cause: FaintCauseSkillDamage, SkillID: skill.SkillID,
		})
	}
	return state, events, actualDamage
}

// directDamageAmount 从行动瞬间的双方快照计算直接伤害规则。返回的失败原因只在规则明确要求技能失败时
// 有值；调用者据此写入 SkillFailedEvent，而不是回退到普通公式伤害或伪造一次未命中。
func directDamageAmount(
	actor MemberSnapshot,
	target MemberSnapshot,
	skill SkillSnapshot,
	receivedDamage *receivedDamageHit,
) (uint32, bool, SkillFailureReason, bool) {
	switch skill.damageMode() {
	case SkillDamageModeFixedAmount:
		return skill.DamageAmount, false, "", true
	case SkillDamageModeUserLevel:
		return uint32(actor.Level), false, "", true
	case SkillDamageModeTargetCurrentHPFraction:
		amount := uint32(uint64(target.CurrentHP) * uint64(skill.DamageNumerator) / uint64(skill.DamageDenominator))
		return max(amount, skill.MinimumDamage), false, "", true
	case SkillDamageModeTargetCurrentHPMinusUserCurrentHP:
		if target.CurrentHP <= actor.CurrentHP {
			return 0, false, SkillFailureReasonTargetHPNotGreaterThanUserHP, false
		}
		return target.CurrentHP - actor.CurrentHP, false, "", true
	case SkillDamageModeUserCurrentHPAndUserFaints:
		return actor.CurrentHP, true, "", true
	case SkillDamageModeOneHitKnockOut:
		return target.CurrentHP, false, "", true
	case SkillDamageModeReceivedDamage:
		if receivedDamage == nil {
			return 0, false, SkillFailureReasonReceivedDamageMemoryUnavailable, false
		}
		return receivedDamage.amount, false, "", true
	default:
		return 0, false, "", false
	}
}

// receivedDamageHit 是从当前回合事件流还原出的单段、可返还受伤事实。
//
// 该结构仅在一次 ResolveTurn 调用栈中传递：它故意不进入 State、回放快照或数据库，避免上回合、换人前或
// 已倒下来源的伤害被错误复用。
type receivedDamageHit struct {
	// sourceSlot 是伤害来源当前仍占据的场上槽位，反打技能必须重定向到该槽位。
	sourceSlot SlotRef
	// amount 是按当前技能冻结倍率向下取整、且至少为 1 的最终返还伤害。
	amount uint32
}

// latestReceivedDamage 倒序检索本回合中使用者最后一段合格的直接 HP 伤害。
//
// 只有 DamageAppliedEvent 才能成为记忆来源，因此替身、天气、异常和零伤害不会误触发。来源必须是仍在场且未
// 倒下的对手，并且能从其当前技能槽确认物理或特殊伤害类别；这使目标重定向与后续伤害计算都基于同一份权威事件。
func latestReceivedDamage(
	state State,
	events []Event,
	actorSlot SlotRef,
	skill SkillSnapshot,
) (receivedDamageHit, bool) {
	actor, actorExists := state.ActiveMember(actorSlot)
	if !actorExists {
		return receivedDamageHit{}, false
	}
	actorRef := MemberRef{Side: actorSlot.Side, Position: actor.Position}
	for index := len(events) - 1; index >= 0; index-- {
		event, isDamage := events[index].(DamageAppliedEvent)
		if !isDamage || event.TurnNumber != state.turnNumber || event.Target != actorRef || event.Amount == 0 ||
			event.Actor.Side == actorSlot.Side {
			continue
		}
		sourceSlot, source, sourceActive := activeSlotForMember(state, event.Actor)
		if !sourceActive || source.CurrentHP == 0 {
			continue
		}
		var sourceDamageClass DamageClass
		foundSourceSkill := false
		for _, sourceSkill := range source.Skills {
			if sourceSkill.SkillID == event.SkillID {
				sourceDamageClass = sourceSkill.DamageClass
				foundSourceSkill = true
				break
			}
		}
		if !foundSourceSkill ||
			(sourceDamageClass != DamageClassPhysical || !skill.ReceivedDamageAcceptsPhysical) &&
				(sourceDamageClass != DamageClassSpecial || !skill.ReceivedDamageAcceptsSpecial) {
			continue
		}
		amount := uint64(event.Amount) * uint64(skill.ReceivedDamageNumerator) / uint64(skill.ReceivedDamageDenominator)
		if amount == 0 {
			amount = 1
		}
		if amount > uint64(^uint32(0)) {
			amount = uint64(^uint32(0))
		}
		return receivedDamageHit{sourceSlot: sourceSlot, amount: uint32(amount)}, true
	}
	return receivedDamageHit{}, false
}

// activeSlotForMember 返回指定稳定成员引用当前占据的场上槽位及其实时快照。
//
// 伤害记忆不能攻击已换下或已倒下的来源；因此此函数不通过成员稳定位置猜测目标，而是明确验证该成员仍在当前
// ActiveMembers 列表中。成员位置与场上槽位是不同的概念，双打换人后尤其不能混用。
func activeSlotForMember(state State, memberRef MemberRef) (SlotRef, MemberSnapshot, bool) {
	for _, side := range state.sides {
		if side.Side != memberRef.Side {
			continue
		}
		for index, activeMemberPosition := range side.ActiveMembers {
			if activeMemberPosition != memberRef.Position {
				continue
			}
			member, exists := state.member(memberRef.Side, memberRef.Position)
			if !exists {
				return SlotRef{}, MemberSnapshot{}, false
			}
			return SlotRef{Side: memberRef.Side, Position: SlotPosition(index + 1)}, member, true
		}
	}
	return SlotRef{}, MemberSnapshot{}, false
}

// determineSkillHitCount 决定一项伤害技能在本次使用中应结算的连续命中段数。
//
// 单段技能按 1 段处理。标准 2 至 5 段使用现代规则的 35%、35%、15%、15%
// 分布；其它合法范围使用均匀分布。拥有 MultiHitMaximum 的使用者在范围可变时直接取最大段数，因而不读取
// 段数随机数。随机轨迹只记录真正需要随机选择段数的情形，因而同一局可精确重放。
func determineSkillHitCount(actor MemberSnapshot, skill SkillSnapshot, random RandomSource) (uint8, RandomSource, []RandomTraceEntry, error) {
	minimum, maximum := skill.hitRange()
	if minimum == maximum {
		return minimum, random, nil, nil
	}
	if actor.MultiHitMaximum {
		return maximum, random, nil, nil
	}
	if itemMinimum, itemMaximum, found := heldItemMultiHitRange(actor, minimum, maximum); found {
		minimum, maximum = itemMinimum, itemMaximum
	}
	if minimum == 2 && maximum == 5 {
		roll, nextRandom, entry, err := random.Next(100, "multi-hit count for "+skill.SkillID.String())
		if err != nil {
			return 0, RandomSource{}, nil, err
		}
		hitCount := uint8(5)
		switch {
		case roll < 35:
			hitCount = 2
		case roll < 70:
			hitCount = 3
		case roll < 85:
			hitCount = 4
		}
		return hitCount, nextRandom, []RandomTraceEntry{entry}, nil
	}
	roll, nextRandom, entry, err := random.Next(int32(maximum-minimum+1), "multi-hit count for "+skill.SkillID.String())
	if err != nil {
		return 0, RandomSource{}, nil, err
	}
	return minimum + uint8(roll), nextRandom, []RandomTraceEntry{entry}, nil
}

// heldItemMultiHitRange 返回当前持有道具为指定原始区间的多段技能提供的实际随机段数范围。
//
// 只有成员仍持有道具、四个冻结参数完整且技能原始区间完全匹配时才应用覆盖；固定段数和不匹配区间继续使用
// 技能自身规则。调用方在该结果上继续读取随机源，因此 4–5 段仍是随机结果而不是固定值。
func heldItemMultiHitRange(actor MemberSnapshot, minimum, maximum uint8) (uint8, uint8, bool) {
	if actor.ItemID == 0 || actor.HeldItemMultiHitCountMinimum == 0 || actor.HeldItemMultiHitCountMaximum < actor.HeldItemMultiHitCountMinimum ||
		actor.HeldItemMultiHitRequiredMinimum == 0 || actor.HeldItemMultiHitRequiredMaximum < actor.HeldItemMultiHitRequiredMinimum ||
		minimum != actor.HeldItemMultiHitRequiredMinimum || maximum != actor.HeldItemMultiHitRequiredMaximum {
		return 0, 0, false
	}
	return actor.HeldItemMultiHitCountMinimum, actor.HeldItemMultiHitCountMaximum, true
}

// hitRange 返回技能冻结后的显式连续命中范围。
func (skill SkillSnapshot) hitRange() (uint8, uint8) {
	return skill.MinHits, skill.MaxHits
}

// resolveCriticalHit 按技能和使用者特性共同提供的冻结要害等级执行一次命中段的要害判定。必定要害不消耗
// 随机数，等级 0、1、2 分别使用 1/24、1/8、1/2 的公开概率表；每段独立调用以保持多段命中与离线回放一致。
func resolveCriticalHit(actor MemberSnapshot, skill SkillSnapshot, random RandomSource) (bool, RandomSource, []RandomTraceEntry, error) {
	itemStage := uint16(0)
	if actor.ItemID != 0 && actor.HeldItemCriticalHitStageBoost {
		itemStage = 1
	}
	stage := min(uint16(skill.CriticalHitStage)+uint16(actor.CriticalHitStageBoost)+itemStage, uint16(6))
	denominator := int32(1)
	switch stage {
	case 0:
		denominator = 24
	case 1:
		denominator = 8
	case 2:
		denominator = 2
	}
	if denominator == 1 {
		return true, random, nil, nil
	}
	roll, nextRandom, entry, err := random.Next(denominator, "critical hit for "+skill.SkillID.String())
	if err != nil {
		return false, RandomSource{}, nil, err
	}
	return roll == 0, nextRandom, []RandomTraceEntry{entry}, nil
}

// applyDamageBasedSkillHealthEffect 按本段目标实际损失生命值处理吸取或反作用后效。
//
// 正向吸取严格向下取整，负向反作用采用四舍五入且在非零比例时至少扣除 1 点；两种写入都以最新
// 使用者状态为准并夹取到合法生命区间。这样多目标技能会按每个实际目标独立结算自身后效，且任何目标
// 因未命中或伤害为 0 没有扣血时不会产生伪造的回复或反作用事件。
func applyDamageBasedSkillHealthEffect(
	state State,
	actorRef MemberRef,
	skill SkillSnapshot,
	damageAmount uint32,
) (State, []Event) {
	if damageAmount == 0 || skill.DrainPercent == 0 {
		return state, nil
	}
	actor, exists := state.member(actorRef.Side, actorRef.Position)
	if !exists || actor.CurrentHP == 0 {
		return state, nil
	}
	if skill.DrainPercent > 0 {
		amount := uint32(uint64(damageAmount) * uint64(skill.DrainPercent) / 100)
		if actor.ItemID != 0 && actor.HeldItemDrainHealingBoost {
			amount = uint32(uint64(amount) * 13 / 10)
		}
		amount = min(amount, actor.MaxHP-actor.CurrentHP)
		if amount == 0 {
			return state, nil
		}
		actor.CurrentHP += amount
		state.replaceMember(actorRef.Side, actor)
		return state, []Event{SkillHealingAppliedEvent{
			Type: EventKindSkillHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, SkillID: skill.SkillID, Source: SkillHealingSourceDrain, Amount: amount, CurrentHP: actor.CurrentHP,
		}}
	}
	// 技能反作用免疫只作用于“按实际伤害回算”的负 DrainPercent。固定生命代价、道具反伤、天气和其它
	// 间接伤害均由各自阶段处理，不能因名称相近而被一并跳过。
	if actor.SkillRecoilDamageImmunity {
		return state, nil
	}
	amount := roundedHalfUpPercent(damageAmount, uint8(-skill.DrainPercent))
	amount = min(amount, actor.CurrentHP)
	if amount == 0 {
		return state, nil
	}
	actor.CurrentHP -= amount
	state.replaceMember(actorRef.Side, actor)
	events := []Event{SkillRecoilDamageAppliedEvent{
		Type: EventKindSkillRecoilDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, Amount: amount, SourceDamageAmount: damageAmount, CurrentHP: actor.CurrentHP,
	}}
	if actor.CurrentHP == 0 {
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: actorRef, Cause: FaintCauseSkillRecoil, SkillID: skill.SkillID,
		})
	}
	return state, events
}

// applyFixedSkillHealthEffect 按使用者最大生命值处理技能成功后的固定比例回复或代价。
//
// 正数回复向下取整，负数代价使用与反作用一致的四舍五入规则。该后效在一项范围技能完成全部
// 真实目标结算后只执行一次，避免同一技能因为命中多个目标而重复回复或重复支付生命代价。
func applyFixedSkillHealthEffect(state State, actorRef MemberRef, skill SkillSnapshot) (State, []Event) {
	if skill.HealingPercent == 0 {
		return state, nil
	}
	actor, exists := state.member(actorRef.Side, actorRef.Position)
	if !exists || actor.CurrentHP == 0 {
		return state, nil
	}
	if skill.HealingPercent > 0 {
		amount := uint32(uint64(actor.MaxHP) * uint64(skill.HealingPercent) / 100)
		amount = min(amount, actor.MaxHP-actor.CurrentHP)
		if amount == 0 {
			return state, nil
		}
		actor.CurrentHP += amount
		state.replaceMember(actorRef.Side, actor)
		return state, []Event{SkillHealingAppliedEvent{
			Type: EventKindSkillHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, SkillID: skill.SkillID, Source: SkillHealingSourceFixed, Amount: amount, CurrentHP: actor.CurrentHP,
		}}
	}
	amount := roundedHalfUpPercent(actor.MaxHP, uint8(-skill.HealingPercent))
	amount = min(amount, actor.CurrentHP)
	if amount == 0 {
		return state, nil
	}
	actor.CurrentHP -= amount
	state.replaceMember(actorRef.Side, actor)
	events := []Event{SkillRecoilDamageAppliedEvent{
		Type: EventKindSkillRecoilDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, Amount: amount, SourceDamageAmount: actor.MaxHP, CurrentHP: actor.CurrentHP,
	}}
	if actor.CurrentHP == 0 {
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: actorRef, Cause: FaintCauseSkillRecoil, SkillID: skill.SkillID,
		})
	}
	return state, events
}

// applyTargetSkillHealing 按技能实际目标最大生命值的显式分数回复目标。
//
// 波动类技能会复用使用者当前特性的精确倍率：先按目标最大生命计算基础回复并至少取 1，再乘倍率向下
// 取整，最后按目标缺失生命夹取。该规则不消费随机数，也不把目标回复误记为使用者自身固定回复。
func applyTargetSkillHealing(state State, actorRef, targetRef MemberRef, skill SkillSnapshot) (State, []Event) {
	if skill.TargetHealingNumerator == 0 || skill.TargetHealingDenominator == 0 {
		return state, nil
	}
	actor, actorExists := state.member(actorRef.Side, actorRef.Position)
	target, targetExists := state.member(targetRef.Side, targetRef.Position)
	if !actorExists || actor.CurrentHP == 0 || !targetExists || target.CurrentHP == 0 || target.CurrentHP >= target.MaxHP {
		return state, nil
	}
	// 先在 uint64 中保留完整中间结果。目标最大生命与 16 位特性倍率的乘积可能超过 uint32；若在按
	// 缺失生命夹取前缩回 uint32，会发生回绕并把本应回满的技能错误变成较小回复量。
	amount := max(uint64(target.MaxHP)*uint64(skill.TargetHealingNumerator)/uint64(skill.TargetHealingDenominator), 1)
	if skill.PulseBased && actor.PulseBasedSkillDamageBoost != nil {
		amount = max(amount*uint64(actor.PulseBasedSkillDamageBoost.Numerator)/uint64(actor.PulseBasedSkillDamageBoost.Denominator), 1)
	}
	amount = min(amount, uint64(target.MaxHP-target.CurrentHP))
	if amount == 0 {
		return state, nil
	}
	appliedAmount := uint32(amount)
	target.CurrentHP += appliedAmount
	state.replaceMember(targetRef.Side, target)
	return state, []Event{SkillHealingAppliedEvent{
		Type: EventKindSkillHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: targetRef, SkillID: skill.SkillID, Source: SkillHealingSourceTargetMaximumHP,
		Amount: appliedAmount, CurrentHP: target.CurrentHP,
	}}
}

// roundedHalfUpPercent 计算正整数基数乘以百分比后的四舍五入结果；正百分比且正基数至少得到 1。
func roundedHalfUpPercent(base uint32, percent uint8) uint32 {
	if base == 0 || percent == 0 {
		return 0
	}
	return max(uint32((uint64(base)*uint64(percent)+50)/100), 1)
}

// accuracyAfterStages 返回命中与闪避阶级共同修正后的整数命中率。
// 基础值 0 保留“必中”语义；其它结果按现代 3 分母曲线向下取整，并夹取到 1 至 100。
func accuracyAfterStages(base uint8, actor, target MemberSnapshot, skill SkillSnapshot) uint8 {
	if base == 0 {
		return 0
	}
	actorAccuracyStage := actor.StatStages[StatAccuracy]
	if target.IgnoreOpponentAccuracyStatStages && !ignoresTargetAbilityEffects(actor, skill) {
		actorAccuracyStage = 0
	}
	targetEvasionStage := target.StatStages[StatEvasion]
	if actor.IgnoreOpponentAccuracyStatStages {
		targetEvasionStage = 0
	}
	accuracyNumerator, accuracyDenominator := accuracyStageRatio(actorAccuracyStage)
	evasionNumerator, evasionDenominator := accuracyStageRatio(targetEvasionStage)
	value := uint64(base) * accuracyNumerator * evasionDenominator / (accuracyDenominator * evasionNumerator)
	return uint8(max(uint64(1), min(uint64(100), value)))
}

// skillAccuracy 返回本次命中判定使用的最终命中率。
//
// 一击必杀使用独立规则：先根据使用者是否拥有本次技能属性选择基础命中率，再叠加等级差；它不读取技能普通
// Accuracy 字段，也不受命中或闪避能力阶级影响。其它技能会先读取当前天气的显式命中覆盖；覆盖为 0 表示必中，
// 其余覆盖和基础 Accuracy 一样继续受命中/闪避能力阶级影响。
func skillAccuracy(weather *WeatherEffect, actor, target MemberSnapshot, skill SkillSnapshot) uint8 {
	if skill.damageMode() == SkillDamageModeOneHitKnockOut {
		base := skill.OneHitKnockOutBaseAccuracy
		if skill.OneHitKnockOutSameElementUserBaseAccuracy != 0 && containsString(actor.ElementIDs, effectiveSkillElementForMember(actor, skill, weather)) {
			base = skill.OneHitKnockOutSameElementUserBaseAccuracy
		}
		accuracy := int(base) + int(actor.Level) - int(target.Level)
		if accuracy < 1 {
			return 1
		}
		return uint8(min(accuracy, 100))
	}
	if actor.AccuracyAlwaysHits || target.AccuracyAlwaysHits && !ignoresTargetAbilityEffects(actor, skill) {
		return 0
	}
	base := skill.Accuracy
	if override, found := weatherAccuracy(skill.WeatherAccuracyOverrides, weather); found {
		base = override
	}
	accuracy := accuracyAfterStages(base, actor, target, skill)
	accuracy = applyAbilityAccuracyMultiplier(weather, actor, target, skill, accuracy)
	if actor.ItemID != 0 && actor.HeldItemAccuracyBoost && accuracy != 0 {
		accuracy = uint8(min(uint16(100), uint16(accuracy)*11/10))
	}
	if target.ItemID != 0 && target.HeldItemOpponentAccuracyReduction && accuracy != 0 {
		accuracy = uint8(max(uint16(1), uint16(accuracy)*9/10))
	}
	if actor.ItemID != 0 && actor.HeldItemAccuracyAfterTargetActedBoost && actor.LastSkillActionTurn != 0 &&
		target.LastSkillActionTurn == actor.LastSkillActionTurn && accuracy != 0 {
		accuracy = uint8(min(uint16(100), uint16(accuracy)*6/5))
	}
	if skill.DamageClass == DamageClassStatus && target.StatusSkillAccuracyCap != 0 && !ignoresTargetAbilityEffects(actor, skill) {
		accuracy = min(accuracy, target.StatusSkillAccuracyCap)
	}
	return accuracy
}

// accuracyStageRatio 把 -6 至 6 的命中或闪避阶级转换成精确的分子与分母。
func accuracyStageRatio(stage int8) (uint64, uint64) {
	if stage >= 0 {
		return uint64(3 + stage), 3
	}
	return 3, uint64(3 - stage)
}

// resolveStatStageEffects 按资料声明顺序应用技能的普通能力阶级增减效果。
// 每项效果独立消费概率随机数，并从上一项效果产生的最新状态读取接收者；达到边界而没有实际变化时不写事件。
func resolveStatStageEffects(
	state State,
	actorRef MemberRef,
	selectedTargetRef MemberRef,
	skill SkillSnapshot,
	random RandomSource,
	includeSelectedTarget bool,
	includeUser bool,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	events := make([]Event, 0, len(skill.StatStageEffects))
	trace := make([]RandomTraceEntry, 0, len(skill.StatStageEffects))
	for _, effect := range skill.StatStageEffects {
		if effect.Target == EffectTargetSelected && !includeSelectedTarget || effect.Target == EffectTargetUser && !includeUser {
			continue
		}
		if effect.ChancePercent == 0 {
			continue
		}
		if effect.ChancePercent < 100 {
			roll, nextRandom, entry, err := random.Next(100, "stat stage chance for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			if roll+1 > int32(effect.ChancePercent) {
				continue
			}
		}
		targetRef := selectedTargetRef
		if effect.Target == EffectTargetUser {
			targetRef = actorRef
		}
		target, ok := state.member(targetRef.Side, targetRef.Position)
		if !ok || target.CurrentHP == 0 {
			continue
		}
		if effect.StageDelta < 0 && targetRef.Side != actorRef.Side && target.ItemID != 0 && target.HeldItemOpponentStatStageReductionImmunity {
			continue
		}
		before := target.StatStages[effect.Stat]
		after := max(int8(-6), min(int8(6), before+effect.StageDelta))
		if before == after {
			continue
		}
		stages := make(map[Stat]int8, len(target.StatStages)+1)
		for stat, stage := range target.StatStages {
			stages[stat] = stage
		}
		stages[effect.Stat] = after
		target.StatStages = stages
		state.replaceMember(targetRef.Side, target)
		events = append(events, StatStageChangedEvent{
			Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, Stat: effect.Stat, Delta: after - before, CurrentStage: after,
		})
		if effect.StageDelta < 0 && target.ItemID != 0 && target.HeldItemNegativeStatStageReset {
			var resetEvents []Event
			state, resetEvents = applyHeldItemNegativeStatStageReset(state, targetRef)
			events = append(events, resetEvents...)
		}
		if effect.StageDelta > 0 && targetRef == actorRef {
			var copyEvents []Event
			state, copyEvents = applyHeldItemOpponentPositiveStatStageCopy(state, actorRef)
			events = append(events, copyEvents...)
		}
	}
	return state, random, events, trace, nil
}

// applyHeldItemNegativeStatStageReset 清除持有者的全部负能力阶级并消费白色香草类道具。
// 每个实际恢复的能力项均发布普通能力阶级事件，因此重放无需识别非结构化的道具文本。
func applyHeldItemNegativeStatStageReset(state State, targetRef MemberRef) (State, []Event) {
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !found || target.ItemID == 0 || !target.HeldItemNegativeStatStageReset {
		return state, nil
	}
	itemID := target.ItemID
	stages := cloneStatStages(target.StatStages)
	events := make([]Event, 0, len(stages))
	for _, stat := range []Stat{StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion} {
		before := stages[stat]
		if before >= 0 {
			continue
		}
		stages[stat] = 0
		events = append(events, StatStageChangedEvent{
			Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: targetRef, Target: targetRef, Stat: stat, Delta: -before, CurrentStage: 0,
		})
	}
	if len(events) == 0 {
		return state, nil
	}
	target.StatStages = stages
	target = clearHeldItemRuntimeState(target)
	state.replaceMember(targetRef.Side, target)
	events = append(events, HeldItemStatReactionConsumedEvent{
		Type: EventKindHeldItemStatReactionConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Target: targetRef, ItemID: itemID, Reason: "negativeStatStageReset",
	})
	return state, events
}

// resolveMajorStatusCures 在一次技能已经成功处理至少一个目标后，按三个相互独立的资料语义清除主要异常。
//
// 自我净化只处理使用者；同侧上场净化只处理当前处于场上槽位的成员；整队净化则还会处理后备成员。
// 某条资料即使同时声明多个范围，也会按成员稳定位置去重，确保同一个主要异常最多产生一条清除事件。
// 清除时必须同步归零睡眠与剧毒的附属计数，否则重放快照会留下与空主要异常互相矛盾的运行态。
func resolveMajorStatusCures(state State, actorRef MemberRef, skill SkillSnapshot) (State, []Event) {
	if !skill.CuresUserMajorStatus && !skill.CuresUserSideActiveMajorStatuses && !skill.CuresUserSideMajorStatuses {
		return state, nil
	}

	targets := make([]MemberRef, 0, MaximumMembersPerSide)
	seen := make(map[MemberPosition]struct{}, MaximumMembersPerSide)
	appendTarget := func(position MemberPosition) {
		if _, duplicate := seen[position]; duplicate {
			return
		}
		seen[position] = struct{}{}
		targets = append(targets, MemberRef{Side: actorRef.Side, Position: position})
	}
	if skill.CuresUserMajorStatus {
		appendTarget(actorRef.Position)
	}
	for _, side := range state.sides {
		if side.Side != actorRef.Side {
			continue
		}
		if skill.CuresUserSideActiveMajorStatuses {
			for _, position := range side.ActiveMembers {
				appendTarget(position)
			}
		}
		if skill.CuresUserSideMajorStatuses {
			for _, member := range side.Members {
				appendTarget(member.Position)
			}
		}
		break
	}

	events := make([]Event, 0, len(targets))
	for _, targetRef := range targets {
		target, found := state.member(targetRef.Side, targetRef.Position)
		if !found || target.MajorStatus == "" {
			continue
		}
		status := target.MajorStatus
		target.MajorStatus = ""
		target.BadPoisonCounter = 0
		target.SleepTurnsRemaining = 0
		state.replaceMember(targetRef.Side, target)
		events = append(events, MajorStatusClearedEvent{
			Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: targetRef, Status: status,
		})
	}
	return state, events
}

func resolveMajorStatusApplications(
	state State,
	actorRef MemberRef,
	selectedTargetRef MemberRef,
	skill SkillSnapshot,
	random RandomSource,
	includeSelectedTarget bool,
	includeUser bool,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	events := make([]Event, 0, len(skill.StatusApplications))
	trace := make([]RandomTraceEntry, 0, len(skill.StatusApplications))
	for _, application := range skill.StatusApplications {
		if application.Target == EffectTargetSelected && !includeSelectedTarget || application.Target == EffectTargetUser && !includeUser {
			continue
		}
		if application.ChancePercent == 0 {
			continue
		}
		if application.ChancePercent < 100 {
			roll, nextRandom, entry, err := random.Next(100, "status chance for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			if roll+1 > int32(application.ChancePercent) {
				continue
			}
		}

		targetRef := selectedTargetRef
		if application.Target == EffectTargetUser {
			targetRef = actorRef
		}
		// 每一项效果都从上一项效果产生的最新权威状态重新读取目标。
		// 这样同一技能中的后续异常会看见已经写入的首项异常，并产生稳定的 existingStatus 阻止事件。
		target, ok := state.member(targetRef.Side, targetRef.Position)
		if !ok {
			continue
		}
		if target.CurrentHP == 0 {
			continue
		}
		if target.MajorStatus != "" {
			events = append(events, MajorStatusBlockedEvent{
				Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetRef, Status: application.Status,
				Reason: MajorStatusBlockReasonExistingStatus,
			})
			continue
		}
		if majorStatusBlockedByElement(state.rules, target, application.Status) {
			events = append(events, MajorStatusBlockedEvent{
				Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetRef, Status: application.Status,
				Reason: MajorStatusBlockReasonElementImmunity,
			})
			continue
		}
		if terrainBlocksMajorStatus(state.environment.Terrain, state.rules, target, application.Status) {
			events = append(events, MajorStatusBlockedEvent{
				Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actorRef, Target: targetRef, Status: application.Status, Reason: MajorStatusBlockReasonTerrainImmunity,
			})
			continue
		}
		target.MajorStatus = application.Status
		switch application.Status {
		case MajorStatusBadPoison:
			target.BadPoisonCounter = 1
		case MajorStatusSleep:
			roll, nextRandom, entry, err := random.Next(3, "sleep duration for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			target.SleepTurnsRemaining = roll + 1
		}
		state.replaceMember(targetRef.Side, target)
		events = append(events, MajorStatusAppliedEvent{
			Type: EventKindMajorStatusApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, Status: application.Status,
		})
		var paralysisCureEvents []Event
		state, paralysisCureEvents = applyHeldItemParalysisCure(state, targetRef, application.Status)
		events = append(events, paralysisCureEvents...)
		var sleepCureEvents []Event
		state, sleepCureEvents = applyHeldItemSleepCure(state, targetRef, application.Status)
		events = append(events, sleepCureEvents...)
		var poisonCureEvents []Event
		state, poisonCureEvents = applyHeldItemPoisonCure(state, targetRef, application.Status)
		events = append(events, poisonCureEvents...)
		var burnCureEvents []Event
		state, burnCureEvents = applyHeldItemBurnCure(state, targetRef, application.Status)
		events = append(events, burnCureEvents...)
		var freezeCureEvents []Event
		state, freezeCureEvents = applyHeldItemFreezeCure(state, targetRef, application.Status)
		events = append(events, freezeCureEvents...)
		var allMajorStatusCureEvents []Event
		state, allMajorStatusCureEvents = applyHeldItemAllMajorStatusCure(state, targetRef, application.Status)
		events = append(events, allMajorStatusCureEvents...)
	}
	return state, random, events, trace, nil
}

// resolveVolatileStatusApplications 按资料声明顺序尝试把易变状态写入目标或使用者。
//
// 每项状态拥有独立的概率和时长随机接点；状态已存在时不会重置时长。disable 不根据技能名称猜测
// 目标，而是明确读取目标已经记录的 LastUsedSkillPosition，因而在重放、审计和双打并发行动中保持稳定。
func resolveVolatileStatusApplications(
	state State,
	actorRef MemberRef,
	selectedTargetRef MemberRef,
	skill SkillSnapshot,
	random RandomSource,
	includeSelectedTarget bool,
	includeUser bool,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	events := make([]Event, 0, len(skill.VolatileStatusApplications))
	trace := make([]RandomTraceEntry, 0, len(skill.VolatileStatusApplications)*2)
	for _, application := range skill.VolatileStatusApplications {
		if application.Target == EffectTargetSelected && !includeSelectedTarget || application.Target == EffectTargetUser && !includeUser {
			continue
		}
		targetRef := selectedTargetRef
		if application.Target == EffectTargetUser {
			targetRef = actorRef
		}
		if application.ChancePercent < 100 {
			roll, nextRandom, entry, err := random.Next(100, "volatile status chance for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			if roll+1 > int32(application.ChancePercent) {
				continue
			}
		}
		target, exists := state.member(targetRef.Side, targetRef.Position)
		if !exists || target.CurrentHP == 0 {
			continue
		}
		if application.Status == VolatileStatusSubstitute {
			if target.SubstituteHP != 0 {
				events = append(events, SkillFailedEvent{
					Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
					Reason: SkillFailureReasonSubstituteAlreadyActive,
				})
				continue
			}
			cost := substituteCost(target.MaxHP, application.SubstituteCostNumerator, application.SubstituteCostDenominator)
			if cost == 0 || target.CurrentHP <= cost {
				events = append(events, SkillFailedEvent{
					Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
					Reason: SkillFailureReasonInsufficientHPForSubstitute,
				})
				continue
			}
			target.CurrentHP -= cost
			target.SubstituteHP = cost
			state.replaceMember(targetRef.Side, target)
			events = append(events, SubstituteStartedEvent{
				Type: EventKindSubstituteStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
				HPCost: cost, SubstituteHP: target.SubstituteHP,
			})
			continue
		}
		if target.hasVolatileStatus(application.Status) {
			continue
		}
		if application.Status == VolatileStatusProtection {
			// 首次保护无需掷骰；连续成功时将分母按 3 的幂递增。这里将概率判定放在资料通用
			// ChancePercent 之后，确保保护本身的稳定配置仍保持“必定尝试”，而不是由管理端
			// 任意百分比绕过连续使用规则。
			denominator := protectionChanceDenominator(target.ProtectionChain)
			if denominator > 1 {
				roll, nextRandom, entry, err := random.Next(denominator, "protection chance for "+skill.SkillID.String())
				if err != nil {
					return State{}, RandomSource{}, nil, nil, err
				}
				random = nextRandom
				trace = append(trace, entry)
				if roll != 0 {
					events = append(events, SkillFailedEvent{
						Type: EventKindSkillFailed, SchemaVersion: 1, TurnNumber: state.turnNumber,
						Actor: actorRef, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
						Reason: SkillFailureReasonProtectionFailed,
					})
					continue
				}
			}
		}
		turns := application.MinTurns
		actor, actorFound := state.member(actorRef.Side, actorRef.Position)
		if application.Status == VolatileStatusBinding && actorFound && actor.ItemID != 0 && actor.HeldItemBindingTurns != 0 {
			turns = actor.HeldItemBindingTurns
		} else if application.MaxTurns > application.MinTurns {
			roll, nextRandom, entry, err := random.Next(int32(application.MaxTurns-application.MinTurns+1), "volatile status duration for "+skill.SkillID.String())
			if err != nil {
				return State{}, RandomSource{}, nil, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			turns += uint8(roll)
		}
		position := SkillPosition(0)
		switch application.Status {
		case VolatileStatusConfusion:
			target.ConfusionTurnsRemaining = turns
		case VolatileStatusBinding:
			target.BindingTurnsRemaining = turns
			target.BindingDamageDenominator = 0
			if actorFound && actor.ItemID != 0 {
				target.BindingDamageDenominator = actor.HeldItemBindingDamageDenominator
			}
		case VolatileStatusTaunt:
			target.TauntTurnsRemaining = turns
		case VolatileStatusProtection:
			target.ProtectionTurnsRemaining = turns
			target.ProtectionChain++
		case VolatileStatusLockedMove:
			// 锁招总时长包括已经成功执行的当前回合；只保留后续仍必须重复的次数。
			if turns <= 1 {
				continue
			}
			target.LockedSkillPosition = skill.Position
			target.LockedTurnsRemaining = turns - 1
			position = skill.Position
		case VolatileStatusDisable:
			if target.LastUsedSkillPosition == 0 {
				continue
			}
			target.DisabledSkillPosition = target.LastUsedSkillPosition
			target.DisabledTurnsRemaining = turns
			position = target.LastUsedSkillPosition
		case VolatileStatusCharging:
			// 首段蓄力在 resolveUseSkill 的 PP 消耗路径中处理；完成段已从临时技能快照移除。
			continue
		default:
			return State{}, RandomSource{}, nil, nil, ErrInvalidInitialState
		}
		state.replaceMember(targetRef.Side, target)
		events = append(events, VolatileStatusAppliedEvent{
			Type: EventKindVolatileStatusApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: actorRef, Target: targetRef, SkillID: skill.SkillID, Status: application.Status,
			TurnsRemaining: turns, SkillPosition: position,
		})
		var confusionCureEvents []Event
		state, confusionCureEvents = applyHeldItemConfusionCure(state, targetRef, application.Status)
		events = append(events, confusionCureEvents...)
	}
	return state, random, events, trace, nil
}

// hasVolatileStatus 报告成员是否已经持有指定状态。重复施加不刷新持续时间，避免顺序不同的范围技能
// 改写随机轨迹或形成隐式无限持续状态。
// protectionChanceDenominator 返回下一次保护成功率使用的随机分母。ProtectionChain 为 0 时首
// 次保护必定成功；每一次连续成功都把下次分母乘以 3，并对 int32 上限夹取以保持随机源边界合法。
func protectionChanceDenominator(chain uint8) int32 {
	denominator := int64(1)
	for index := uint8(0); index < chain && denominator < math.MaxInt32; index++ {
		denominator = min(denominator*3, int64(math.MaxInt32))
	}
	return int32(denominator)
}

func (member MemberSnapshot) hasVolatileStatus(status VolatileStatus) bool {
	switch status {
	case VolatileStatusConfusion:
		return member.ConfusionTurnsRemaining != 0
	case VolatileStatusBinding:
		return member.BindingTurnsRemaining != 0
	case VolatileStatusProtection:
		return member.ProtectionTurnsRemaining != 0
	case VolatileStatusSubstitute:
		return member.SubstituteHP != 0
	case VolatileStatusTaunt:
		return member.TauntTurnsRemaining != 0
	case VolatileStatusCharging:
		return member.ChargingTurnsRemaining != 0
	case VolatileStatusLockedMove:
		return member.LockedTurnsRemaining != 0
	case VolatileStatusDisable:
		return member.DisabledTurnsRemaining != 0
	default:
		return false
	}
}

// substituteCost 返回建立替身应支付的本体生命值。
//
// 费用按最大生命与资料分数向下取整，正常情况下至少为 1；随后夹取到 maxHP-1，确保建立替身不会让
// 使用者本体归零。最大生命为 1 时不存在合法费用，调用方会把返回的 0 解释为 HP 不足而明确失败。
func substituteCost(maxHP uint32, numerator, denominator uint8) uint32 {
	if maxHP == 0 || numerator == 0 || denominator == 0 {
		return 0
	}
	cost := uint32(uint64(maxHP) * uint64(numerator) / uint64(denominator))
	cost = max(cost, 1)
	return min(cost, maxHP-1)
}

// majorStatusBlockedByElement 报告目标当前属性是否提供指定主要异常的规则免疫。
// RuleSnapshot 使用稳定属性 code 定位当前实时资料中的实际 Identifier，避免引擎把数据库标识写死在代码里。
func majorStatusBlockedByElement(rules RuleSnapshot, target MemberSnapshot, status MajorStatus) bool {
	var immuneElementCodes []string
	switch status {
	case MajorStatusBurn:
		immuneElementCodes = []string{"fire"}
	case MajorStatusPoison, MajorStatusBadPoison:
		immuneElementCodes = []string{"poison", "steel"}
	case MajorStatusParalysis:
		immuneElementCodes = []string{"electric"}
	case MajorStatusFreeze:
		immuneElementCodes = []string{"ice"}
	case MajorStatusSleep:
		return false
	}
	for _, code := range immuneElementCodes {
		if elementID := rules.ElementIDs[code]; elementID != 0 && containsString(target.ElementIDs, elementID) {
			return true
		}
	}
	return false
}

// leechSeedBlockedByElement 报告目标是否拥有草属性而免疫寄生种子。
//
// 规则快照以属性 code 到实时资料 Identifier 的映射表达免疫，避免纯引擎将某个部署环境的属性 Identifier 固化到代码中。
func leechSeedBlockedByElement(rules RuleSnapshot, target MemberSnapshot) bool {
	grassElementID := rules.ElementIDs["grass"]
	return grassElementID != 0 && containsString(target.ElementIDs, grassElementID)
}

// skillIsSuperEffectiveAgainstTarget 判断本次伤害技能相对于目标当前属性是否严格克制。
//
// 相性乘积与普通伤害公式使用同一份冻结关系表和强风弱点中和规则；0 倍、等倍、抗性及任意多属性组合的
// 最终倍率小于或等于 1 都返回 false。该判断只服务于命中前特性 gate，不产生伤害、事件或随机消费。
func skillIsSuperEffectiveAgainstTarget(state State, actor, target MemberSnapshot, skill SkillSnapshot) bool {
	numerator, denominator := uint64(1), uint64(1)
	skillElementID := effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))
	strongWeather := effectiveStrongWeather(state)
	for _, defenseElementID := range target.ElementIDs {
		effectivenessNumerator, effectivenessDenominator := state.rules.effectiveness(skillElementID, defenseElementID)
		if strongWeather != nil && strongWindsNeutralizeFlyingWeakness(*strongWeather, state.rules, skillElementID, defenseElementID) {
			effectivenessNumerator, effectivenessDenominator = 1, 1
		}
		if effectivenessNumerator == 0 {
			return false
		}
		numerator *= uint64(effectivenessNumerator)
		denominator *= uint64(effectivenessDenominator)
	}
	return numerator > denominator
}

func calculateDamage(
	rules RuleSnapshot,
	strongWeather *StrongWeatherState,
	weather *WeatherEffect,
	terrain *TerrainEffect,
	defenderConditions SideConditionSnapshot,
	format FormatSnapshot,
	attacker, defender MemberSnapshot,
	skill SkillSnapshot,
	randomPercent uint8,
	criticalHit bool,
	multiTarget bool,
	allyModifierValues ...abilityAllyDamageModifiers,
) uint32 {
	allyModifiers := abilityAllyDamageModifiers{}
	if len(allyModifierValues) != 0 {
		allyModifiers = allyModifierValues[0]
	}
	skillElementID := effectiveSkillElementForMember(attacker, skill, weather)
	attackingStat := attacker.Stats.Attack
	defendingStat := defender.Stats.Defense
	attackingStage := attacker.StatStages[StatAttack]
	defendingStage := defender.StatStages[StatDefense]
	if skill.DamageClass == DamageClassSpecial {
		attackingStat = attacker.Stats.SpecialAttack
		defendingStat = defender.Stats.SpecialDefense
		attackingStage = attacker.StatStages[StatSpecialAttack]
		defendingStage = defender.StatStages[StatSpecialDefense]
	}
	// 该特性始终只移除“对手”一侧参与本次公式的能力阶级：使用者忽略目标防守，目标忽略使用者攻击。
	// 不能写回 StatStages，否则一次伤害会永久篡改后续回合和其它目标观察到的权威状态。
	if attacker.IgnoreOpponentDamageStatStages {
		defendingStage = 0
	}
	if defender.IgnoreOpponentDamageStatStages && !ignoresTargetAbilityEffects(attacker, skill) {
		attackingStage = 0
	}
	if criticalHit && attackingStage < 0 {
		attackingStage = 0
	}
	if criticalHit && defendingStage > 0 {
		defendingStage = 0
	}
	attackingStat = modifiedBattleStat(attackingStat, attackingStage)
	defendingStat = modifiedBattleStat(defendingStat, defendingStage)
	ignoreDefenderAbility := ignoresTargetAbilityEffects(attacker, skill)
	attackingStatKind, defendingStatKind := StatAttack, StatDefense
	if skill.DamageClass == DamageClassSpecial {
		attackingStatKind, defendingStatKind = StatSpecialAttack, StatSpecialDefense
	}
	// 攻击侧特性能力倍率在能力阶级之后合并并只取整一次。目标拥有的攻击能力修正规则属于防守特性，
	// 因而与其它目标侧特性共享统一穿透开关；伙伴互助组倍率则来自当前场上事实。
	attackingStat = attackingStatAfterAbility(
		attackingStat, attacker, defender, attackingStatKind, weather, terrain, ignoreDefenderAbility,
		allyModifiers.attackingStatMultiplier,
	)
	if skill.DamageClass == DamageClassSpecial && defender.ItemID != 0 && defender.HeldItemSpecialDefenseBoost {
		defendingStat = max(uint32(uint64(defendingStat)*3/2), 1)
	}
	if skill.DamageClass == DamageClassPhysical && attacker.MajorStatus == MajorStatusBurn &&
		!attackingStatMultiplierIgnoresBurn(attacker, weather, terrain) {
		attackingStat = max(attackingStat/2, 1)
	}
	defendingStat = weatherDefenseModifier(weather, rules, defender, skill.DamageClass, defendingStat)
	// 防守侧特性能力倍率在能力阶级、道具及天气修正之后进入基础伤害整数除法。攻击方对目标防御能力的
	// 修正规则属于攻击方自身特性，不会被“无视目标特性”误删。
	defendingStat = defendingStatAfterAbility(defendingStat, attacker, defender, defendingStatKind, terrain, ignoreDefenderAbility)
	attackNumerator, attackDenominator := highestStatMultiplier(attacker, weather, terrain, attackingStatKind)
	attackingStat = applyHighestStatMultiplier(attackingStat, attackNumerator, attackDenominator)
	defenseNumerator, defenseDenominator := highestStatMultiplier(defender, weather, terrain, defendingStatKind)
	defendingStat = applyHighestStatMultiplier(defendingStat, defenseNumerator, defenseDenominator)
	levelFactor := uint64(2*uint32(attacker.Level)/5 + 2)
	basePower := dynamicPower(skill, attacker, defender)
	basePower = heldItemElementDamageBoostPower(basePower, attacker, skillElementID)
	basePower = heldItemDamageClassPowerBoost(basePower, attacker, skill.DamageClass)
	basePower = heldItemConsumableElementDamageBoostPower(basePower, attacker, skillElementID)
	basePower = heldItemPunchBasedPower(basePower, attacker, skill)
	powerNumerator, powerDenominator := weatherPowerMultiplier(skill, weather)
	// 天气资料倍率修正的是进入普通公式的基础威力。先保留分数参与整数计算，避免先截断 1/2、3/2 等倍率后
	// 让低威力技能无端损失精度；后续随机、同属性、环境、屏障和相性修正仍沿用各自独立的结算阶段。
	conversionNumerator, conversionDenominator := skillElementConversionPowerMultiplier(attacker, skill, weather)
	baseDamage := ((levelFactor * uint64(basePower) * powerNumerator * conversionNumerator / powerDenominator / conversionDenominator * uint64(attackingStat) /
		uint64(defendingStat)) / 50) + 2
	numerator := baseDamage * uint64(randomPercent)
	denominator := uint64(100)
	if multiTarget {
		// 现代双打中实际命中两个及以上目标的范围伤害使用 0.75 修正。调用方只会在
		// 目标集合在行动开始时确实超过一个时传入 true，避免某个目标后续未命中影响倍率。
		numerator *= 3
		denominator *= 4
	}
	sameElementNumerator, sameElementDenominator := abilitySameElementBonus(attacker, skillElementID)
	numerator *= sameElementNumerator
	denominator *= sameElementDenominator
	weatherNumerator, weatherDenominator := weatherDamageModifier(weather, rules, skillElementID)
	numerator *= weatherNumerator
	denominator *= weatherDenominator
	terrainNumerator, terrainDenominator := terrainDamageModifier(terrain, rules, attacker, defender, skill, skillElementID)
	numerator *= terrainNumerator
	denominator *= terrainDenominator
	if heldItemElementDamageReductionApplies(rules, strongWeather, defender, skillElementID) {
		denominator *= 2
	}
	contactAbilityNumerator, contactAbilityDenominator := contactDamageAbilityModifier(attacker, defender, skill)
	numerator *= contactAbilityNumerator
	denominator *= contactAbilityDenominator
	protectionNumerator, protectionDenominator := protectionBypassDamageMultiplier(attacker, defender, skill)
	numerator *= protectionNumerator
	denominator *= protectionDenominator
	fireAbilityNumerator, fireAbilityDenominator := fireDamageAbilityModifier(rules, attacker, defender, skill, skillElementID)
	numerator *= fireAbilityNumerator
	denominator *= fireAbilityDenominator
	barrierNumerator, barrierDenominator := sideBarrierDamageModifier(defenderConditions, format, skill.DamageClass, criticalHit)
	numerator *= barrierNumerator
	denominator *= barrierDenominator
	if criticalHit {
		numerator *= 3
		denominator *= 2
	}
	effectivenessNumeratorTotal, effectivenessDenominatorTotal := uint64(1), uint64(1)
	for _, defenderElementID := range defender.ElementIDs {
		effectivenessNumerator, effectivenessDenominator := rules.effectiveness(skillElementID, defenderElementID)
		if effectivenessNumerator == 0 && defender.ItemID != 0 && defender.HeldItemTypeImmunitySuppression {
			effectivenessNumerator, effectivenessDenominator = 1, 1
		}
		if strongWeather != nil && strongWindsNeutralizeFlyingWeakness(*strongWeather, rules, skillElementID, defenderElementID) {
			effectivenessNumerator, effectivenessDenominator = 1, 1
		}
		numerator *= uint64(effectivenessNumerator)
		denominator *= uint64(effectivenessDenominator)
		effectivenessNumeratorTotal *= uint64(effectivenessNumerator)
		effectivenessDenominatorTotal *= uint64(effectivenessDenominator)
	}
	if attacker.ItemID != 0 && attacker.HeldItemSuperEffectiveDamageBoost && effectivenessNumeratorTotal > effectivenessDenominatorTotal {
		numerator *= 6
		denominator *= 5
	}
	if attacker.ItemID != 0 && attacker.HeldItemDamageBoostWithRecoil {
		numerator *= 13
		denominator *= 10
	}
	consecutiveNumerator, consecutiveDenominator := heldItemConsecutiveSkillDamageMultiplier(attacker, skill)
	numerator *= consecutiveNumerator
	denominator *= consecutiveDenominator
	if attacker.ChargedElementID != 0 && attacker.ChargedElementID == skillElementID && attacker.ChargedDamageNumerator != 0 && attacker.ChargedDamageDenominator != 0 {
		numerator *= uint64(attacker.ChargedDamageNumerator)
		denominator *= uint64(attacker.ChargedDamageDenominator)
	}
	return finalizeDamageWithAbilityMultiplier(
		numerator,
		denominator,
		weather,
		attacker,
		defender,
		skill,
		skillElementID,
		criticalHit,
		effectivenessNumeratorTotal,
		effectivenessDenominatorTotal,
		ignoreDefenderAbility,
		allyModifiers,
	)
}

// applyHeldItemDamageBoostRecoil 在伤害强化道具成功造成任意实际技能伤害后，按持有者最大生命十分之一反伤。
//
// actualDamage 同时包含本体和替身实际损失，零伤害不触发。反伤属于间接伤害，因此受通用间接伤害免疫保护；
// 道具不会消费，多段技能则逐段触发，与规则的伤害后 hook 保持一致。
func applyHeldItemDamageBoostRecoil(state State, actorRef MemberRef, skill SkillSnapshot, actualDamage uint32) (State, []Event) {
	if actualDamage == 0 {
		return state, nil
	}
	actor, found := state.member(actorRef.Side, actorRef.Position)
	if !found || actor.CurrentHP == 0 || actor.ItemID == 0 || !actor.HeldItemDamageBoostWithRecoil || actor.IndirectDamageImmunity {
		return state, nil
	}
	amount := min(max(actor.MaxHP/10, uint32(1)), actor.CurrentHP)
	actor.CurrentHP -= amount
	state.replaceMember(actorRef.Side, actor)
	events := []Event{HeldItemRecoilDamageAppliedEvent{
		Type: EventKindHeldItemRecoilDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, ItemID: actor.ItemID, Amount: amount, CurrentHP: actor.CurrentHP,
	}}
	if actor.CurrentHP == 0 {
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: actorRef, Cause: FaintCauseHeldItemDamage, SkillID: skill.SkillID,
		})
	}
	return state, events
}

// applyHeldItemDamageDealtHealing 按本段实际技能伤害的八分之一回复持有者生命，至少回复 1 点并按缺失生命封顶。
func applyHeldItemDamageDealtHealing(state State, actorRef MemberRef, skill SkillSnapshot, actualDamage uint32) (State, []Event) {
	if actualDamage == 0 {
		return state, nil
	}
	actor, found := state.member(actorRef.Side, actorRef.Position)
	if !found || actor.CurrentHP == 0 || actor.CurrentHP == actor.MaxHP || actor.ItemID == 0 || !actor.HeldItemDamageDealtHeal {
		return state, nil
	}
	amount := min(max(actualDamage/8, uint32(1)), actor.MaxHP-actor.CurrentHP)
	actor.CurrentHP += amount
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{SkillHealingAppliedEvent{
		Type: EventKindSkillHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, Source: SkillHealingSourceHeldItemDamageDealt, Amount: amount, CurrentHP: actor.CurrentHP,
	}}
}

// clearHeldItemAirborneAfterBodyDamage 关闭气球类道具的受伤前空中状态，但保留道具所有权供公开与转移规则读取。
func clearHeldItemAirborneAfterBodyDamage(state State, targetRef MemberRef, bodyDamage uint32) (State, []Event) {
	if bodyDamage == 0 {
		return state, nil
	}
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !found || target.ItemID == 0 || !target.HeldItemAirborneUntilDamaged {
		return state, nil
	}
	target.HeldItemAirborneUntilDamaged = false
	state.replaceMember(targetRef.Side, target)
	return state, []Event{HeldItemAirborneEndedEvent{
		Type: EventKindHeldItemAirborneEnded, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Target: targetRef, ItemID: target.ItemID,
	}}
}

// heldItemElementDamageBoostPower 返回传统属性强化携带道具对匹配属性技能提供的固定威力强化。
//
// Item Metadata 使用 ElementDamageBoostElementID 冻结道具的属性身份，Battle 再把该身份写入
// HeldItemElementID；引擎只比较冻结 Identifier，不根据道具名称、显示文本或实时资料推断属性。现代规则中的这批
// 非消耗型道具统一把普通直接伤害技能的有效威力乘以 6/5，属性不匹配或道具已经失去时保持原威力。固定伤害、
// 比例伤害和间接伤害不会进入普通伤害公式，因而不会错误获得本倍率。
func heldItemElementDamageBoostPower(basePower uint16, attacker MemberSnapshot, skillElementID Identifier) uint16 {
	if attacker.ItemID == 0 || attacker.HeldItemElementID == 0 || skillElementID != attacker.HeldItemElementID {
		return basePower
	}
	return max(uint16(uint64(basePower)*6/5), uint16(1))
}

// heldItemDamageClassPowerBoost 返回力量头带和博识眼镜对匹配伤害分类提供的固定威力强化。
//
// 两条冻结开关分别限定物理与特殊普通伤害，均使用现代规则的 11/10 有效威力倍率。变化技能不会进入普通伤害
// 公式；固定伤害和比例伤害也绕过本函数。道具失去后 ItemID 为空，即使旧快照开关仍被错误保留也不能生效。
func heldItemDamageClassPowerBoost(basePower uint16, attacker MemberSnapshot, damageClass DamageClass) uint16 {
	if attacker.ItemID == 0 {
		return basePower
	}
	strongBattlees := damageClass == DamageClassPhysical && attacker.HeldItemPhysicalDamagePowerBoost50 ||
		damageClass == DamageClassSpecial && attacker.HeldItemSpecialDamagePowerBoost50
	if strongBattlees {
		return max(uint16(uint64(basePower)*3/2), uint16(1))
	}
	matches := damageClass == DamageClassPhysical && attacker.HeldItemPhysicalDamagePowerBoost ||
		damageClass == DamageClassSpecial && attacker.HeldItemSpecialDamagePowerBoost
	if !matches {
		return basePower
	}
	return max(uint16(uint64(basePower)*11/10), uint16(1))
}

// heldItemPunchBasedPower 返回持有道具对拳击类技能在普通直接伤害威力阶段提供的固定强化结果。
//
// 该规则只适用于仍持有非空道具、具有对应冻结效果且明确标记为拳击类的技能。固定伤害、比例伤害和间接伤害
// 不会进入普通伤害公式，因而不会调用本函数；PunchBased 也不隐含 MakesContact。
func heldItemPunchBasedPower(basePower uint16, attacker MemberSnapshot, skill SkillSnapshot) uint16 {
	if attacker.ItemID == 0 || !attacker.HeldItemPunchBasedSkillPowerBoost || !skill.PunchBased {
		return basePower
	}
	return max(uint16(uint64(basePower)*11/10), uint16(1))
}

// heldItemConsumableElementDamageBoostPower 返回一次性属性威力强化道具在普通伤害威力阶段贡献的倍率结果。
//
// 该函数只计算当前仍持有道具时的本次伤害，绝不在这里消费运行态；消费必须等真实本体伤害写入后由独立函数
// 执行。固定伤害、比例伤害和间接伤害不会调用普通伤害公式，故不会错误触发该规则。
func heldItemConsumableElementDamageBoostPower(basePower uint16, attacker MemberSnapshot, skillElementID Identifier) uint16 {
	if attacker.ItemID == 0 || attacker.HeldItemConsumableElementDamageBoostElementID == 0 ||
		attacker.HeldItemConsumableElementDamageBoostNumerator == 0 || attacker.HeldItemConsumableElementDamageBoostDenominator == 0 ||
		skillElementID != attacker.HeldItemConsumableElementDamageBoostElementID {
		return basePower
	}
	return max(uint16(uint64(basePower)*uint64(attacker.HeldItemConsumableElementDamageBoostNumerator)/uint64(attacker.HeldItemConsumableElementDamageBoostDenominator)), uint16(1))
}

// sideBarrierDamageModifier 返回防守方侧状态对普通伤害的明确分数修正。
//
// 反射壁只处理物理、光墙只处理特殊、极光幕处理两种普通伤害；同一次伤害最多采用一个适用屏障，避免并存状态
// 产生错误叠乘。要害忽略屏障。单打按二分之一、双打按三分之二减伤，变化技能永远不经过该路径。
func sideBarrierDamageModifier(
	conditions SideConditionSnapshot,
	format FormatSnapshot,
	damageClass DamageClass,
	criticalHit bool,
) (uint64, uint64) {
	if criticalHit || damageClass == DamageClassStatus {
		return 1, 1
	}
	applies := damageClass == DamageClassPhysical && conditions.Reflect != nil ||
		damageClass == DamageClassSpecial && conditions.LightScreen != nil || conditions.AuroraVeil != nil
	if !applies {
		return 1, 1
	}
	if format.ActiveSlotsPerSide > 1 {
		return 2, 3
	}
	return 1, 2
}

// weatherDamageModifier 返回普通天气对指定技能属性伤害的明确分数修正。
//
// 日照强化火属性并削弱水属性；降雨的方向相反。属性代码只通过规则快照映射到实时资料 Identifier，因而纯引擎
// 不会把某个部署环境的属性主键或显示名称写死在伤害公式中。沙暴和降雪目前没有伤害倍率；降雪防御加成以及
// 命中、特性、道具、强天气、天气封锁均属于后续独立能力，不能用这里的默认值伪装为已经支持。
func weatherDamageModifier(weather *WeatherEffect, rules RuleSnapshot, skillElementID Identifier) (uint64, uint64) {
	if weather == nil {
		return 1, 1
	}
	fireElementID := rules.ElementIDs["fire"]
	waterElementID := rules.ElementIDs["water"]
	switch weather.Kind {
	case WeatherKindSun:
		if skillElementID != 0 && skillElementID == fireElementID {
			return 3, 2
		}
		if skillElementID != 0 && skillElementID == waterElementID {
			return 1, 2
		}
	case WeatherKindRain:
		if skillElementID != 0 && skillElementID == waterElementID {
			return 3, 2
		}
		if skillElementID != 0 && skillElementID == fireElementID {
			return 1, 2
		}
	}
	return 1, 1
}

// weatherDefenseModifier 返回普通天气对防守方已应用能力阶级后的防御数值的明确修正。
//
// 降雪使冰属性成员的物防按 1.5 倍参与普通伤害公式；沙暴使岩石属性成员的特防按 1.5 倍参与公式。两项修正
// 都在能力阶级之后、伤害基础值计算之前应用，与规则定义的顺序一致。缺少对应属性代码时不猜测资料 ID，也
// 不应用修正；特性、道具、强天气和天气封锁仍由后续独立机制负责。
func weatherDefenseModifier(
	weather *WeatherEffect,
	rules RuleSnapshot,
	defender MemberSnapshot,
	damageClass DamageClass,
	defendingStat uint32,
) uint32 {
	if weather == nil {
		return defendingStat
	}
	iceElementID := rules.ElementIDs["ice"]
	rockElementID := rules.ElementIDs["rock"]
	if weather.Kind == WeatherKindSnow && damageClass == DamageClassPhysical && iceElementID != 0 &&
		containsString(defender.ElementIDs, iceElementID) {
		return max(defendingStat*3/2, 1)
	}
	if weather.Kind == WeatherKindSandstorm && damageClass == DamageClassSpecial && rockElementID != 0 &&
		containsString(defender.ElementIDs, rockElementID) {
		return max(defendingStat*3/2, 1)
	}
	return defendingStat
}

// terrainDamageModifier 返回普通场地对指定技能属性伤害的明确分数修正。
//
// 电气、青草和精神场地分别强化接地使用者的同属性技能；薄雾场地使针对接地目标的龙属性伤害减半；青草场地还会
// 使带有 WeakenedByGrassyTerrain 标记且命中接地目标的技能伤害减半。接地判断只读取冻结规则快照中的飞行属性
// 映射，绝不猜测资料 Identifier。各场地修正会先合并为一个精确分数，再与其它最终伤害倍率统一取整，避免 1.3 与 0.5
// 分别取整造成可重放结果漂移。精神场地对先制行动的阻止、异常免疫以及气球/特性等接地变化由各自状态机制接入。
func terrainDamageModifier(
	terrain *TerrainEffect,
	rules RuleSnapshot,
	attacker, defender MemberSnapshot,
	skill SkillSnapshot,
	skillElementID Identifier,
) (uint64, uint64) {
	if terrain == nil || skillElementID == 0 {
		return 1, 1
	}
	if terrain.Kind == TerrainKindMisty && skillElementID == rules.ElementIDs["dragon"] && memberGrounded(rules, defender) {
		return 1, 2
	}
	switch terrain.Kind {
	case TerrainKindElectric:
		if memberGrounded(rules, attacker) && skillElementID == rules.ElementIDs["electric"] {
			return 13, 10
		}
	case TerrainKindGrassy:
		numerator, denominator := uint64(1), uint64(1)
		if memberGrounded(rules, attacker) && skillElementID == rules.ElementIDs["grass"] {
			numerator *= 13
			denominator *= 10
		}
		if skill.WeakenedByGrassyTerrain && memberGrounded(rules, defender) {
			denominator *= 2
		}
		return numerator, denominator
	case TerrainKindPsychic:
		if memberGrounded(rules, attacker) && skillElementID == rules.ElementIDs["psychic"] {
			return 13, 10
		}
	}
	return 1, 1
}

// terrainBlocksMajorStatus 报告接地目标是否被当前普通场地阻止获得新的主要异常。
//
// 电气场地仅阻止睡眠，薄雾场地阻止所有主要异常。此处只处理基础场地的确定规则；特性、道具、重力及其它
// 接地变化会在各自的成员运行态机制完成后共享 memberGrounded 的扩展实现，不能伪装为属性常量。
func terrainBlocksMajorStatus(terrain *TerrainEffect, rules RuleSnapshot, target MemberSnapshot, status MajorStatus) bool {
	if terrain == nil || !memberGrounded(rules, target) {
		return false
	}
	return terrain.Kind == TerrainKindMisty || terrain.Kind == TerrainKindElectric && status == MajorStatusSleep
}

// psychicTerrainBlocksPrioritySkill 报告精神场地是否阻止一项正优先度技能影响接地对手。
//
// 判定在目标已经按当前场上槽位重新解析后、命中和伤害随机数之前执行。这样换人后的接地状态会被正确读取，且
// 被场地阻止的目标不会污染命中、要害或伤害随机轨迹。范围技能会逐目标判定，因此仍可影响同次范围内的非接地
// 对手；使用者自身或同侧目标不受此规则影响。
func psychicTerrainBlocksPrioritySkill(
	terrain *TerrainEffect,
	rules RuleSnapshot,
	actorSlot, targetSlot SlotRef,
	target MemberSnapshot,
	skill SkillSnapshot,
) bool {
	return terrain != nil && terrain.Kind == TerrainKindPsychic && skill.Priority > 0 && actorSlot.Side != targetSlot.Side &&
		memberGrounded(rules, target)
}

// priorityMoveImmunityForSideBlocker 返回阻止本次技能的目标侧当前上场特性拥有者。
//
// 该规则只处理对手正优先度的定向技能。保护范围由每个拥有者独立声明：拥有者总会保护自己，而只有
// PriorityMoveImmunityForSideProtectsAllies 为 true 时才扩展到当前上场伙伴。攻击方的无视目标特性规则
// 必须在这里整体绕过，不能在命中后再撤销伤害或追加效果，以保证 PP 消耗和随机轨迹准确。
func priorityMoveImmunityForSideBlocker(
	state State,
	actorSlot, targetSlot SlotRef,
	actor MemberSnapshot,
	skill SkillSnapshot,
) (MemberRef, bool) {
	if skill.Priority <= 0 || actorSlot.Side == targetSlot.Side || ignoresTargetAbilityEffects(actor, skill) {
		return MemberRef{}, false
	}
	target, found := state.ActiveMember(targetSlot)
	if !found || target.CurrentHP == 0 {
		return MemberRef{}, false
	}
	for _, side := range state.sides {
		if side.Side != targetSlot.Side {
			continue
		}
		for _, activePosition := range side.ActiveMembers {
			for _, member := range side.Members {
				if member.Position != activePosition || member.CurrentHP == 0 || !member.PriorityMoveImmunityForSideEnabled {
					continue
				}
				if member.Position == target.Position || member.PriorityMoveImmunityForSideProtectsAllies {
					return MemberRef{Side: side.Side, Position: member.Position}, true
				}
			}
		}
	}
	return MemberRef{}, false
}

// ignoresTargetAbilityEffects 判断本次技能是否应跳过对手成员提供的防守特性。
//
// 普通无视目标特性开关适用于所有技能；变化技能后置特性则严格限制在 DamageClassStatus，不能意外穿透
// 伤害技能面对的要害、能力阶级、强制换人或属性相关防守。调用方仍须自行判断目标是否为对手，避免该语义
// 扩散到同侧辅助和使用者自身的生命周期规则。
func ignoresTargetAbilityEffects(actor MemberSnapshot, skill SkillSnapshot) bool {
	return actor.IgnoreTargetAbilityEffects || actor.StatusSkillMovesLastAndIgnoresTargetAbility && skill.DamageClass == DamageClassStatus
}

func resolveEndTurnMajorStatusDamage(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, activePosition := range side.ActiveMembers {
			member, exists := state.member(side.Side, activePosition)
			if !exists || member.CurrentHP == 0 {
				continue
			}
			if rule := reactiveRules(member).MajorStatusEndTurnHealing; rule != nil && containsMajorStatus(rule.Statuses, member.MajorStatus) {
				if member.CurrentHP < member.MaxHP {
					healing := min(max(member.MaxHP/uint32(rule.Denominator), 1), member.MaxHP-member.CurrentHP)
					member.CurrentHP += healing
					state.replaceMember(side.Side, member)
					ref := MemberRef{Side: side.Side, Position: member.Position}
					events = append(events, AbilityHPChangedEvent{Type: EventKindAbilityHPChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Source: ref, Target: ref, Effect: "majorStatusEndTurnHealing", Healing: true, Amount: healing, CurrentHP: member.CurrentHP})
				}
				continue
			}
			if member.IndirectDamageImmunity {
				continue
			}
			var damage uint32
			switch member.MajorStatus {
			case MajorStatusBurn, MajorStatusPoison:
				damage = max(member.MaxHP/16, 1)
			case MajorStatusBadPoison:
				scaledDamage := uint64(member.MaxHP) * uint64(member.BadPoisonCounter) / 16
				if scaledDamage >= uint64(member.MaxHP) {
					damage = member.MaxHP
				} else {
					damage = max(uint32(scaledDamage), 1)
				}
			default:
				continue
			}
			actualDamage := min(damage, member.CurrentHP)
			member.CurrentHP -= actualDamage
			state.replaceMember(side.Side, member)
			events = append(events, MajorStatusDamageAppliedEvent{
				Type: EventKindMajorStatusDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: MemberRef{Side: side.Side, Position: member.Position}, Status: member.MajorStatus,
				Amount: actualDamage, CurrentHP: member.CurrentHP,
			})
			if member.CurrentHP == 0 {
				events = append(events, ParticipantFaintedEvent{
					Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: MemberRef{Side: side.Side, Position: member.Position},
					Cause:  FaintCauseMajorStatusDamage, MajorStatus: member.MajorStatus,
				})
			} else if member.MajorStatus == MajorStatusBadPoison && member.BadPoisonCounter < math.MaxInt32 {
				member.BadPoisonCounter++
				state.replaceMember(side.Side, member)
			}
		}
	}
	return state, events
}

// resolveEndTurnVolatileStatusEffects 处理必须在主要异常伤害之后结算的束缚伤害与持续时间。
//
// 束缚只作用于仍在场且可战斗的成员；它禁止主动换人，因此不会在正常流程中跨成员位置残留。每次
// 回合末先造成最大生命八分之一的固定伤害，再递减持续时间，最后一次伤害后立即写出解除事件。
func resolveEndTurnVolatileStatusEffects(state State) (State, []Event) {
	events := make([]Event, 0, 6)
	for _, side := range state.sides {
		for _, activePosition := range side.ActiveMembers {
			member, exists := state.member(side.Side, activePosition)
			if !exists || member.CurrentHP == 0 {
				continue
			}
			actorRef := MemberRef{Side: side.Side, Position: member.Position}
			protectedThisTurn := member.ProtectionTurnsRemaining != 0
			if protectedThisTurn {
				member.ProtectionTurnsRemaining--
				state.replaceMember(side.Side, member)
				if member.ProtectionTurnsRemaining == 0 {
					events = append(events, VolatileStatusClearedEvent{
						Type: EventKindVolatileStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
						Target: actorRef, Status: VolatileStatusProtection,
					})
				}
			}
			// 保护清除后重新读取成员，确保同一回合中与束缚共存时不会用旧副本覆盖状态更新。
			member, _ = state.member(side.Side, activePosition)
			if !protectedThisTurn && member.ProtectionChain != 0 {
				// 只有本回合成功保护的成员可以把连续计数带入下一回合；任何其他行动、被阻止或
				// 未尝试保护都会重新开始概率链。
				member.ProtectionChain = 0
				state.replaceMember(side.Side, member)
			}
			// 命中锁定从建立回合起经过两个回合末：当前回合结束后仅保留一次后续命中机会，
			// 下一回合结束后彻底清除。重新读取可避免保护链更新覆盖本次生命周期推进。
			member, _ = state.member(side.Side, activePosition)
			if member.AccuracyLockTarget != nil || member.AccuracyLockTurnsRemaining != 0 {
				member = advanceAccuracyLockAtEndTurn(member)
				state.replaceMember(side.Side, member)
			}
			member, _ = state.member(side.Side, activePosition)
			if member.BindingTurnsRemaining == 0 {
				continue
			}
			// 束缚状态本身不因免疫而清除；免疫期间不扣血，也不消耗束缚持续回合。
			if member.IndirectDamageImmunity {
				continue
			}
			denominator := uint32(member.BindingDamageDenominator)
			if denominator == 0 {
				denominator = 8
			}
			damage := min(max(member.MaxHP/denominator, 1), member.CurrentHP)
			member.CurrentHP -= damage
			member.BindingTurnsRemaining--
			state.replaceMember(side.Side, member)
			events = append(events, VolatileStatusDamageAppliedEvent{
				Type: EventKindVolatileStatusDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: actorRef, Status: VolatileStatusBinding, Amount: damage, CurrentHP: member.CurrentHP,
			})
			if member.CurrentHP == 0 {
				events = append(events, ParticipantFaintedEvent{
					Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: actorRef, Cause: FaintCauseVolatileStatusDamage, VolatileStatus: VolatileStatusBinding,
				})
			}
			if member.BindingTurnsRemaining == 0 {
				member.BindingDamageDenominator = 0
				state.replaceMember(side.Side, member)
				events = append(events, VolatileStatusClearedEvent{
					Type: EventKindVolatileStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: actorRef, Status: VolatileStatusBinding,
				})
			}
		}
	}
	return state, events
}

// resolveEndTurnLeechSeedEffects 在主要异常和束缚之后结算仍在场的寄生种子。
//
// 抽取量始终取目标最大生命的八分之一且至少为 1。目标本次实际剩余生命不足该数值时，事件中的伤害量会
// 按当前生命夹取；但来源的回复仍按理论抽取量计算，这是寄生种子“吸取固定份额”而不是“转移实际伤害”的
// 语义。来源保存为槽位，因此原使用者换下后，该槽位当前可战斗的成员仍会获得回复。
func resolveEndTurnLeechSeedEffects(state State) (State, []Event) {
	events := make([]Event, 0, 4)
	for _, side := range state.sides {
		for _, activePosition := range side.ActiveMembers {
			target, exists := state.member(side.Side, activePosition)
			if !exists || target.CurrentHP == 0 || target.LeechSeedSourceSlot == nil || target.IndirectDamageImmunity {
				continue
			}
			sourceSlot := *target.LeechSeedSourceSlot
			recipient, recipientExists := state.ActiveMember(sourceSlot)
			// 来源槽位没有可战斗成员时保留种子但不进行抽取。这样来源下一回合补位后，持续状态仍能
			// 正确回复到该槽位的新成员；目标自身离场则会在换人路径中清除种子。
			if !recipientExists || recipient.CurrentHP == 0 {
				continue
			}
			targetRef := MemberRef{Side: side.Side, Position: target.Position}
			theoreticalDrain := max(target.MaxHP/8, 1)
			actualDamage := min(theoreticalDrain, target.CurrentHP)
			target.CurrentHP -= actualDamage
			state.replaceMember(side.Side, target)
			events = append(events, LeechSeedDamageAppliedEvent{
				Type: EventKindLeechSeedDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: targetRef, SourceSlot: sourceSlot, Amount: actualDamage, CurrentHP: target.CurrentHP,
			})
			if target.CurrentHP == 0 {
				events = append(events, ParticipantFaintedEvent{
					Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: targetRef, Cause: FaintCauseLeechSeed,
				})
			}

			// 重新读取接收者，以便未来的结算阶段即使扩展为可修改场上成员，也不会用陈旧快照覆盖其它状态。
			recipient, recipientExists = state.ActiveMember(sourceSlot)
			if !recipientExists || recipient.CurrentHP == 0 {
				continue
			}
			healing := min(theoreticalDrain, recipient.MaxHP-recipient.CurrentHP)
			if healing == 0 {
				continue
			}
			recipient.CurrentHP += healing
			state.replaceMember(sourceSlot.Side, recipient)
			events = append(events, LeechSeedHealingAppliedEvent{
				Type: EventKindLeechSeedHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: targetRef, Recipient: MemberRef{Side: sourceSlot.Side, Position: recipient.Position},
				Amount: healing, CurrentHP: recipient.CurrentHP,
			})
		}
	}
	return state, events
}

// resolveEndTurnEnvironmentEffects 按全场环境的统一回合末阶段推进持续效果。
//
// 场地速度顺序不会在效果建立当回合重新规划已经冻结的行动队列；这里只减少剩余回合，新的比较方向仅在下一次
// ResolveTurn 生成行动计划时读取。未来天气、地形和侧状态会以各自的显式阶段加入此函数，不能抢占现有阶段顺序。
func resolveEndTurnEnvironmentEffects(state State) (State, []Event) {
	terrain, terrainEvents := resolveEndTurnTerrainEffects(state)
	state = terrain
	weather, weatherEvents := resolveEndTurnWeatherEffects(state)
	state = weather
	state, weatherFormEvents := synchronizeWeatherForms(state)
	environmentEvents := append(append(terrainEvents, weatherEvents...), weatherFormEvents...)
	effect := state.environment.FieldSpeedOrder
	if effect == nil {
		return state, environmentEvents
	}
	next := effect.advanceTurn()
	if next != nil {
		state.environment.FieldSpeedOrder = next
		return state, environmentEvents
	}
	state.environment.FieldSpeedOrder = nil
	return state, append(environmentEvents, FieldSpeedOrderEndedEvent{
		Type: EventKindFieldSpeedOrderEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, FieldSpeedOrderKind: effect.Kind,
	})
}

// resolveEndTurnHeldItemHealing 在环境回复完成后、侧状态持续时间推进前结算持有道具的固定比例回复。
//
// 该阶段只读取当前仍持有道具的存活成员。回复量始终基于最大生命计算、至少为 1 点，并按缺失生命夹取；因此
// 已满生命的成员不会写入零回复事件。属性条件回复读取当前 ElementIDs，故太晶化、形态变化与道具属性身份都会
// 即时影响结算。规则没有概率、持续时间或消费语义，绝不能读取或追加随机轨迹。
func resolveEndTurnHeldItemHealing(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, ok := state.member(side.Side, position)
			if !ok || member.CurrentHP == 0 || member.CurrentHP >= member.MaxHP || member.ItemID == 0 {
				continue
			}
			if member.HeldItemEndTurnHealDenominator != 0 {
				denominator := member.HeldItemEndTurnHealDenominator
				healing := min(max(member.MaxHP/uint32(denominator), 1), member.MaxHP-member.CurrentHP)
				member.CurrentHP += healing
				state.replaceMember(side.Side, member)
				events = append(events, HeldItemHealingAppliedEvent{
					Type: EventKindHeldItemHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: MemberRef{Side: side.Side, Position: member.Position}, ItemID: member.ItemID,
					Denominator: denominator, Amount: healing, CurrentHP: member.CurrentHP,
				})
			}
			// 重新读取成员，使无条件回复的状态写入不会被属性条件回复的旧快照覆盖；同一道具若资料明确声明两种
			// 效果，二者按效果列表顺序连续结算，并各自留下可重放的结构化事件。
			member, ok = state.member(side.Side, position)
			if !ok || member.CurrentHP == 0 || member.CurrentHP >= member.MaxHP || member.ItemID == 0 ||
				member.HeldItemEndTurnHealForElementID == 0 || member.HeldItemEndTurnHealForElementDenominator == 0 ||
				!containsString(member.ElementIDs, member.HeldItemEndTurnHealForElementID) {
				continue
			}
			denominator := member.HeldItemEndTurnHealForElementDenominator
			healing := min(max(member.MaxHP/uint32(denominator), 1), member.MaxHP-member.CurrentHP)
			member.CurrentHP += healing
			state.replaceMember(side.Side, member)
			events = append(events, HeldItemHealingAppliedEvent{
				Type: EventKindHeldItemHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: MemberRef{Side: side.Side, Position: member.Position}, ItemID: member.ItemID,
				Denominator: denominator, Amount: healing, CurrentHP: member.CurrentHP,
			})
		}
	}
	return state, events
}

// resolveEndTurnHeldItemDamage 在持有道具回复之后、侧状态持续时间推进前结算持有道具的固定比例自伤。
//
// 规则只读取当前仍持有道具的存活成员，按最大生命计算至少 1 点伤害并按当前生命夹取。属性条件自伤读取当前
// ElementIDs，故太晶化、形态变化与道具属性身份都会即时改变触发条件。它是独立于技能伤害、接触与天气的间接
// 伤害，因此 IndirectDamageImmunity 会阻止它；规则没有概率、持续时间或消费语义，不能读取随机源。
func resolveEndTurnHeldItemDamage(state State) (State, []Event) {
	events := make([]Event, 0, 3)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, ok := state.member(side.Side, position)
			if !ok || member.CurrentHP == 0 || member.ItemID == 0 || member.IndirectDamageImmunity {
				continue
			}
			if member.HeldItemEndTurnDamageDenominator != 0 {
				denominator := member.HeldItemEndTurnDamageDenominator
				damage := min(max(member.MaxHP/uint32(denominator), 1), member.CurrentHP)
				member.CurrentHP -= damage
				state.replaceMember(side.Side, member)
				ref := MemberRef{Side: side.Side, Position: member.Position}
				events = append(events, HeldItemDamageAppliedEvent{
					Type: EventKindHeldItemDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: ref, ItemID: member.ItemID, Denominator: denominator, Amount: damage, CurrentHP: member.CurrentHP,
				})
				if member.CurrentHP == 0 {
					events = append(events, ParticipantFaintedEvent{
						Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
						Target: ref, Cause: FaintCauseHeldItemDamage,
					})
				}
			}
			// 重新读取成员，以避免无条件自伤写入覆盖属性条件自伤；若前一效果已使成员倒下，后续效果自然不再触发。
			member, ok = state.member(side.Side, position)
			if !ok || member.CurrentHP == 0 || member.ItemID == 0 || member.IndirectDamageImmunity ||
				member.HeldItemEndTurnDamageWithoutElementID == 0 || member.HeldItemEndTurnDamageWithoutElementDenominator == 0 ||
				containsString(member.ElementIDs, member.HeldItemEndTurnDamageWithoutElementID) {
				continue
			}
			denominator := member.HeldItemEndTurnDamageWithoutElementDenominator
			damage := min(max(member.MaxHP/uint32(denominator), 1), member.CurrentHP)
			member.CurrentHP -= damage
			state.replaceMember(side.Side, member)
			ref := MemberRef{Side: side.Side, Position: member.Position}
			events = append(events, HeldItemDamageAppliedEvent{
				Type: EventKindHeldItemDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: ref, ItemID: member.ItemID, Denominator: denominator, Amount: damage, CurrentHP: member.CurrentHP,
			})
			if member.CurrentHP == 0 {
				events = append(events, ParticipantFaintedEvent{
					Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: ref, Cause: FaintCauseHeldItemDamage,
				})
			}
		}
	}
	return state, events
}

// resolveEndTurnSideConditions 推进所有阵营侧状态的生命周期。
//
// 侧状态在全场环境之后结算：它们不会影响同一回合已经冻结的行动计划，且成员状态、寄生种子、场地与天气的
// 回合末结果已经先于侧状态到期事件写入。每种持续屏障均保留独立状态写入和结束事件，不能借由通用效果数组
// 合并，从而保持重放事件和资料规则的领域语义。
func resolveEndTurnSideConditions(state State) (State, []Event) {
	events := make([]Event, 0, 5)
	for index := range state.sides {
		if effect := state.sides[index].Conditions.Reflect; effect != nil {
			if next := effect.advanceTurn(); next != nil {
				state.sides[index].Conditions.Reflect = next
			} else {
				state.sides[index].Conditions.Reflect = nil
				events = append(events, ReflectEndedEvent{
					Type: EventKindReflectEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Side: state.sides[index].Side,
				})
			}
		}
		if effect := state.sides[index].Conditions.LightScreen; effect != nil {
			if next := effect.advanceTurn(); next != nil {
				state.sides[index].Conditions.LightScreen = next
			} else {
				state.sides[index].Conditions.LightScreen = nil
				events = append(events, LightScreenEndedEvent{
					Type: EventKindLightScreenEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Side: state.sides[index].Side,
				})
			}
		}
		if effect := state.sides[index].Conditions.AuroraVeil; effect != nil {
			if next := effect.advanceTurn(); next != nil {
				state.sides[index].Conditions.AuroraVeil = next
			} else {
				state.sides[index].Conditions.AuroraVeil = nil
				events = append(events, AuroraVeilEndedEvent{
					Type: EventKindAuroraVeilEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Side: state.sides[index].Side,
				})
			}
		}
		effect := state.sides[index].Conditions.Tailwind
		if effect == nil {
			continue
		}
		if next := effect.advanceTurn(); next != nil {
			state.sides[index].Conditions.Tailwind = next
			continue
		}
		state.sides[index].Conditions.Tailwind = nil
		events = append(events, TailwindEndedEvent{
			Type: EventKindTailwindEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Side: state.sides[index].Side,
		})
	}
	return state, events
}

// resolveEndTurnTerrainEffects 先结算青草场地对接地成员的回复，再推进普通场地的持续回合。
func resolveEndTurnTerrainEffects(state State) (State, []Event) {
	effect := state.environment.Terrain
	if effect == nil {
		return state, nil
	}
	events := make([]Event, 0, 5)
	if effect.Kind == TerrainKindGrassy {
		for _, side := range state.sides {
			for _, position := range side.ActiveMembers {
				member, ok := state.member(side.Side, position)
				if !ok || member.CurrentHP == 0 || member.CurrentHP >= member.MaxHP || !memberGrounded(state.rules, member) {
					continue
				}
				healing := min(max(member.MaxHP/16, 1), member.MaxHP-member.CurrentHP)
				member.CurrentHP += healing
				state.replaceMember(side.Side, member)
				events = append(events, TerrainHealingAppliedEvent{Type: EventKindTerrainHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: MemberRef{Side: side.Side, Position: member.Position}, Terrain: effect.Kind, Amount: healing, CurrentHP: member.CurrentHP})
			}
		}
	}
	if next := effect.advanceTurn(); next != nil {
		state.environment.Terrain = next
		return state, events
	}
	state.environment.Terrain = nil
	return state, append(events, TerrainEndedEvent{Type: EventKindTerrainEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Terrain: effect.Kind})
}

// memberGrounded 报告成员是否能受到当前仅依据属性可判断的接地环境效果影响。
//
// 纯引擎通过规则快照读取飞行属性 Identifier；快照没有提供该映射时按接地处理，避免把未知资料主键猜成飞行属性。
// 强制接地道具优先于受伤前空中道具和飞行属性；其它尚未冻结的接地来源不能由名称或实时资料推断。
func memberGrounded(rules RuleSnapshot, member MemberSnapshot) bool {
	if member.ItemID != 0 && member.HeldItemForceGrounded {
		return true
	}
	if member.ItemID != 0 && member.HeldItemAirborneUntilDamaged {
		return false
	}
	flyingElementID := rules.ElementIDs["flying"]
	return flyingElementID == 0 || !containsString(member.ElementIDs, flyingElementID)
}

// resolveEndTurnWeatherEffects 先结算特性天气回复和沙暴伤害，再推进普通天气的持续回合。
//
// 这里读取 effectiveWeather 而不是直接把原始环境天气用于规则结算：天气封锁特性会暂停所有普通天气的
// 可执行效果，包括这类特性回复，但绝不会停止原始天气的回合递减和自然结束事件。
func resolveEndTurnWeatherEffects(state State) (State, []Event) {
	effect := state.environment.Weather
	if effect == nil {
		return state, nil
	}
	events := make([]Event, 0, 5)
	if effective := effectiveWeather(state); effective != nil {
		for _, side := range state.sides {
			for _, position := range side.ActiveMembers {
				member, ok := state.member(side.Side, position)
				if !ok || member.CurrentHP == 0 || member.CurrentHP == member.MaxHP {
					continue
				}
				denominator, heals := healsInWeather(member, effective.Kind)
				if !heals {
					continue
				}
				healing := min(max(member.MaxHP/denominator, 1), member.MaxHP-member.CurrentHP)
				member.CurrentHP += healing
				state.replaceMember(side.Side, member)
				events = append(events, WeatherHealingAppliedEvent{
					Type: EventKindWeatherHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Target: MemberRef{Side: side.Side, Position: member.Position}, Weather: effective.Kind,
					Amount: healing, CurrentHP: member.CurrentHP,
				})
			}
		}
	}
	if effect.Kind == WeatherKindSandstorm && effectiveWeather(state) != nil {
		for _, side := range state.sides {
			for _, position := range side.ActiveMembers {
				member, ok := state.member(side.Side, position)
				if !ok || member.CurrentHP == 0 || member.IndirectDamageImmunity || sandstormImmune(state.rules, member) || immuneToWeatherDamage(member, effect.Kind) || (member.ItemID != 0 && member.HeldItemSandstormDamageImmunity) {
					continue
				}
				damage := min(max(member.MaxHP/16, 1), member.CurrentHP)
				member.CurrentHP -= damage
				state.replaceMember(side.Side, member)
				ref := MemberRef{Side: side.Side, Position: member.Position}
				events = append(events, WeatherDamageAppliedEvent{Type: EventKindWeatherDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref, Weather: effect.Kind, Amount: damage, CurrentHP: member.CurrentHP})
				if member.CurrentHP == 0 {
					events = append(events, ParticipantFaintedEvent{Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref, Cause: FaintCauseWeather})
				}
			}
		}
	}
	if next := effect.advanceTurn(); next != nil {
		state.environment.Weather = next
		return state, events
	}
	state.environment.Weather = nil
	return state, append(events, WeatherEndedEvent{Type: EventKindWeatherEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Weather: effect.Kind})
}

// sandstormImmune 报告成员是否因岩石、地面或钢属性免疫沙暴伤害。
func sandstormImmune(rules RuleSnapshot, member MemberSnapshot) bool {
	for _, code := range []string{"rock", "ground", "steel"} {
		if id := rules.ElementIDs[code]; id != 0 && containsString(member.ElementIDs, id) {
			return true
		}
	}
	return false
}

func (rules RuleSnapshot) effectiveness(attackElementID, defenseElementID Identifier) (uint16, uint16) {
	for _, effectiveness := range rules.ElementEffectiveness {
		if effectiveness.AttackElementID == attackElementID && effectiveness.DefenseElementID == defenseElementID {
			return effectiveness.Numerator, effectiveness.Denominator
		}
	}
	return 1, 1
}

func containsString[T comparable](values []T, target T) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

func (state *State) replaceMember(sideID Side, replacement MemberSnapshot) {
	for sideIndex := range state.sides {
		if state.sides[sideIndex].Side != sideID {
			continue
		}
		for memberIndex := range state.sides[sideIndex].Members {
			if state.sides[sideIndex].Members[memberIndex].Position == replacement.Position {
				state.sides[sideIndex].Members[memberIndex] = cloneMember(replacement)
				return
			}
		}
	}
}

func (state State) member(sideID Side, position MemberPosition) (MemberSnapshot, bool) {
	for _, side := range state.sides {
		if side.Side != sideID {
			continue
		}
		for _, member := range side.Members {
			if member.Position == position {
				return cloneMember(member), true
			}
		}
	}
	return MemberSnapshot{}, false
}

func (state State) isActive(sideID Side, position MemberPosition) bool {
	for _, side := range state.sides {
		if side.Side != sideID {
			continue
		}
		for _, activePosition := range side.ActiveMembers {
			if activePosition == position {
				return true
			}
		}
	}
	return false
}

func (state *State) switchActive(slot SlotRef, nextPosition MemberPosition) {
	for sideIndex := range state.sides {
		if state.sides[sideIndex].Side == slot.Side {
			state.sides[sideIndex].ActiveMembers[slot.Position-1] = nextPosition
			return
		}
	}
}

// leaveBattlefield 清理成员离开场上槽位后不应保留的运行态，并在需要时先恢复变身前画像。
//
// 变身必须先还原，否则本次入场期间从目标复制的技能、PP 和特性规则会泄露到后备成员。其余清理项只属于
// 连续在场周期：主要异常、生命和道具仍保留，剧毒倍率、能力阶级与易变状态则按既有换人规则复位。
func leaveBattlefield(member MemberSnapshot) MemberSnapshot {
	member = restoreTransformSnapshot(member)
	member = restoreHeldItemElementIdentity(member)
	member.ApparentCreatureID = 0
	if member.MajorStatus == MajorStatusBadPoison {
		member.BadPoisonCounter = 1
	}
	member.StatStages = make(map[Stat]int8)
	member.ConfusionTurnsRemaining = 0
	member.BindingTurnsRemaining = 0
	member.BindingDamageDenominator = 0
	member.ProtectionTurnsRemaining = 0
	member.ProtectionChain = 0
	member.SubstituteHP = 0
	member.TauntTurnsRemaining = 0
	member.ChargingSkillPosition = 0
	member.ChargingTurnsRemaining = 0
	member.RechargeTurnsRemaining = 0
	member.AccuracyLockTarget = nil
	member.AccuracyLockTurnsRemaining = 0
	member.LockedSkillPosition = 0
	member.LockedTurnsRemaining = 0
	member.DisabledSkillPosition = 0
	member.DisabledTurnsRemaining = 0
	member.HeldItemChoiceLockedSkillPosition = 0
	member.LastSkillActionTurn = 0
	member.LastDeclaredSkillID = 0
	member.ConsecutiveDeclaredSkillUses = 0
	member.HalfHPThresholdAbilityActivated = false
	member.ChargedElementID = 0
	member.ChargedDamageNumerator = 1
	member.ChargedDamageDenominator = 1
	member.LeechSeedSourceSlot = nil
	return member
}
