package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnStartsAndExpiresTerrain 验证场地建立后在回合末递减，并在最后一个持续回合自然结束。
func TestResolveTurnStartsAndExpiresTerrain(t *testing.T) {
	t.Parallel()

	setter := newMember(1, "terrain-setter", 500, 500)
	setter.Stats.Speed = 200
	setter.Skills[0] = terrainSkill(1, battleengine.TerrainKindElectric, 2)
	other := newMember(1, "terrain-observer", 500, 500)
	other.Skills[0].Power = 1
	state := newTerrainState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, other)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 11)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	started, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if effect := started.State.Snapshot().Environment.Terrain; effect == nil || effect.Kind != battleengine.TerrainKindElectric || effect.TurnsRemaining != 1 {
		t.Fatalf("场地建立后状态 = %+v", effect)
	}
	if !containsTerrainEvent(started.Events, battleengine.EventKindTerrainStarted) {
		t.Fatalf("缺少场地建立事件: %v", started.Events)
	}

	expired, err := battleengine.ResolveTurn(started.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), started.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() 场地过期 error = %v", err)
	}
	if effect := expired.State.Snapshot().Environment.Terrain; effect != nil {
		t.Fatalf("场地过期后状态 = %+v", effect)
	}
	if !containsTerrainEvent(expired.Events, battleengine.EventKindTerrainEnded) {
		t.Fatalf("缺少场地结束事件: %v", expired.Events)
	}
}

// TestResolveTurnGrassyTerrainHealsGroundedOnly 验证青草场地只在回合末回复接地的场上成员，不会回复飞行属性成员。
func TestResolveTurnGrassyTerrainHealsGroundedOnly(t *testing.T) {
	t.Parallel()

	grounded := newMember(1, "grassy-grounded", 160, 100)
	grounded.Stats.Speed = 200
	flying := newMember(1, "grassy-flying", 160, 100)
	flying.ElementIDs = testIDs("flying")
	grounded.Skills[0].Power = 1
	flying.Skills[0].Power = 1
	state := newTerrainState(t,
		battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"flying": testID("flying")}},
		grounded,
		flying,
	)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 12)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var healed []battleengine.MemberRef
	for _, event := range result.Events {
		if healing, ok := event.(battleengine.TerrainHealingAppliedEvent); ok {
			healed = append(healed, healing.Target)
		}
	}
	if len(healed) != 1 || healed[0] != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) {
		t.Fatalf("青草场地回复目标 = %+v，期望仅第一方接地成员", healed)
	}
}

// TestResolveTurnTerrainAdjustsDamage 验证三种强化场地只强化接地使用者的对应属性技能，薄雾只削弱针对接地目标的
// 龙属性伤害；场地不会退化为对所有成员的无条件倍率。
func TestResolveTurnTerrainAdjustsDamage(t *testing.T) {
	t.Parallel()

	noTerrainElectric := resolveEnvironmentDamage(t, battleengine.EnvironmentSnapshot{}, testID("electric"), battleengine.DamageClassSpecial, testID("normal"))
	electricTerrain := resolveEnvironmentDamage(t, battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 2}}, testID("electric"), battleengine.DamageClassSpecial, testID("normal"))
	if electricTerrain <= noTerrainElectric {
		t.Fatalf("电气场地电属性伤害 = %d，常规伤害 = %d；期望电气场地强化接地使用者", electricTerrain, noTerrainElectric)
	}
	noTerrainDragon := resolveEnvironmentDamage(t, battleengine.EnvironmentSnapshot{}, testID("dragon"), battleengine.DamageClassSpecial, testID("normal"))
	mistyDragon := resolveEnvironmentDamage(t, battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindMisty, TurnsRemaining: 2}}, testID("dragon"), battleengine.DamageClassSpecial, testID("normal"))
	if mistyDragon >= noTerrainDragon {
		t.Fatalf("薄雾场地龙属性伤害 = %d，常规伤害 = %d；期望薄雾削弱针对接地目标的龙属性伤害", mistyDragon, noTerrainDragon)
	}
	noTerrainAirborne := resolveEnvironmentDamageWithElements(t,
		battleengine.EnvironmentSnapshot{},
		testID("electric"),
		battleengine.DamageClassSpecial,
		testID("normal"),
		testIDs("flying"),
	)
	airborneElectric := resolveEnvironmentDamageWithElements(t,
		battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 2}},
		testID("electric"),
		battleengine.DamageClassSpecial,
		testID("normal"),
		testIDs("flying"),
	)
	if airborneElectric != noTerrainAirborne {
		t.Fatalf("非接地使用者电气场地伤害 = %d，常规伤害 = %d；期望非接地成员不获得场地强化", airborneElectric, noTerrainAirborne)
	}
}

