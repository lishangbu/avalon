package battleengine

import "testing"

// TestHeldItemAccuracyMissSpeedBoost 验证技能确因命中判定落空后，打空保险提升两级速度并消费。
func TestHeldItemAccuracyMissSpeedBoost(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.ItemID = testID("blunder-policy")
	actor.HeldItemAccuracyMissStatStageBoostStat, actor.HeldItemAccuracyMissStatStageBoostDelta = StatSpeed, 2
	state.replaceMember(SideOne, actor)

	state, events := applyHeldItemAccuracyMissStatStageBoost(state, MemberRef{Side: SideOne, Position: 1}, testID("missed-skill"))
	actor, _ = state.member(SideOne, 1)
	if actor.ItemID != 0 || actor.StatStages[StatSpeed] != 2 || len(events) != 2 || events[0].Kind() != EventKindStatStageChanged || events[1].Kind() != EventKindHeldItemTriggeredConsumed {
		t.Fatalf("打空保险结果 = %+v, events=%+v", actor, events)
	}
}

// TestHeldItemAccuracyMissSpeedBoostBoundaries 验证打空保险只响应仍可战斗成员的真实命中落空，且能力已满时保留道具。
func TestHeldItemAccuracyMissSpeedBoostBoundaries(t *testing.T) {
	for _, test := range []struct {
		name         string
		currentHP    uint32
		currentStage int8
	}{
		{name: "使用者已经倒下", currentHP: 0},
		{name: "速度已经达到上限", currentHP: 100, currentStage: 6},
	} {
		t.Run(test.name, func(t *testing.T) {
			state := heldItemAdvancedState(t)
			actor, _ := state.member(SideOne, 1)
			actor.CurrentHP = test.currentHP
			actor.ItemID = testID("blunder-policy")
			actor.HeldItemAccuracyMissStatStageBoostStat, actor.HeldItemAccuracyMissStatStageBoostDelta = StatSpeed, 2
			actor.StatStages[StatSpeed] = test.currentStage
			state.replaceMember(SideOne, actor)

			state, events := applyHeldItemAccuracyMissStatStageBoost(state, MemberRef{Side: SideOne, Position: 1}, testID("missed-skill"))
			actor, _ = state.member(SideOne, 1)
			if actor.ItemID != testID("blunder-policy") || actor.StatStages[StatSpeed] != test.currentStage || len(events) != 0 {
				t.Fatalf("边界结果 = actor=%+v events=%+v", actor, events)
			}
		})
	}
}

// TestHeldItemAccuracyMissEventOrder 验证完整技能链严格先记录落空，再记录实际阶级变化和道具消费。
func TestHeldItemAccuracyMissEventOrder(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.Stats.Speed = 200
	actor.ItemID = testID("blunder-policy")
	actor.HeldItemAccuracyMissStatStageBoostStat, actor.HeldItemAccuracyMissStatStageBoostDelta = StatSpeed, 2
	actor.Skills[0].Accuracy = 50
	state.replaceMember(SideOne, actor)
	target, _ := state.member(SideTwo, 1)
	target.FlinchedTurn = 1
	state.replaceMember(SideTwo, target)
	replay, err := NewTracedRandom([]RandomTraceEntry{{Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("first").String(), Value: 99}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error=%v", err)
	}
	result, err := ResolveTurn(state, heldItemAdvancedCommand(1, 1), replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error=%v", err)
	}
	missed, changed, consumed := -1, -1, -1
	for index, event := range result.Events {
		switch event.Kind() {
		case EventKindSkillMissed:
			missed = index
		case EventKindStatStageChanged:
			changed = index
		case EventKindHeldItemTriggeredConsumed:
			consumed = index
		}
	}
	if missed < 0 || changed <= missed || consumed <= changed {
		t.Fatalf("打空保险事件顺序 = %+v", result.Events)
	}
}

// TestHeldItemReceivedDamageStatBoosts 验证弱点保险、球根和充电电池只响应真实本体且匹配的伤害事实。
func TestHeldItemReceivedDamageStatBoosts(t *testing.T) {
	tests := []struct {
		name       string
		configure  func(*MemberSnapshot)
		elementID  Identifier
		super      bool
		wantAttack int8
		wantSpAtk  int8
		wantSpDef  int8
	}{
		{name: "弱点保险", configure: func(member *MemberSnapshot) { member.HeldItemWeaknessPolicy = true }, elementID: testID("fire"), super: true, wantAttack: 2, wantSpAtk: 2},
		{name: "球根", configure: func(member *MemberSnapshot) { member.HeldItemWaterDamageSpecialAttackBoostElementID = testID("water") }, elementID: testID("water"), wantSpAtk: 1},
		{name: "充电电池", configure: func(member *MemberSnapshot) { member.HeldItemElectricDamageAttackBoostElementID = testID("electric") }, elementID: testID("electric"), wantAttack: 1},
		{name: "光苔", configure: func(member *MemberSnapshot) { member.HeldItemWaterDamageSpecialDefenseBoostElementID = testID("water") }, elementID: testID("water"), wantSpDef: 1},
		{name: "雪球", configure: func(member *MemberSnapshot) { member.HeldItemIceDamageAttackBoostElementID = testID("ice") }, elementID: testID("ice"), wantAttack: 1},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			state := heldItemAdvancedState(t)
			target, _ := state.member(SideTwo, 1)
			target.ItemID = testID(test.name)
			test.configure(&target)
			state.replaceMember(SideTwo, target)
			state, events := applyHeldItemReceivedDamageStatBoost(state, MemberRef{Side: SideTwo, Position: 1}, testID("attack"), test.elementID, test.super, 10)
			target, _ = state.member(SideTwo, 1)
			if target.ItemID != 0 || target.StatStages[StatAttack] != test.wantAttack || target.StatStages[StatSpecialAttack] != test.wantSpAtk || target.StatStages[StatSpecialDefense] != test.wantSpDef || len(events) < 2 {
				t.Fatalf("%s 结果 = %+v, events=%+v", test.name, target, events)
			}
		})
	}
}

