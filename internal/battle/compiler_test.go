package battle_test

import (
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/team"
)

func TestCompileInitialStatePreservesSelectedTeamPositions(t *testing.T) {
	t.Parallel()
	session := startingSession()
	initial, err := battle.CompileInitialState(session, initialFacts())
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if len(initial.Sides) != 2 || len(initial.Sides[0].Members) != 2 || initial.Sides[0].Members[1].Position != 3 {
		t.Fatalf("CompileInitialState() = %+v, want original selected positions", initial)
	}
	if initial.Sides[0].ActiveMembers[0] != 3 || initial.Sides[1].ActiveMembers[0] != 1 {
		t.Fatalf("CompileInitialState() active members = %+v", initial.Sides)
	}
	if _, err := battleengine.NewState(initial); err != nil {
		t.Fatalf("compiled initial state must be accepted by engine: %v", err)
	}
}

// TestCompileInitialStateFreezesMemberTeraElement 验证 Team 选择的太晶属性会随成员事实进入引擎快照，而不是在回合请求中读取。
func TestCompileInitialStateFreezesMemberTeraElement(t *testing.T) {
	t.Parallel()
	facts := initialFacts()
	facts.Rules.TerastallizationEnabled = true
	teraElementID := snowflake.NewTestID()
	facts.Sides[0].Members[2].TeraElementID = teraElementID
	initial, err := battle.CompileInitialState(startingSession(), facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	member := initial.Sides[0].Members[1]
	if member.Position != 3 || member.TeraElementID != teraElementID || len(member.NaturalElementIDs) != 1 || member.NaturalElementIDs[0] != member.ElementIDs[0] {
		t.Fatalf("冻结成员 = %+v，期望太晶属性及自然属性基线均保留", member)
	}
}

// TestCompileInitialStateFreezesAbilityAccuracyRules 验证特性命中规则在 Battle 启动时深拷贝到引擎快照。
//
// 对局开始后即使资料管理端改写特性详情，已创建对局也只能使用这里冻结的整数分数和开关，不能重新读取实时资料。
func TestCompileInitialStateFreezesAbilityAccuracyRules(t *testing.T) {
	t.Parallel()
	facts := initialFacts()
	member := &facts.Sides[0].Members[0]
	member.AccuracyMultiplier = &battleengine.AccuracyMultiplier{Numerator: 13, Denominator: 10}
	member.PhysicalSkillAccuracyMultiplier = &battleengine.AccuracyMultiplier{Numerator: 4, Denominator: 5}
	member.OpponentAccuracySandstormMultiplier = &battleengine.AccuracyMultiplier{Numerator: 4, Denominator: 5}
	member.OpponentAccuracySnowMultiplier = &battleengine.AccuracyMultiplier{Numerator: 4, Denominator: 5}
	member.OpponentAccuracyConfusionMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
	member.AccuracyAlwaysHits = true
	member.StatusSkillAccuracyCap = 50
	member.IgnoreOpponentAccuracyStatStages = true
	member.CriticalHitImmunity = true
	member.SkillRecoilDamageImmunity = true
	member.IndirectDamageImmunity = true
	member.ContactDamageToAttackerDenominator = 8
	member.HeldItemContactDamageToAttackerDenominator = 6
	member.HeldItemEndTurnHealDenominator = 16
	member.HeldItemEndTurnDamageDenominator = 8
	member.ContactTransferToAttacker = true
	member.ChargeSkipOnce = true
	member.HeldItemSurviveFatalDamageAtFullHP = true
	member.HeldItemReflectTurnsRemaining = 8
	member.HeldItemLightScreenTurnsRemaining = 8
	member.HeldItemAuroraVeilTurnsRemaining = 8
	member.HeldItemRainTurnsRemaining = 8
	member.HeldItemSandstormTurnsRemaining = 8
	member.HeldItemSnowTurnsRemaining = 8
	member.HeldItemSunTurnsRemaining = 8
	member.HeldItemTerrainTurnsRemaining = 8
	member.HeldItemSandstormDamageImmunity = true
	member.HeldItemWeightHalf = true
	member.HeldItemCuresParalysis = true
	member.HeldItemCuresSleep = true
	member.HeldItemCuresPoison = true
	member.HeldItemCuresBurn = true
	member.HeldItemCuresFreeze = true
	member.HeldItemCuresAllMajorStatuses = true
	member.HeldItemCuresConfusion = true
	member.HeldItemPunchBasedSkillPowerBoost = true
	member.IgnoreOpponentDamageStatStages = true
	member.IgnoreTargetAbilityEffects = true
	member.SurviveFatalDamageAtFullHP = true
	member.OpponentStatusSkillImmunity = true
	member.NonSuperEffectiveDamageImmunity = true
	member.ContactSkillProtectionBypass = true
	member.ContactSuppression = true
	member.ReceivedContactDamageHalved = true
	member.ReceivedFireDamageDoubled = true
	member.Skills[0].MakesContact = true
	member.Skills[0].PunchBased = true
	member.Skills[0].PowderBased = true

	initial, err := battle.CompileInitialState(startingSession(), facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0]
	if frozen.AccuracyMultiplier == nil || frozen.AccuracyMultiplier.Numerator != 13 ||
		frozen.PhysicalSkillAccuracyMultiplier == nil || frozen.OpponentAccuracySandstormMultiplier == nil ||
		frozen.OpponentAccuracySnowMultiplier == nil || frozen.OpponentAccuracyConfusionMultiplier == nil ||
		!frozen.AccuracyAlwaysHits || frozen.StatusSkillAccuracyCap != 50 || !frozen.IgnoreOpponentAccuracyStatStages ||
		!frozen.CriticalHitImmunity || !frozen.SkillRecoilDamageImmunity || !frozen.IndirectDamageImmunity || frozen.ContactDamageToAttackerDenominator != 8 || frozen.HeldItemContactDamageToAttackerDenominator != 6 || frozen.HeldItemEndTurnHealDenominator != 16 || frozen.HeldItemEndTurnDamageDenominator != 8 || !frozen.ContactTransferToAttacker || !frozen.ChargeSkipOnce || !frozen.HeldItemSurviveFatalDamageAtFullHP || frozen.HeldItemReflectTurnsRemaining != 8 || frozen.HeldItemLightScreenTurnsRemaining != 8 || frozen.HeldItemAuroraVeilTurnsRemaining != 8 || frozen.HeldItemRainTurnsRemaining != 8 || frozen.HeldItemSandstormTurnsRemaining != 8 || frozen.HeldItemSnowTurnsRemaining != 8 || frozen.HeldItemSunTurnsRemaining != 8 || frozen.HeldItemTerrainTurnsRemaining != 8 || !frozen.HeldItemSandstormDamageImmunity || !frozen.HeldItemWeightHalf || !frozen.HeldItemCuresParalysis || !frozen.HeldItemCuresSleep || !frozen.HeldItemCuresPoison || !frozen.HeldItemCuresBurn || !frozen.HeldItemCuresFreeze || !frozen.HeldItemCuresAllMajorStatuses || !frozen.HeldItemCuresConfusion || !frozen.HeldItemPunchBasedSkillPowerBoost || !frozen.IgnoreOpponentDamageStatStages ||
		!frozen.IgnoreTargetAbilityEffects || !frozen.SurviveFatalDamageAtFullHP || !frozen.OpponentStatusSkillImmunity || !frozen.NonSuperEffectiveDamageImmunity ||
		!frozen.ContactSkillProtectionBypass || !frozen.ContactSuppression || !frozen.ReceivedContactDamageHalved || !frozen.ReceivedFireDamageDoubled || len(frozen.Skills) != 1 || !frozen.Skills[0].MakesContact || !frozen.Skills[0].PunchBased || !frozen.Skills[0].PowderBased {
		t.Fatalf("冻结的命中特性规则 = %+v", frozen)
	}
	member.AccuracyMultiplier.Numerator = 1
	if frozen.AccuracyMultiplier.Numerator != 13 {
		t.Fatalf("Battle 初始状态与输入事实共享命中倍率指针: frozen=%+v input=%+v", frozen.AccuracyMultiplier, member.AccuracyMultiplier)
	}
}

func TestCompileInitialStateRejectsMissingSelectedFacts(t *testing.T) {
	t.Parallel()
	facts := initialFacts()
	facts.Sides[0].Members = facts.Sides[0].Members[:1]
	_, err := battle.CompileInitialState(startingSession(), facts)
	if !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("CompileInitialState() error = %v, want ErrInitialStateCompilation", err)
	}
}