// TestResolveTurnGrassyTerrainWeakensTaggedSkills 验证青草场地只会削弱带有专用标记、且命中接地目标的震动类
// 技能；同一标记不能把非接地目标误判为接地，也必须与青草属性强化在同一最终倍率阶段相乘。
func TestResolveTurnGrassyTerrainWeakensTaggedSkills(t *testing.T) {
	t.Parallel()

	grassyTerrain := battleengine.EnvironmentSnapshot{
		Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 2},
	}
	noTerrainGrounded := resolveEnvironmentDamageWithSkillProperties(
		t, battleengine.EnvironmentSnapshot{}, testID("ground"), battleengine.DamageClassPhysical,
		testIDs("ground"), testIDs("normal"), true,
	)
	grassyGrounded := resolveEnvironmentDamageWithSkillProperties(
		t, grassyTerrain, testID("ground"), battleengine.DamageClassPhysical,
		testIDs("ground"), testIDs("normal"), true,
	)
	if grassyGrounded != noTerrainGrounded/2 {
		t.Fatalf("青草场地接地目标震动技能伤害 = %d，常规伤害 = %d；期望最终伤害减半", grassyGrounded, noTerrainGrounded)
	}

	noTerrainAirborne := resolveEnvironmentDamageWithSkillProperties(
		t, battleengine.EnvironmentSnapshot{}, testID("ground"), battleengine.DamageClassPhysical,
		testIDs("ground"), testIDs("flying"), true,
	)
	grassyAirborne := resolveEnvironmentDamageWithSkillProperties(
		t, grassyTerrain, testID("ground"), battleengine.DamageClassPhysical,
		testIDs("ground"), testIDs("flying"), true,
	)
	if grassyAirborne != noTerrainAirborne {
		t.Fatalf("青草场地非接地目标震动技能伤害 = %d，常规伤害 = %d；期望不受减伤", grassyAirborne, noTerrainAirborne)
	}

	noTerrainGrass := resolveEnvironmentDamageWithSkillProperties(
		t, battleengine.EnvironmentSnapshot{}, testID("grass"), battleengine.DamageClassSpecial,
		testIDs("grass"), testIDs("normal"), true,
	)
	grassyGrass := resolveEnvironmentDamageWithSkillProperties(
		t, grassyTerrain, testID("grass"), battleengine.DamageClassSpecial,
		testIDs("grass"), testIDs("normal"), true,
	)
	if grassyGrass != noTerrainGrass*13/20 {
		t.Fatalf("青草场地同属性强化与震动减伤后的伤害 = %d，常规伤害 = %d；期望倍率 1.3 × 0.5", grassyGrass, noTerrainGrass)
	}
}

// TestResolveTurnTerrainBlocksMajorStatuses 验证电气场地只阻止接地目标的睡眠，薄雾场地阻止接地目标的所有主要
// 异常；事件必须保留场地免疫原因，不能退化为普通未命中。
func TestResolveTurnTerrainBlocksMajorStatuses(t *testing.T) {
	t.Parallel()

	for _, test := range []struct {
		name    string
		terrain battleengine.TerrainKind
		status  battleengine.MajorStatus
	}{
		{name: "电气场地阻止睡眠", terrain: battleengine.TerrainKindElectric, status: battleengine.MajorStatusSleep},
		{name: "薄雾场地阻止灼伤", terrain: battleengine.TerrainKindMisty, status: battleengine.MajorStatusBurn},
	} {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			attacker := newMember(1, "terrain-status-attacker", 300, 300)
			attacker.Stats.Speed = 200
			attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("terrain-status-skill"), Name: "场地异常", ElementID: testID("normal"),
				DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
				Accuracy: 100, RemainingPP: 5, MaxPP: 5,
				StatusApplications: []battleengine.MajorStatusApplication{{
					Status: test.status, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
				}},
			}
			defender := newMember(1, "terrain-status-defender", 300, 300)
			defender.Skills[0].Power = 1
			state := newTerrainState(t,
				battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: test.terrain, TurnsRemaining: 2}},
				battleengine.RuleSnapshot{SchemaVersion: 1},
				attacker,
				defender,
			)
			random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 13)
			if err != nil {
				t.Fatalf("NewRandomSource() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
				fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
				fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
			), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			for _, event := range result.Events {
				if blocked, ok := event.(battleengine.MajorStatusBlockedEvent); ok &&
					blocked.Target == (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) &&
					blocked.Status == test.status && blocked.Reason == battleengine.MajorStatusBlockReasonTerrainImmunity {
					return
				}
			}
			t.Fatalf("缺少场地主要异常免疫事件: %+v", result.Events)
		})
	}
}

// TestResolveTurnPsychicTerrainBlocksPrioritySkill 验证精神场地会在命中、要害与伤害随机数之前阻止正优先度技能
// 影响接地对手，同时保留已使用技能和明确失败原因。
func TestResolveTurnPsychicTerrainBlocksPrioritySkill(t *testing.T) {
	t.Parallel()

	attacker := newMember(1, "psychic-terrain-attacker", 300, 300)
	attacker.Stats.Speed = 200
	attacker.Skills[0].SkillID = testID("psychic-terrain-priority")
	attacker.Skills[0].Priority = 1
	defender := newMember(1, "psychic-terrain-defender", 300, 300)
	defender.Skills[0].Power = 1
	state := newTerrainState(t,
		battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindPsychic, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1},
		attacker,
		defender,
	)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 14)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if failed, ok := event.(battleengine.SkillFailedEvent); ok && failed.SkillID == testID("psychic-terrain-priority") &&
			failed.Reason == battleengine.SkillFailureReasonPsychicTerrainTargetGrounded {
			return
		}
	}
	t.Fatalf("缺少精神场地阻断先制技能事件: %+v", result.Events)
}

// terrainSkill 创建可建立普通场地的自身范围变化技能。
func terrainSkill(position battleengine.SkillPosition, kind battleengine.TerrainKind, turns uint8) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("terrain-skill"), Name: "场地", ElementID: testID("terrain-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		TerrainApplication: &battleengine.TerrainApplication{
			Effect: battleengine.TerrainEffect{Kind: kind, TurnsRemaining: turns}, ChancePercent: 100,
		},
	}
}

// newTerrainState 使用带有场地依赖属性代码的独立规则快照创建单打状态。
func newTerrainState(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	rules battleengine.RuleSnapshot,
	first battleengine.MemberSnapshot,
	second battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "terrain", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       rules,
		Environment: environment,
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

// containsTerrainEvent 报告事件流是否包含指定的场地生命周期事件。
func containsTerrainEvent(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}
