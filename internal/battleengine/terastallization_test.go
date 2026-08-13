package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnTerastallizesBeforeSkillResolution 验证太晶化只能绑定技能行动，并在 PP 消耗与伤害计算前写入成员及阵营状态。
func TestResolveTurnTerastallizesBeforeSkillResolution(t *testing.T) {
	t.Parallel()
	first := newMember(1, "tera-first", 500, 500)
	first.TeraElementID = testID("tera-fire")
	first.ElementIDs = testIDs("normal")
	first.Skills[0].ElementID = testID("tera-fire")
	second := newMember(1, "tera-second", 500, 500)
	state := newTerastallizationState(t, first, second)

	firstAction := useSkillCommand(battleengine.SideOne, battleengine.SideTwo)
	firstAction.UseSkill.Terastallize = true
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{firstAction, useSkillCommand(battleengine.SideTwo, battleengine.SideOne)},
	}, mustRandom(t, 331))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || !member.Terastallized || member.TeraElementID != testID("tera-fire") || len(member.ElementIDs) != 1 || member.ElementIDs[0] != testID("tera-fire") {
		t.Fatalf("太晶化后的成员状态 = %+v，期望保留单一太晶属性", member)
	}
	if len(member.NaturalElementIDs) != 1 || member.NaturalElementIDs[0] != testID("normal") {
		t.Fatalf("太晶化后的自然属性基线 = %v，期望 [normal]", member.NaturalElementIDs)
	}
	if !hasParticipantTerastallized(result.Events, battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}, testID("tera-fire")) {
		t.Fatalf("回合事件未记录太晶化: %+v", result.Events)
	}
	if len(result.Events) < 3 || result.Events[1].Kind() != battleengine.EventKindParticipantTerastallized || result.Events[2].Kind() != battleengine.EventKindSkillUsed {
		t.Fatalf("太晶化与技能事件顺序 = %v，期望太晶化先于技能宣告", eventKinds(result.Events))
	}
	summary := result.State.Summary()
	if len(summary.Sides) != 2 || !summary.Sides[0].TerastallizationUsed || !summary.Members[0].Terastallized {
		t.Fatalf("太晶化状态摘要 = %+v，期望成员和阵营机会均可重放", summary)
	}
}

// TestResolveTurnRejectsInvalidTerastallizationRequests 验证赛制关闭、重复机会和缺少太晶属性都在引擎命令边界拒绝。
func TestResolveTurnRejectsInvalidTerastallizationRequests(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name  string
		state func(*testing.T) battleengine.State
		want  battleengine.TurnCommandErrorCode
	}{
		{
			name: "赛制未启用", want: battleengine.TurnCommandErrorTerastallizationDisabled,
			state: func(t *testing.T) battleengine.State {
				first, second := newMember(1, "tera-disabled-first", 500, 500), newMember(1, "tera-disabled-second", 500, 500)
				first.TeraElementID = testID("tera-fire")
				state := newTerastallizationState(t, first, second)
				snapshot := state.Snapshot()
				snapshot.Rules.TerastallizationEnabled = false
				updated, err := battleengine.NewState(battleengine.InitialState{
					Format: snapshot.Format, Rules: snapshot.Rules, Environment: snapshot.Environment, Sides: snapshot.Sides,
				})
				if err != nil {
					t.Fatalf("NewState() error = %v", err)
				}
				return updated
			},
		},
		{
			name: "本方机会已使用", want: battleengine.TurnCommandErrorTerastallizationAlreadyUsed,
			state: func(t *testing.T) battleengine.State {
				first, second := newMember(1, "tera-used-first", 500, 500), newMember(1, "tera-used-second", 500, 500)
				first.TeraElementID = testID("tera-fire")
				state := newTerastallizationState(t, first, second)
				snapshot := state.Snapshot()
				snapshot.Sides[0].TerastallizationUsed = true
				updated, err := battleengine.NewState(battleengine.InitialState{
					Format: snapshot.Format, Rules: snapshot.Rules, Environment: snapshot.Environment, Sides: snapshot.Sides,
				})
				if err != nil {
					t.Fatalf("NewState() error = %v", err)
				}
				return updated
			},
		},
		{
			name: "成员没有太晶属性", want: battleengine.TurnCommandErrorTeraElementUnavailable,
			state: func(t *testing.T) battleengine.State {
				return newTerastallizationState(t, newMember(1, "tera-missing-first", 500, 500), newMember(1, "tera-missing-second", 500, 500))
			},
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			state := test.state(t)
			firstAction := useSkillCommand(battleengine.SideOne, battleengine.SideTwo)
			firstAction.UseSkill.Terastallize = true
			_, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
				SchemaVersion: 1, TurnNumber: 1,
				Actions: []battleengine.Action{firstAction, useSkillCommand(battleengine.SideTwo, battleengine.SideOne)},
			}, mustRandom(t, 337))
			var commandError *battleengine.TurnCommandError
			if !errors.As(err, &commandError) || commandError.Code != test.want {
				t.Fatalf("ResolveTurn() error = %v，期望命令错误 %s", err, test.want)
			}
		})
	}
}