func startingSession() battle.Battle {
	now := time.Date(2026, time.July, 30, 0, 0, 0, 0, time.UTC)
	return battle.Battle{
		ID: snowflake.NewTestID(), Status: battle.StatusRunning,
		Format: battle.Format{
			RosterCount: 3, SelectCount: 2, ActiveParticipantsPerSide: 1,
			PreviewDuration: time.Minute, BattleDuration: 5 * time.Minute,
		},
		Participants: []battle.Participant{
			{Side: battle.ParticipantSideOne, Team: battle.TeamSnapshot{Members: fixtureTeamMembers()}},
			{Side: battle.ParticipantSideTwo, Team: battle.TeamSnapshot{Members: fixtureTeamMembers()}},
		},
		PreviewSubmissions: []battle.PreviewSubmission{
			{Side: battle.ParticipantSideOne, MemberPositions: []int32{1, 3}, ActivePositions: []int32{3}, SubmittedAt: now},
			{Side: battle.ParticipantSideTwo, MemberPositions: []int32{1, 3}, ActivePositions: []int32{1}, SubmittedAt: now},
		},
	}
}

func fixtureTeamMembers() []team.Member {
	return []team.Member{{Position: 1}, {Position: 2}, {Position: 3}}
}

func initialFacts() battle.InitialStateFacts {
	return battle.InitialStateFacts{
		Format: battleengine.FormatSnapshot{Code: "test-single", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battle.BattleSideFacts{
			{Side: battle.ParticipantSideOne, Members: fixtureBattleMemberFacts()},
			{Side: battle.ParticipantSideTwo, Members: fixtureBattleMemberFacts()},
		},
	}
}

func fixtureBattleMemberFacts() []battle.BattleMemberFacts {
	return []battle.BattleMemberFacts{
		fixtureBattleMemberFactsAt(1), fixtureBattleMemberFactsAt(2), fixtureBattleMemberFactsAt(3),
	}
}

func fixtureBattleMemberFactsAt(position battleengine.MemberPosition) battle.BattleMemberFacts {
	return battle.BattleMemberFacts{
		Position: position, CreatureID: snowflake.NewTestID(), Level: 50, MaxHP: 100,
		Stats:      battleengine.StatBlock{Attack: 100, Defense: 100, SpecialAttack: 100, SpecialDefense: 100, Speed: 100},
		ElementIDs: []battleengine.Identifier{snowflake.NewTestID()},
		Skills: []battleengine.SkillSnapshot{{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: snowflake.NewTestID(), Name: "测试技能", ElementID: snowflake.NewTestID(),
			DamageClass: battleengine.DamageClassPhysical, Power: 40, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		}},
	}
}