// TestHeldItemReceivedDamageStatBoostBoundaries 验证无属性、替身零本体伤害和全阶级上限均不会消费受伤反应道具。
func TestHeldItemReceivedDamageStatBoostBoundaries(t *testing.T) {
	for _, test := range []struct {
		name       string
		elementID  Identifier
		bodyDamage uint32
		maxStages  bool
	}{
		{name: "无属性伤害", bodyDamage: 10},
		{name: "替身承伤", elementID: testID("fire"), bodyDamage: 0},
		{name: "全部能力达到上限", elementID: testID("fire"), bodyDamage: 10, maxStages: true},
	} {
		t.Run(test.name, func(t *testing.T) {
			state := heldItemAdvancedState(t)
			target, _ := state.member(SideTwo, 1)
			target.ItemID, target.HeldItemWeaknessPolicy = testID("weakness-policy"), true
			if test.maxStages {
				target.StatStages[StatAttack], target.StatStages[StatSpecialAttack] = 6, 6
			}
			state.replaceMember(SideTwo, target)
			state, events := applyHeldItemReceivedDamageStatBoost(state, MemberRef{Side: SideTwo, Position: 1}, testID("attack"), test.elementID, true, test.bodyDamage)
			target, _ = state.member(SideTwo, 1)
			if target.ItemID != testID("weakness-policy") || len(events) != 0 {
				t.Fatalf("边界结果 = target=%+v events=%+v", target, events)
			}
		})
	}
}

// TestHeldItemAdditionalFlinchChance 验证王者之证只为普通伤害技能追加独立的 10% 畏缩概率。
func TestHeldItemAdditionalFlinchChance(t *testing.T) {
	actor := heldItemAdvancedMember(1, "actor")
	actor.ItemID = testID("kings-rock")
	actor.HeldItemAdditionalFlinchChancePercent = 10
	if got := effectiveFlinchChance(actor, actor.Skills[0]); got != 10 {
		t.Fatalf("王者之证畏缩概率 = %d，期望 10", got)
	}
	status := actor.Skills[0]
	status.DamageClass = DamageClassStatus
	if got := effectiveFlinchChance(actor, status); got != status.FlinchChancePercent {
		t.Fatalf("变化技能畏缩概率 = %d，期望 %d", got, status.FlinchChancePercent)
	}
}

