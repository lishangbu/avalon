package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInCopyOpponentAbility 验证初始入场复制存活对手的特性身份和已冻结规则。
func TestInitialStateAppliesSwitchInCopyOpponentAbility(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "switch-in-copy-ability-receiver", 1_000, 1_000)
	receiver.AbilityID = testID("trace-ability")
	receiver.SwitchInCopyOpponentAbility = true
	source := newMember(1, "switch-in-copy-ability-source", 1_000, 1_000)
	source.AbilityID = testID("weather-lock-ability")
	source.WeatherEffectsSuppressed = true
	source.CriticalHitImmunity = true
	source.SkillRecoilDamageImmunity = true
	source.IndirectDamageImmunity = true
	source.ContactDamageToAttackerDenominator = 8
	source.IgnoreOpponentDamageStatStages = true
	source.IgnoreTargetAbilityEffects = true
	source.SurviveFatalDamageAtFullHP = true
	source.OpponentStatusSkillImmunity = true
	source.NonSuperEffectiveDamageImmunity = true
	source.ContactSkillProtectionBypass = true
	source.ContactSuppression = true
	source.ReceivedContactDamageHalved = true
	source.ReceivedFireDamageDoubled = true
	source.WeatherDamageImmunities = []battleengine.WeatherKind{battleengine.WeatherKindSandstorm}
	source.SwitchInAllyHeal = &battleengine.SwitchInAllyHeal{HealDenominator: 3}
	source.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{
		RequiredTargetElementID: testID("switch-restriction-element"), RequiresGroundedTarget: true, SameEffectGrantsImmunity: true,
	}
	source.SwitchInRevealOpponentHeldItems = true
	source.SwitchInRevealOpponentHighestPowerSkill = true
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-copy-ability-initial", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	copied, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found {
		t.Fatal("初始上场复制者不存在")
	}
	if copied.AbilityID != source.AbilityID || !copied.WeatherEffectsSuppressed ||
		!copied.CriticalHitImmunity || !copied.SkillRecoilDamageImmunity || !copied.IndirectDamageImmunity || copied.ContactDamageToAttackerDenominator != 8 || !copied.IgnoreOpponentDamageStatStages || !copied.IgnoreTargetAbilityEffects || !copied.SurviveFatalDamageAtFullHP || !copied.OpponentStatusSkillImmunity || !copied.NonSuperEffectiveDamageImmunity || !copied.ContactSkillProtectionBypass || !copied.ContactSuppression || !copied.ReceivedContactDamageHalved || !copied.ReceivedFireDamageDoubled ||
		len(copied.WeatherDamageImmunities) != 1 || copied.WeatherDamageImmunities[0] != battleengine.WeatherKindSandstorm ||
		copied.SwitchInAllyHeal == nil || copied.SwitchInAllyHeal.HealDenominator != 3 || copied.SwitchInCopyOpponentAbility ||
		copied.OpponentSwitchRestriction == nil || copied.OpponentSwitchRestriction.RequiredTargetElementID != testID("switch-restriction-element") ||
		!copied.OpponentSwitchRestriction.RequiresGroundedTarget || !copied.OpponentSwitchRestriction.SameEffectGrantsImmunity ||
		!copied.SwitchInRevealOpponentHeldItems || !copied.SwitchInRevealOpponentHighestPowerSkill {
		t.Fatalf("初始入场特性复制结果不完整: %+v", copied)
	}
	source.OpponentSwitchRestriction.RequiredTargetElementID = testID("changed-after-state-creation")
	copied, found = state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || copied.OpponentSwitchRestriction == nil || copied.OpponentSwitchRestriction.RequiredTargetElementID != testID("switch-restriction-element") {
		t.Fatalf("复制特性错误共享主动换人限制规则: %+v", copied.OpponentSwitchRestriction)
	}
}

// TestSwitchInCopyOpponentAbilityDoesNotBorrowSourceFormProfiles 验证复制特性不会把来源成员按其培养值冻结的形态画像
// 挪给接收者。接收者没有完整画像时必须明确忽略形态规则，而不是在未来离场或换入时静默找不到目标。
func TestSwitchInCopyOpponentAbilityDoesNotBorrowSourceFormProfiles(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "copy-form-receiver", 1_000, 1_000)
	receiver.AbilityID = testID("trace-ability")
	receiver.SwitchInCopyOpponentAbility = true
	receiver.FormProfiles = []battleengine.FormProfile{formProfile(receiver)}

	source := newMember(1, "copy-form-source-base", 1_000, 1_000)
	source.AbilityID = testID("form-ability")
	source.FormProfiles = []battleengine.FormProfile{
		formProfile(source),
		{CreatureID: testID("copy-form-source-alternate"), MaxHP: 1_200, Stats: source.Stats, Weight: 200, ElementIDs: testIDs("fire")},
	}
	source.SwitchInFormChange = &battleengine.SwitchInFormChange{
		BaseCreatureID: source.CreatureID, AlternateCreatureID: testID("copy-form-source-alternate"),
	}
	source.SwitchOutFormChange = &battleengine.SwitchOutFormChange{
		BaseCreatureID: testID("copy-form-source-alternate"), AlternateCreatureID: source.CreatureID,
	}
	source.WeatherFormChange = &battleengine.WeatherFormChange{
		DefaultCreatureID: source.CreatureID,
		Targets: []battleengine.WeatherFormTarget{{
			Weather: battleengine.WeatherKindSun, CreatureID: testID("copy-form-source-alternate"),
		}},
	}

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "copy-form-profile-isolation", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	copied, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || copied.AbilityID != source.AbilityID || copied.SwitchInFormChange != nil ||
		copied.SwitchOutFormChange != nil || copied.WeatherFormChange != nil ||
		len(copied.FormProfiles) != 1 || copied.FormProfiles[0].CreatureID != receiver.CreatureID {
		t.Fatalf("复制特性错误借用了来源形态画像: %+v", copied)
	}
}

// TestResolveTurnSwitchInCopyOpponentAbilityPublishesEvent 验证实际换入按稳定对手槽位选择来源并发布结构化复制事件。
func TestResolveTurnSwitchInCopyOpponentAbilityPublishesEvent(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-copy-ability-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-copy-ability-incoming", 1_000, 1_000)
	incoming.AbilityID = testID("trace-ability")
	incoming.SwitchInCopyOpponentAbility = true
	firstSource := newMember(1, "switch-in-copy-ability-first-source", 1_000, 1_000)
	firstSource.AbilityID = testID("first-source-ability")
	firstSource.WeatherEffectsSuppressed = true
	secondSource := newMember(2, "switch-in-copy-ability-second-source", 1_000, 1_000)
	secondSource.AbilityID = testID("second-source-ability")
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-copy-ability-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{firstSource, secondSource}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 281))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var copiedEvent *battleengine.AbilityCopiedEvent
	for _, event := range result.Events {
		value, ok := event.(battleengine.AbilityCopiedEvent)
		if ok {
			copiedEvent = &value
			break
		}
	}
	if copiedEvent == nil || copiedEvent.Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) ||
		copiedEvent.Source != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) ||
		copiedEvent.PreviousAbilityID != testID("trace-ability") || copiedEvent.AbilityID != firstSource.AbilityID {
		t.Fatalf("换入特性复制事件不正确: %+v", result.Events)
	}
	copied, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || copied.AbilityID != firstSource.AbilityID || !copied.WeatherEffectsSuppressed {
		t.Fatalf("换入后未复制来源特性规则: %+v", copied)
	}
}