// TestResolveTurnAppliesTerastallizationAbilityRules 验证太晶化特性在太晶化事件之后依次写入自身能力阶级并清除普通环境。
func TestResolveTurnAppliesTerastallizationAbilityRules(t *testing.T) {
	t.Parallel()
	first := newMember(1, "tera-ability-first", 500, 500)
	first.TeraElementID = testID("tera-water")
	first.TerastallizationStatStageChange = &battleengine.TerastallizationStatStageChange{
		Stat: battleengine.StatSpeed, StageDelta: 1,
	}
	first.TerastallizationEnvironmentClear = true
	second := newMember(1, "tera-ability-second", 500, 500)
	state := newTerastallizationState(t, first, second)
	snapshot := state.Snapshot()
	snapshot.Environment = battleengine.EnvironmentSnapshot{
		Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 3},
		Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 3},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: snapshot.Format, Rules: snapshot.Rules, Environment: snapshot.Environment, Sides: snapshot.Sides,
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}

	firstAction := useSkillCommand(battleengine.SideOne, battleengine.SideTwo)
	firstAction.UseSkill.Terastallize = true
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{firstAction, useSkillCommand(battleengine.SideTwo, battleengine.SideOne)},
	}, mustRandom(t, 347))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if member.StatStages[battleengine.StatSpeed] != 1 {
		t.Fatalf("太晶化后速度能力阶级 = %d，期望 1", member.StatStages[battleengine.StatSpeed])
	}
	environment := result.State.Snapshot().Environment
	if environment.Weather != nil || environment.Terrain != nil {
		t.Fatalf("太晶化清场后的环境 = %+v，期望普通天气与场地均已清除", environment)
	}
	if !hasEventKind(result.Events, battleengine.EventKindStatStageChanged) || !hasEventKind(result.Events, battleengine.EventKindWeatherEnded) || !hasEventKind(result.Events, battleengine.EventKindTerrainEnded) {
		t.Fatalf("太晶化特性事件 = %v，期望能力变化、天气结束和场地结束事件", eventKinds(result.Events))
	}
}

// TestResolveTurnKeepsTeraElementDuringFormChange 验证太晶化后发生的天气形态切换只更新自然属性基线，不能覆盖当前太晶属性。
func TestResolveTurnKeepsTeraElementDuringFormChange(t *testing.T) {
	t.Parallel()
	first := newMember(1, "tera-form-base", 500, 500)
	first.Stats.Speed = 200
	first.ElementIDs = testIDs("base-element")
	first.TeraElementID = testID("tera-electric")
	first.FormProfiles = []battleengine.FormProfile{
		{CreatureID: testID("tera-form-base"), MaxHP: 500, Stats: first.Stats, Weight: 1, ElementIDs: testIDs("base-element")},
		{CreatureID: testID("tera-form-rain"), MaxHP: 500, Stats: first.Stats, Weight: 1, ElementIDs: testIDs("rain-element")},
	}
	first.WeatherFormChange = &battleengine.WeatherFormChange{
		DefaultCreatureID: testID("tera-form-base"),
		Targets:           []battleengine.WeatherFormTarget{{Weather: battleengine.WeatherKindRain, CreatureID: testID("tera-form-rain")}},
	}
	second := newMember(1, "tera-form-second", 500, 500)
	second.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 3)
	state := newTerastallizationState(t, first, second)

	firstAction := useSkillCommand(battleengine.SideOne, battleengine.SideTwo)
	firstAction.UseSkill.Terastallize = true
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{firstAction, {
			Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}},
		}},
	}, mustRandom(t, 349))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if member.CreatureID != testID("tera-form-rain") || len(member.NaturalElementIDs) != 1 || member.NaturalElementIDs[0] != testID("rain-element") {
		t.Fatalf("天气形态变化后的自然画像 = %+v，期望切换到雨天形态及其自然属性", member)
	}
	if !member.Terastallized || len(member.ElementIDs) != 1 || member.ElementIDs[0] != testID("tera-electric") {
		t.Fatalf("天气形态变化后的当前属性 = %v，期望继续保持太晶属性", member.ElementIDs)
	}
}

// newTerastallizationState 构造一场允许太晶化的最小单打状态，避免测试把赛制开关与资料读取实现混在一起。
func newTerastallizationState(t *testing.T, first, second battleengine.MemberSnapshot) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "tera-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, TerastallizationEnabled: true},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// hasParticipantTerastallized 在事件流中查找一条精确匹配的太晶化公开事件。
func hasParticipantTerastallized(events []battleengine.Event, member battleengine.MemberRef, elementID Identifier) bool {
	for _, event := range events {
		terastallized, ok := event.(battleengine.ParticipantTerastallizedEvent)
		if ok && terastallized.Member == member && terastallized.ElementID == elementID {
			return true
		}
	}
	return false
}

// hasEventKind 报告事件流是否包含一种稳定事件类型。
func hasEventKind(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}