// TestHeldItemAdditionalFlinchPreventsLaterAction 验证道具追加畏缩在伤害事件之后、较慢目标行动之前生效。
func TestHeldItemAdditionalFlinchPreventsLaterAction(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.Stats.Speed = 200
	actor.ItemID, actor.HeldItemAdditionalFlinchChancePercent = testID("kings-rock"), 100
	state.replaceMember(SideOne, actor)
	target, _ := state.member(SideTwo, 1)
	target.Stats.Speed = 100
	state.replaceMember(SideTwo, target)
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 7)
	result, err := ResolveTurn(state, heldItemAdvancedCommand(1, 1), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error=%v", err)
	}
	damageIndex, flinchIndex, preventedIndex := -1, -1, -1
	for index, event := range result.Events {
		switch value := event.(type) {
		case DamageAppliedEvent:
			if value.Actor.Side == SideOne {
				damageIndex = index
			}
		case FlinchAppliedEvent:
			flinchIndex = index
		case SkillPreventedEvent:
			if value.Actor.Side == SideTwo && value.Reason == SkillPreventionReasonFlinch {
				preventedIndex = index
			}
		}
	}
	if damageIndex < 0 || flinchIndex <= damageIndex || preventedIndex <= flinchIndex {
		t.Fatalf("王者之证事件顺序 = %+v", result.Events)
	}
}

// TestHeldItemAdditionalFlinchFollowsStatusAndPrecedesStat 验证道具追加畏缩位于规则声明的追加效果窗口。
func TestHeldItemAdditionalFlinchFollowsStatusAndPrecedesStat(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.Stats.Speed = 200
	actor.ItemID, actor.HeldItemAdditionalFlinchChancePercent = testID("kings-rock"), 100
	actor.Skills[0].StatusApplications = []MajorStatusApplication{{Status: MajorStatusPoison, Target: EffectTargetSelected, ChancePercent: 100}}
	actor.Skills[0].StatStageEffects = []StatStageEffect{{Stat: StatDefense, Target: EffectTargetSelected, StageDelta: -1, ChancePercent: 100}}
	state.replaceMember(SideOne, actor)
	target, _ := state.member(SideTwo, 1)
	target.Stats.Speed = 100
	state.replaceMember(SideTwo, target)
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 7)
	result, err := ResolveTurn(state, heldItemAdvancedCommand(1, 1), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error=%v", err)
	}
	statusIndex, flinchIndex, stageIndex := -1, -1, -1
	for index, event := range result.Events {
		switch event.Kind() {
		case EventKindMajorStatusApplied:
			statusIndex = index
		case EventKindFlinchApplied:
			flinchIndex = index
		case EventKindStatStageChanged:
			stageIndex = index
		}
	}
	if statusIndex < 0 || flinchIndex <= statusIndex || stageIndex <= flinchIndex {
		t.Fatalf("追加效果事件顺序 = %+v", result.Events)
	}
}

// TestHeldItemActionOrderBrackets 验证先制之爪成功时前置、失败时保持正常，而后攻之尾确定后置。
func TestHeldItemActionOrderBrackets(t *testing.T) {
	quick := heldItemAdvancedMember(1, "quick")
	quick.ItemID, quick.HeldItemRandomActionOrderBoostChancePercent = testID("quick-claw"), 20
	last := heldItemAdvancedMember(1, "last")
	last.ItemID, last.HeldItemForcedLastActionOrder = testID("lagging-tail"), true
	if skillActionOrderBracket(last, last.Skills[0]) >= 0 {
		t.Fatal("后攻之尾没有进入后置排序层")
	}
	plan := actionPlan{member: quick, action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}}}
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 1)
	plans, _, trace, err := applyHeldItemRandomActionOrderBoost([]actionPlan{plan}, random)
	if err != nil || len(trace) != 1 || (plans[0].orderBracket != 1 && plans[0].orderBracket != 0) {
		t.Fatalf("先制之爪排序 = %+v trace=%+v err=%v", plans, trace, err)
	}
	quick.HeldItemRandomActionOrderBoostChancePercent = 100
	state := heldItemAdvancedState(t)
	plans = []actionPlan{{member: quick, action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}}}}
	_, plans, _, events, trace, err := applyHeldItemActionOrderEffects(state, plans, random, 1)
	if err != nil || plans[0].orderBracket != 1 || len(events) != 1 || events[0].Kind() != EventKindHeldItemActionOrderApplied || len(trace) != 0 {
		t.Fatalf("必定先行事件 = plans=%+v events=%+v trace=%+v err=%v", plans, events, trace, err)
	}
}

