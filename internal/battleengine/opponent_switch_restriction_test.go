package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestOpponentSwitchRestrictionPreventsOnlyMatchingVoluntarySwitches 验证对手限制只拒绝满足条件的主动换人。
//
// 倒下补位和技能、道具造成的强制换人由其它生命周期处理；本测试只覆盖回合命令边界，并同时固定接地、属性、
// 同类规则免疫和持有道具豁免的优先级。
func TestOpponentSwitchRestrictionPreventsOnlyMatchingVoluntarySwitches(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name          string
		configure     func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot)
		wantPrevented bool
	}{
		{
			name: "任意目标限制主动换人",
			configure: func(target, source *battleengine.MemberSnapshot) {
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{}
			},
			wantPrevented: true,
		},
		{
			name: "持有道具豁免限制",
			configure: func(target, source *battleengine.MemberSnapshot) {
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{}
				target.SwitchRestrictionImmunity = true
			},
		},
		{
			name: "未接地目标不受接地限制",
			configure: func(target, source *battleengine.MemberSnapshot) {
				target.ElementIDs = testIDs("flying-element")
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{RequiresGroundedTarget: true}
			},
		},
		{
			name: "完全相同的规则提供免疫",
			configure: func(target, source *battleengine.MemberSnapshot) {
				target.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{SameEffectGrantsImmunity: true}
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{SameEffectGrantsImmunity: true}
			},
		},
		{
			name: "不同规则不提供同类免疫",
			configure: func(target, source *battleengine.MemberSnapshot) {
				target.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{}
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{SameEffectGrantsImmunity: true}
			},
			wantPrevented: true,
		},
		{
			name: "属性不匹配不限制",
			configure: func(target, source *battleengine.MemberSnapshot) {
				source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{RequiredTargetElementID: testID("water-element")}
			},
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			target := newMember(1, "switch-restriction-target-"+test.name, 500, 500)
			reserve := newMember(2, "switch-restriction-reserve-"+test.name, 500, 500)
			source := newMember(1, "switch-restriction-source-"+test.name, 500, 500)
			source.Stats.Speed = 99
			test.configure(&target, &source)
			state, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "opponent-switch-restriction", ActiveSlotsPerSide: 1, TeamSize: 2},
				Rules: battleengine.RuleSnapshot{
					SchemaVersion: 1,
					ElementIDs:    map[string]Identifier{"flying": testID("flying-element")},
				},
				Sides: []battleengine.SideSnapshot{
					{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target, reserve}},
					{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
				},
			})
			if err != nil {
				t.Fatalf("NewState() error = %v", err)
			}

			_, err = battleengine.ResolveTurn(state, battleengine.TurnCommand{
				SchemaVersion: 1,
				TurnNumber:    1,
				Actions: []battleengine.Action{
					{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
					useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
				},
			}, mustRandom(t, 73))
			var commandError *battleengine.TurnCommandError
			prevented := errors.As(err, &commandError) && commandError.Code == battleengine.TurnCommandErrorSwitchPrevented
			if prevented != test.wantPrevented {
				t.Fatalf("ResolveTurn() error = %v, switch prevented = %t, want %t", err, prevented, test.wantPrevented)
			}
			if !test.wantPrevented && err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
		})
	}
}
