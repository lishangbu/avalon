package battleengine

// ParticipantTerastallizedEvent 记录成员已在技能实际开始结算前完成太晶化。
//
// 事件中的属性来自 Battle 启动时冻结的 TeraElementID，而不是客户端输入或实时游戏资料。它与阵营机会状态、
// 成员运行态一同进入 Turn Record，使离线重放能够验证一次太晶化没有被重复消费。
type ParticipantTerastallizedEvent struct {
	// Type 是固定事件种类 participantTerastallized。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的完整回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是完成太晶化的成员稳定引用。
	Member MemberRef `json:"member"`
	// ElementID 是成员被替换成的唯一太晶属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
}

// Kind 返回 participantTerastallized。
func (event ParticipantTerastallizedEvent) Kind() EventKind { return event.Type }

// applyTerastallization 在一次技能真正开始结算前消耗阵营机会并将成员改为单一太晶属性。
//
// 命令校验已经验证该请求的赛制许可和全部一次性前提；此函数仍以防御式检查保持纯引擎状态转换的封闭性。
// 太晶化只覆盖当前属性，NaturalElementIDs 继续保存形态或变身的自然基线，因此后续形态变化不会错误解除太晶。
func applyTerastallization(state State, actorRef SlotRef) (State, []Event) {
	if !state.rules.TerastallizationEnabled {
		return state, nil
	}
	actor, found := state.ActiveMember(actorRef)
	if !found || actor.CurrentHP == 0 || actor.Terastallized || !actor.TeraElementID.IsValid() ||
		state.terastallizationUsed(actorRef.Side) {
		return state, nil
	}
	if len(actor.NaturalElementIDs) == 0 {
		actor.NaturalElementIDs = append([]Identifier(nil), actor.ElementIDs...)
	}
	actor.ElementIDs = []Identifier{actor.TeraElementID}
	actor.Terastallized = true
	// 太晶属性覆盖携带道具属性身份。保留道具基线以保证对局状态可审计；后续形态变化会更新自然基线，
	// 而 restoreHeldItemElementIdentity 会因已太晶化继续保留该唯一属性。
	state.replaceMember(actorRef.Side, actor)
	state.markTerastallizationUsed(actorRef.Side)
	events := []Event{ParticipantTerastallizedEvent{
		Type: EventKindParticipantTerastallized, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: MemberRef{Side: actorRef.Side, Position: actor.Position}, ElementID: actor.TeraElementID,
	}}
	state, statStageEvents := applyTerastallizationStatStageChange(state, actorRef)
	events = append(events, statStageEvents...)
	state, environmentEvents := applyTerastallizationEnvironmentClear(state, actorRef)
	events = append(events, environmentEvents...)
	return state, events
}

// applyTerastallizationStatStageChange 执行太晶化特性声明的自身能力阶级变化，并仅记录实际写入的差值。
func applyTerastallizationStatStageChange(state State, actorRef SlotRef) (State, []Event) {
	member, found := state.ActiveMember(actorRef)
	if !found || member.TerastallizationStatStageChange == nil {
		return state, nil
	}
	rule := member.TerastallizationStatStageChange
	before := member.StatStages[rule.Stat]
	after := max(int8(-6), min(int8(6), before+rule.StageDelta))
	if after == before {
		return state, nil
	}
	member.StatStages = cloneStatStages(member.StatStages)
	member.StatStages[rule.Stat] = after
	state.replaceMember(actorRef.Side, member)
	memberRef := MemberRef{Side: actorRef.Side, Position: member.Position}
	return state, []Event{StatStageChangedEvent{
		Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: memberRef, Target: memberRef, Stat: rule.Stat, Delta: after - before, CurrentStage: after,
	}}
}

// applyTerastallizationEnvironmentClear 清除太晶化特性覆盖范围内的普通天气与普通场地，并同步天气形态。
//
// 该规则复用既有 weatherEnded 与 terrainEnded 公开事件，而不引入含糊的“通用环境清除”事件。强天气不在
// 清除范围内；天气形态只读取有效普通天气，因此普通天气被移除后必须立即同步回默认形态。
func applyTerastallizationEnvironmentClear(state State, actorRef SlotRef) (State, []Event) {
	member, found := state.ActiveMember(actorRef)
	if !found || !member.TerastallizationEnvironmentClear {
		return state, nil
	}
	events := make([]Event, 0, 3)
	if state.environment.Weather != nil {
		weather := state.environment.Weather.Kind
		state.environment.Weather = nil
		events = append(events, WeatherEndedEvent{
			Type: EventKindWeatherEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Weather: weather,
		})
	}
	if state.environment.Terrain != nil {
		terrain := state.environment.Terrain.Kind
		state.environment.Terrain = nil
		events = append(events, TerrainEndedEvent{
			Type: EventKindTerrainEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, Terrain: terrain,
		})
	}
	if len(events) == 0 {
		return state, nil
	}
	state, weatherFormEvents := synchronizeWeatherForms(state)
	return state, append(events, weatherFormEvents...)
}

// terastallizationUsed 返回指定阵营是否已经消耗本局唯一的太晶化机会。
func (state State) terastallizationUsed(sideID Side) bool {
	for _, side := range state.sides {
		if side.Side == sideID {
			return side.TerastallizationUsed
		}
	}
	return false
}

// markTerastallizationUsed 只修改对应阵营的机会状态。
func (state *State) markTerastallizationUsed(sideID Side) {
	for index := range state.sides {
		if state.sides[index].Side == sideID {
			state.sides[index].TerastallizationUsed = true
			return
		}
	}
}