// TestHeldItemRandomActionOrderUsesStableActorOrder 验证多个概率先行道具不受客户端行动数组顺序影响。
func TestHeldItemRandomActionOrderUsesStableActorOrder(t *testing.T) {
	first := heldItemAdvancedMember(1, "first")
	first.ItemID, first.HeldItemRandomActionOrderBoostChancePercent = testID("quick-claw-one"), 50
	second := heldItemAdvancedMember(1, "second")
	second.ItemID, second.HeldItemRandomActionOrderBoostChancePercent = testID("quick-claw-two"), 50
	plans := []actionPlan{
		{member: second, action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideTwo, Position: 1}}},
		{member: first, action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}}},
	}
	replay, err := NewTracedRandom([]RandomTraceEntry{
		{Sequence: 1, Bound: 100, Reason: "held item action order for side 1 member 1", Value: 0},
		{Sequence: 2, Bound: 100, Reason: "held item action order for side 2 member 1", Value: 99},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error=%v", err)
	}
	random := RandomSource{replaying: true, replay: replay}
	plans, _, trace, err := applyHeldItemRandomActionOrderBoost(plans, random)
	if err != nil || len(trace) != 2 || plans[0].orderBracket != 0 || plans[1].orderBracket != 1 {
		t.Fatalf("稳定随机顺序 = plans=%+v trace=%+v err=%v", plans, trace, err)
	}
}

// TestHeldItemLowHPActionOrderBoost 验证释陀果在四分之一生命边界于排序前消费并进入先行层。
func TestHeldItemLowHPActionOrderBoost(t *testing.T) {
	state := heldItemAdvancedState(t)
	holder, _ := state.member(SideOne, 1)
	holder.CurrentHP = holder.MaxHP / 4
	holder.ItemID, holder.HeldItemLowHPActionOrderBoost = testID("custap-berry"), true
	state.replaceMember(SideOne, holder)
	plans := []actionPlan{{action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}}, member: holder}}
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 1)
	state, plans, _, events, trace, err := applyHeldItemActionOrderEffects(state, plans, random, 1)
	holder, _ = state.member(SideOne, 1)
	if err != nil || holder.ItemID != 0 || plans[0].orderBracket != 1 || len(events) != 0 || len(trace) != 0 {
		t.Fatalf("释陀果结果 = holder=%+v plans=%+v events=%+v trace=%+v err=%v", holder, plans, events, trace, err)
	}
}

// TestHeldItemLowHPActionOrderRejectsFaintedHolder 验证零生命不属于闭区间四分之一门槛，不能消费释陀果。
func TestHeldItemLowHPActionOrderRejectsFaintedHolder(t *testing.T) {
	state := heldItemAdvancedState(t)
	holder, _ := state.member(SideOne, 1)
	holder.CurrentHP = 0
	holder.ItemID, holder.HeldItemLowHPActionOrderBoost = testID("custap-berry"), true
	state.replaceMember(SideOne, holder)
	plans := []actionPlan{{action: Action{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}}, member: holder}}
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 1)
	state, plans, _, events, trace, err := applyHeldItemActionOrderEffects(state, plans, random, 1)
	holder, _ = state.member(SideOne, 1)
	if err != nil || holder.ItemID != testID("custap-berry") || plans[0].orderBracket != 0 || len(events) != 0 || len(trace) != 0 {
		t.Fatalf("零生命释陀果结果 = holder=%+v plans=%+v events=%+v trace=%+v err=%v", holder, plans, events, trace, err)
	}
}

// TestHeldItemRoomService 验证戏法空间成功建立后客房服务降低一级速度并消费，速度下限时保留。
func TestHeldItemRoomService(t *testing.T) {
	state := heldItemAdvancedState(t)
	holder, _ := state.member(SideOne, 1)
	holder.ItemID, holder.HeldItemFieldSpeedOrderSpeedStageDrop = testID("room-service"), true
	state.replaceMember(SideOne, holder)
	state, events := applyHeldItemFieldSpeedOrderStatDrop(state, FieldSpeedOrderKindTrickRoom, testID("trick-room"))
	holder, _ = state.member(SideOne, 1)
	if holder.ItemID != 0 || holder.StatStages[StatSpeed] != -1 || len(events) != 2 {
		t.Fatalf("客房服务结果 = %+v events=%+v", holder, events)
	}
}

// TestHeldItemRoomServiceBoundaries 验证结束已有空间或速度已经到下限时不会消费客房服务。
func TestHeldItemRoomServiceBoundaries(t *testing.T) {
	for _, test := range []struct {
		name  string
		kind  FieldSpeedOrderKind
		stage int8
	}{
		{name: "不是戏法空间的新建事件", kind: FieldSpeedOrderKind("")},
		{name: "速度已经达到下限", kind: FieldSpeedOrderKindTrickRoom, stage: -6},
	} {
		t.Run(test.name, func(t *testing.T) {
			state := heldItemAdvancedState(t)
			holder, _ := state.member(SideOne, 1)
			holder.ItemID, holder.HeldItemFieldSpeedOrderSpeedStageDrop = testID("room-service"), true
			holder.StatStages[StatSpeed] = test.stage
			state.replaceMember(SideOne, holder)
			state, events := applyHeldItemFieldSpeedOrderStatDrop(state, test.kind, testID("trick-room"))
			holder, _ = state.member(SideOne, 1)
			if holder.ItemID != testID("room-service") || holder.StatStages[StatSpeed] != test.stage || len(events) != 0 {
				t.Fatalf("客房服务边界 = holder=%+v events=%+v", holder, events)
			}
		})
	}
}

// TestHeldItemConsecutiveSkillDamageBoost 验证节拍器连续宣告同一技能时从第二次起递增倍率，换招后重置。
func TestHeldItemConsecutiveSkillDamageBoost(t *testing.T) {
	member := heldItemAdvancedMember(1, "metronome")
	member.ItemID, member.HeldItemConsecutiveSkillDamageBoost = testID("metronome"), true
	member.LastDeclaredSkillID, member.ConsecutiveDeclaredSkillUses = testID("first"), 2
	if numerator, denominator := heldItemConsecutiveSkillDamageMultiplier(member, member.Skills[0]); numerator != 6 || denominator != 5 {
		t.Fatalf("节拍器第二次最终倍率 = %d/%d，期望 6/5", numerator, denominator)
	}
	member = recordDeclaredSkillUse(member, testID("second"))
	if member.LastDeclaredSkillID != testID("second") || member.ConsecutiveDeclaredSkillUses != 1 {
		t.Fatalf("节拍器换招状态 = %+v", member)
	}
}

// TestHeldItemConsecutiveSkillDamageMultiplierSequence 验证第一次至第六次及封顶后的精确最终倍率。
func TestHeldItemConsecutiveSkillDamageMultiplierSequence(t *testing.T) {
	member := heldItemAdvancedMember(1, "metronome")
	member.ItemID, member.HeldItemConsecutiveSkillDamageBoost = testID("metronome"), true
	member.LastDeclaredSkillID = member.Skills[0].SkillID
	for count, wantNumerator := range []uint64{5, 6, 7, 8, 9, 10, 10} {
		member.ConsecutiveDeclaredSkillUses = uint16(count + 1)
		numerator, denominator := heldItemConsecutiveSkillDamageMultiplier(member, member.Skills[0])
		if numerator != wantNumerator || denominator != 5 {
			t.Fatalf("第 %d 次倍率 = %d/%d，期望 %d/5", count+1, numerator, denominator, wantNumerator)
		}
	}
}

// TestHeldItemConsecutiveSkillStateClearsOnLeave 验证节拍器连续宣告状态属于连续在场周期，离场必须清零。
func TestHeldItemConsecutiveSkillStateClearsOnLeave(t *testing.T) {
	member := heldItemAdvancedMember(1, "metronome")
	member.LastDeclaredSkillID, member.ConsecutiveDeclaredSkillUses = testID("first"), 4
	member = leaveBattlefield(member)
	if member.LastDeclaredSkillID != 0 || member.ConsecutiveDeclaredSkillUses != 0 {
		t.Fatalf("节拍器离场状态 = %+v", member)
	}
}

// TestHeldItemConsecutiveSkillCountsAccuracyMiss 验证节拍器在 SkillUsed 宣告时推进，命中落空不会回退计数。
func TestHeldItemConsecutiveSkillCountsAccuracyMiss(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.Stats.Speed = 200
	actor.ItemID, actor.HeldItemConsecutiveSkillDamageBoost = testID("metronome"), true
	actor.Skills[0].Accuracy = 50
	state.replaceMember(SideOne, actor)
	target, _ := state.member(SideTwo, 1)
	target.Stats.Speed = 100
	target.FlinchedTurn = 1
	state.replaceMember(SideTwo, target)
	replay, err := NewTracedRandom([]RandomTraceEntry{{Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("first").String(), Value: 99}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error=%v", err)
	}
	result, err := ResolveTurn(state, heldItemAdvancedCommand(1, 1), replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error=%v", err)
	}
	actor, _ = result.State.member(SideOne, 1)
	if actor.LastDeclaredSkillID != testID("first") || actor.ConsecutiveDeclaredSkillUses != 1 {
		t.Fatalf("节拍器未命中后的宣告状态 = %+v", actor)
	}
}
