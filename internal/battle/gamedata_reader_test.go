package battle_test

import (
	"context"
	"encoding/json"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/team"
)

// TestGameDataFactsReaderCompilesConsistentInitialState 验证启动读取器仅使用同一实时修订编译出可交给引擎的完整状态。
func TestGameDataFactsReaderCompilesConsistentInitialState(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	reader := fixture.reader()

	facts, err := reader.ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if facts.Sides[0].Members[0].Level != 50 || facts.Sides[0].Members[0].MaxHP == 0 || facts.Sides[0].Members[0].Stats.Attack == 0 {
		t.Fatalf("编译成员事实 = %+v", facts.Sides[0].Members[0])
	}
	if facts.Rules.ElementIDs["normal"] != fixture.elementID {
		t.Fatalf("属性规则映射 = %+v", facts.Rules.ElementIDs)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if _, err := battleengine.NewState(initial); err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
}

// TestGameDataFactsReaderFreezesContactSuppression 验证接触抑制特性会在 Battle 启动前冻结，运行期无需回读资料。
func TestGameDataFactsReaderFreezesContactSuppression(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{ContactSuppression: true, ReceivedContactDamageHalved: true, ReceivedFireDamageDoubled: true, IndirectDamageImmunity: true, ContactDamageToAttackerDenominator: 8}, Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].ContactSuppression || !facts.Sides[0].Members[0].ReceivedContactDamageHalved || !facts.Sides[0].Members[0].ReceivedFireDamageDoubled || !facts.Sides[0].Members[0].IndirectDamageImmunity || facts.Sides[0].Members[0].ContactDamageToAttackerDenominator != 8 {
		t.Fatalf("接触类特性未冻结到成员事实: %+v", facts.Sides[0].Members[0])
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil || !initial.Sides[0].Members[0].ContactSuppression || !initial.Sides[0].Members[0].ReceivedContactDamageHalved || !initial.Sides[0].Members[0].ReceivedFireDamageDoubled || !initial.Sides[0].Members[0].IndirectDamageImmunity || initial.Sides[0].Members[0].ContactDamageToAttackerDenominator != 8 {
		t.Fatalf("接触类特性未冻结到引擎初始状态: initial=%+v, error=%v", initial, err)
	}
}

// TestGameDataFactsReaderFreezesAbilityAccuracyRules 验证命中类特性从实时详情编译为 Battle 独占的强类型快照。
// 资料对象和中间事实随后发生的修改都不能影响已经编译的引擎初始状态；损坏分数必须阻止新对局启动。
func TestGameDataFactsReaderFreezesAbilityAccuracyRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{
			AccuracyMultiplier:                  &abilitydetail.AccuracyMultiplier{Numerator: 13, Denominator: 10},
			PhysicalSkillAccuracyMultiplier:     &abilitydetail.AccuracyMultiplier{Numerator: 4, Denominator: 5},
			OpponentAccuracySandstormMultiplier: &abilitydetail.AccuracyMultiplier{Numerator: 4, Denominator: 5},
			OpponentAccuracySnowMultiplier:      &abilitydetail.AccuracyMultiplier{Numerator: 4, Denominator: 5},
			OpponentAccuracyConfusionMultiplier: &abilitydetail.AccuracyMultiplier{Numerator: 1, Denominator: 2},
			AccuracyAlwaysHits:                  true,
			StatusSkillAccuracyCap:              50,
			IgnoreOpponentAccuracyStatStages:    true,
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.AccuracyMultiplier == nil || member.AccuracyMultiplier.Numerator != 13 ||
		member.PhysicalSkillAccuracyMultiplier == nil || member.PhysicalSkillAccuracyMultiplier.Denominator != 5 ||
		member.OpponentAccuracySandstormMultiplier == nil || member.OpponentAccuracySnowMultiplier == nil ||
		member.OpponentAccuracyConfusionMultiplier == nil || member.OpponentAccuracyConfusionMultiplier.Numerator != 1 ||
		!member.AccuracyAlwaysHits || member.StatusSkillAccuracyCap != 50 || !member.IgnoreOpponentAccuracyStatStages {
		t.Fatalf("命中特性成员事实 = %+v", member)
	}
	fixture.abilityDetail.AccuracyMultiplier.Numerator = 1
	if member.AccuracyMultiplier.Numerator != 13 {
		t.Fatalf("成员事实与实时资料共享命中倍率指针: facts=%+v detail=%+v", member.AccuracyMultiplier, fixture.abilityDetail.AccuracyMultiplier)
	}

	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0]
	member.AccuracyMultiplier.Numerator = 2
	if frozen.AccuracyMultiplier == nil || frozen.AccuracyMultiplier.Numerator != 13 ||
		frozen.PhysicalSkillAccuracyMultiplier == nil || frozen.OpponentAccuracySandstormMultiplier == nil ||
		frozen.OpponentAccuracySnowMultiplier == nil || frozen.OpponentAccuracyConfusionMultiplier == nil ||
		!frozen.AccuracyAlwaysHits || frozen.StatusSkillAccuracyCap != 50 || !frozen.IgnoreOpponentAccuracyStatStages {
		t.Fatalf("Battle 未独立冻结命中特性: %+v", frozen)
	}

	fixture.abilityDetail.AccuracyMultiplier = &abilitydetail.AccuracyMultiplier{Numerator: 0, Denominator: 1}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("损坏命中倍率 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderFreezesHeldItemElementIdentity 验证读取器把特性开关与道具属性身份分别冻结到成员事实，
// 使 Battle Engine 在运行期无需再访问 Item Metadata。
func TestGameDataFactsReaderFreezesHeldItemElementIdentity(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	for side := range fixture.session.Participants {
		fixture.session.Participants[side].Team.Members[0].ItemID = &itemID
	}
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInHeldItemElementIdentity: true}, Version: 1,
	}
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, ElementDamageBoostElementID: &fixture.elementID,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if !member.SwitchInHeldItemElementIdentity || member.HeldItemElementID != fixture.elementID || member.ItemID != itemID {
		t.Fatalf("冻结的道具属性身份 = %+v", member)
	}
}

// TestGameDataFactsReaderFreezesHeldItemEndTurnHealing 验证道具的回合末回复分母在 Battle 启动时冻结到引擎成员。
//
// Battle Engine 运行期不得回读 Item Metadata；资料在 Battle 创建之后即使变化，也不能改变已开始对局的回复比例。
func TestGameDataFactsReaderFreezesHeldItemEndTurnHealing(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, EndTurnHealDenominator: 16,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if facts.Sides[0].Members[0].HeldItemEndTurnHealDenominator != 16 {
		t.Fatalf("冻结的回合末回复道具规则 = %+v", facts.Sides[0].Members[0])
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if initial.Sides[0].Members[0].HeldItemEndTurnHealDenominator != 16 {
		t.Fatalf("引擎成员中的回合末回复道具规则 = %+v", initial.Sides[0].Members[0])
	}
}

// TestGameDataFactsReaderFreezesHeldItemEndTurnHealingForElement 验证属性条件道具回复在 Battle 启动时冻结属性与分母。
//
// 引擎只读取冻结成员快照，运行期间不会回查 Item Metadata；资料修改不能影响已经开始对局的条件回复规则。
func TestGameDataFactsReaderFreezesHeldItemEndTurnHealingForElement(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, EndTurnHealForElementID: &fixture.elementID, EndTurnHealForElementDenominator: 16,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.HeldItemEndTurnHealForElementID != fixture.elementID || member.HeldItemEndTurnHealForElementDenominator != 16 {
		t.Fatalf("冻结的属性条件回合末回复道具规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	finalMember := initial.Sides[0].Members[0]
	if finalMember.HeldItemEndTurnHealForElementID != fixture.elementID || finalMember.HeldItemEndTurnHealForElementDenominator != 16 {
		t.Fatalf("引擎成员中的属性条件回合末回复道具规则 = %+v", finalMember)
	}
}

// TestGameDataFactsReaderFreezesHeldItemEndTurnDamage 验证道具的回合末自伤分母在 Battle 启动时冻结到引擎成员。
//
// 运行期只读取成员快照；资料在 Battle 创建之后即使变化，也不能改变已经开始对局的自伤比例或间接伤害边界。
func TestGameDataFactsReaderFreezesHeldItemEndTurnDamage(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, EndTurnDamageDenominator: 8,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if facts.Sides[0].Members[0].HeldItemEndTurnDamageDenominator != 8 {
		t.Fatalf("冻结的回合末自伤道具规则 = %+v", facts.Sides[0].Members[0])
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if initial.Sides[0].Members[0].HeldItemEndTurnDamageDenominator != 8 {
		t.Fatalf("引擎成员中的回合末自伤道具规则 = %+v", initial.Sides[0].Members[0])
	}
}

// TestGameDataFactsReaderFreezesHeldItemEndTurnDamageWithoutElement 验证属性条件自伤规则在 Battle 启动时冻结属性与分母。
//
// 对局开始后资料变更不得影响条件；Battle Engine 只根据冻结的属性 Identifier、分母与成员当前 ElementIDs 做判定。
func TestGameDataFactsReaderFreezesHeldItemEndTurnDamageWithoutElement(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, EndTurnDamageWithoutElementID: &fixture.elementID, EndTurnDamageWithoutElementDenominator: 8,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.HeldItemEndTurnDamageWithoutElementID != fixture.elementID || member.HeldItemEndTurnDamageWithoutElementDenominator != 8 {
		t.Fatalf("冻结的属性条件回合末自伤道具规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	finalMember := initial.Sides[0].Members[0]
	if finalMember.HeldItemEndTurnDamageWithoutElementID != fixture.elementID || finalMember.HeldItemEndTurnDamageWithoutElementDenominator != 8 {
		t.Fatalf("引擎成员中的属性条件回合末自伤道具规则 = %+v", finalMember)
	}
}

// TestGameDataFactsReaderFreezesHeldItemBindingRules 验证 Current Game Data 将追加效果免疫、固定束缚
// 次数和伤害分母逐字段冻结到 Battle facts，并在编译初始状态时完成第二次值复制。
func TestGameDataFactsReaderFreezesHeldItemBindingRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, DamagingSkillSecondaryEffectImmunity: true,
		BindingTurns: 7, BindingDamageDenominator: 6,
	}}}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error=%v", err)
	}
	member := facts.Sides[0].Members[0]
	if !member.HeldItemDamagingSkillSecondaryEffectImmunity || member.HeldItemBindingTurns != 7 || member.HeldItemBindingDamageDenominator != 6 {
		t.Fatalf("Battle facts 束缚道具规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error=%v", err)
	}
	frozen := initial.Sides[0].Members[0]
	if !frozen.HeldItemDamagingSkillSecondaryEffectImmunity || frozen.HeldItemBindingTurns != 7 || frozen.HeldItemBindingDamageDenominator != 6 {
		t.Fatalf("Battle Engine 冻结束缚道具规则 = %+v", frozen)
	}
}

// TestGameDataFactsReaderFreezesEnvironmentHighestStatMultiplier 验证读取器将特性环境条件转换为引擎封闭枚举快照。
//
// 运行期不再保留资料层的可空指针或 Stable Code，资料后续修改也不能影响已经进入 starting 的 Battle。
func TestGameDataFactsReaderFreezesEnvironmentHighestStatMultiplier(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	weather := abilitydetail.WeatherKindSun
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{EnvironmentHighestStatMultiplier: &abilitydetail.EnvironmentHighestStatMultiplier{
			RequiredWeather: &weather,
		}},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].EnvironmentHighestStatMultiplier
	if rule == nil || rule.RequiredWeather != battleengine.WeatherKindSun || rule.RequiredTerrain != "" {
		t.Fatalf("冻结的环境最高能力规则 = %+v", rule)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0].EnvironmentHighestStatMultiplier
	if frozen == nil || frozen == rule || frozen.RequiredWeather != battleengine.WeatherKindSun {
		t.Fatalf("初始状态中的环境最高能力规则 = %+v，期望独立深复制", frozen)
	}
}

// TestGameDataFactsReaderFreezesHighestStatBoosterAbilities 验证道具可触发特性集合会在 Battle 启动前解析、校验并冻结。
func TestGameDataFactsReaderFreezesHighestStatBoosterAbilities(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	for side := range fixture.session.Participants {
		fixture.session.Participants[side].Team.Members[0].ItemID = &itemID
	}
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, HighestStatBoosterAbilityIDs: []snowflake.ID{fixture.ability.ID},
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if len(member.HighestStatBoosterAbilityIDs) != 1 || member.HighestStatBoosterAbilityIDs[0] != fixture.ability.ID {
		t.Fatalf("冻结的道具可触发特性集合 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0].HighestStatBoosterAbilityIDs
	if len(frozen) != 1 || frozen[0] != fixture.ability.ID || &frozen[0] == &member.HighestStatBoosterAbilityIDs[0] {
		t.Fatalf("初始状态中的道具可触发特性集合 = %v，期望独立快照", frozen)
	}
}

// TestGameDataFactsReaderFreezesForcedSwitchItemRules 验证一次性道具的强制换人规则从实时 Item Metadata 冻结到
// Battle 初始事实与 Battle Engine 成员快照；运行期不得再读取或根据道具名称推断规则。
func TestGameDataFactsReaderFreezesForcedSwitchItemRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, DamagedForceSelfSwitch: true,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if !member.DamagedForceSelfSwitch || member.DamagedForceAttackerSwitch || member.NegativeStatStageForceSelfSwitch {
		t.Fatalf("冻结的道具强制换人规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0]
	if !frozen.DamagedForceSelfSwitch || frozen.DamagedForceAttackerSwitch || frozen.NegativeStatStageForceSelfSwitch {
		t.Fatalf("引擎成员中的道具强制换人规则 = %+v", frozen)
	}
}

// TestGameDataFactsReaderFreezesSwitchRestrictionImmunity 验证道具提供的主动换人限制豁免会冻结到 Battle 与引擎快照。
//
// 该豁免与技能、道具造成的强制换人无关，因此必须使用独立字段传递，不能错误复用 ForcedSwitchImmunity。
func TestGameDataFactsReaderFreezesSwitchRestrictionImmunity(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, SwitchRestrictionImmunity: true, ContactSideEffectImmunity: true, ContactDamageToAttackerDenominator: 6, ContactTransferToAttacker: true, ChargeSkipOnce: true, SurviveFatalDamageAtFullHP: true, ReflectTurnsRemaining: 8, LightScreenTurnsRemaining: 8, AuroraVeilTurnsRemaining: 8, RainTurnsRemaining: 8, SandstormTurnsRemaining: 8, SnowTurnsRemaining: 8, SunTurnsRemaining: 8, TerrainTurnsRemaining: 8, SandstormDamageImmunity: true, WeightHalf: true, CuresParalysis: true, CuresSleep: true, CuresPoison: true, CuresBurn: true, CuresFreeze: true, CuresAllMajorStatuses: true, CuresConfusion: true, PunchBasedSkillPowerBoost: true, PunchBasedContactSuppression: true, PowderSkillImmunity: true, MultiHitCountMinimum: 4, MultiHitCountMaximum: 5, MultiHitRequiredMinimum: 2, MultiHitRequiredMaximum: 5,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchRestrictionImmunity || !facts.Sides[0].Members[0].ContactSideEffectImmunity || facts.Sides[0].Members[0].HeldItemContactDamageToAttackerDenominator != 6 || !facts.Sides[0].Members[0].ContactTransferToAttacker || !facts.Sides[0].Members[0].ChargeSkipOnce || !facts.Sides[0].Members[0].HeldItemSurviveFatalDamageAtFullHP || facts.Sides[0].Members[0].HeldItemReflectTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemLightScreenTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemAuroraVeilTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemRainTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemSandstormTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemSnowTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemSunTurnsRemaining != 8 || facts.Sides[0].Members[0].HeldItemTerrainTurnsRemaining != 8 || !facts.Sides[0].Members[0].HeldItemSandstormDamageImmunity || !facts.Sides[0].Members[0].HeldItemWeightHalf || !facts.Sides[0].Members[0].HeldItemCuresParalysis || !facts.Sides[0].Members[0].HeldItemCuresSleep || !facts.Sides[0].Members[0].HeldItemCuresPoison || !facts.Sides[0].Members[0].HeldItemCuresBurn || !facts.Sides[0].Members[0].HeldItemCuresFreeze || !facts.Sides[0].Members[0].HeldItemCuresAllMajorStatuses || !facts.Sides[0].Members[0].HeldItemCuresConfusion || !facts.Sides[0].Members[0].HeldItemPunchBasedSkillPowerBoost || !facts.Sides[0].Members[0].HeldItemPunchBasedContactSuppression || !facts.Sides[0].Members[0].HeldItemPowderSkillImmunity {
		t.Fatalf("冻结的道具豁免规则 = %+v", facts.Sides[0].Members[0])
	}
	if facts.Sides[0].Members[0].HeldItemMultiHitCountMinimum != 4 || facts.Sides[0].Members[0].HeldItemMultiHitCountMaximum != 5 ||
		facts.Sides[0].Members[0].HeldItemMultiHitRequiredMinimum != 2 || facts.Sides[0].Members[0].HeldItemMultiHitRequiredMaximum != 5 {
		t.Fatalf("冻结的连续命中道具区间 = %+v", facts.Sides[0].Members[0])
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if !initial.Sides[0].Members[0].SwitchRestrictionImmunity || !initial.Sides[0].Members[0].ContactSideEffectImmunity || initial.Sides[0].Members[0].HeldItemContactDamageToAttackerDenominator != 6 || !initial.Sides[0].Members[0].ContactTransferToAttacker || !initial.Sides[0].Members[0].ChargeSkipOnce || !initial.Sides[0].Members[0].HeldItemSurviveFatalDamageAtFullHP || initial.Sides[0].Members[0].HeldItemReflectTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemLightScreenTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemAuroraVeilTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemRainTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemSandstormTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemSnowTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemSunTurnsRemaining != 8 || initial.Sides[0].Members[0].HeldItemTerrainTurnsRemaining != 8 || !initial.Sides[0].Members[0].HeldItemSandstormDamageImmunity || !initial.Sides[0].Members[0].HeldItemWeightHalf || !initial.Sides[0].Members[0].HeldItemCuresParalysis || !initial.Sides[0].Members[0].HeldItemCuresSleep || !initial.Sides[0].Members[0].HeldItemCuresPoison || !initial.Sides[0].Members[0].HeldItemCuresBurn || !initial.Sides[0].Members[0].HeldItemCuresFreeze || !initial.Sides[0].Members[0].HeldItemCuresAllMajorStatuses || !initial.Sides[0].Members[0].HeldItemCuresConfusion || !initial.Sides[0].Members[0].HeldItemPunchBasedSkillPowerBoost || !initial.Sides[0].Members[0].HeldItemPunchBasedContactSuppression || !initial.Sides[0].Members[0].HeldItemPowderSkillImmunity {
		t.Fatalf("引擎成员中的道具豁免规则 = %+v", initial.Sides[0].Members[0])
	}
	if initial.Sides[0].Members[0].HeldItemMultiHitCountMinimum != 4 || initial.Sides[0].Members[0].HeldItemMultiHitCountMaximum != 5 ||
		initial.Sides[0].Members[0].HeldItemMultiHitRequiredMinimum != 2 || initial.Sides[0].Members[0].HeldItemMultiHitRequiredMaximum != 5 {
		t.Fatalf("引擎成员中的连续命中道具区间 = %+v", initial.Sides[0].Members[0])
	}
}

// TestGameDataFactsReaderRejectsDisabledHighestStatBoosterAbility 验证绕过维护校验写入的禁用道具特性引用不能进入新 Battle。
func TestGameDataFactsReaderRejectsDisabledHighestStatBoosterAbility(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	for side := range fixture.session.Participants {
		fixture.session.Participants[side].Team.Members[0].ItemID = &itemID
	}
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, HighestStatBoosterAbilityIDs: []snowflake.ID{snowflake.NewTestID()},
	}}}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderRejectsHeldItemElementIdentityWithDisabledElement 验证 Item Metadata 被绕过维护校验写入
// 未启用属性时，启动编译明确失败而不是把损坏 Identifier 带入正在运行的对局。
func TestGameDataFactsReaderRejectsHeldItemElementIdentityWithDisabledElement(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID, disabledElementID := snowflake.NewTestID(), snowflake.NewTestID()
	for side := range fixture.session.Participants {
		fixture.session.Participants[side].Team.Members[0].ItemID = &itemID
	}
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInHeldItemElementIdentity: true}, Version: 1,
	}
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, ElementDamageBoostElementID: &disabledElementID,
	}}}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderUsesFrozenMemberLevelForPreserveFormat 验证保留等级赛制读取 Team 中冻结的成员等级。
func TestGameDataFactsReaderUsesFrozenMemberLevelForPreserveFormat(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRulePreserve})
	fixture.member.Level = 37
	fixture.session.Participants = []battle.Participant{
		{Side: battle.ParticipantSideOne, Team: battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 1, Members: []team.Member{fixture.member}}},
		{Side: battle.ParticipantSideTwo, Team: battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 1, Members: []team.Member{fixture.member}}},
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if facts.Sides[0].Members[0].Level != 37 {
		t.Fatalf("保留等级 = %d，期望 37", facts.Sides[0].Members[0].Level)
	}
}

// TestGameDataFactsReaderCompilesStatChangesWithoutOptionalDetail 验证独立技能数值变化不会因缺少技能详情而被静默丢弃。
func TestGameDataFactsReaderCompilesStatChangesWithoutOptionalDetail(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	for statID, value := range fixture.statsByID {
		if value.Code == "attack" {
			fixture.statChanges = []skillstatchange.Change{{ID: snowflake.NewTestID(), SkillID: fixture.skillID, StatID: statID, ChangeValue: 1, Version: 1}}
			break
		}
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	effects := facts.Sides[0].Members[0].Skills[0].StatStageEffects
	if len(effects) != 1 || effects[0].Stat != battleengine.StatAttack || effects[0].Target != battleengine.EffectTargetSelected {
		t.Fatalf("技能数值变化 = %+v", effects)
	}
}

// TestGameDataFactsReaderCompilesSkillTargetScope 验证实时技能目标资料会编译为冻结的强类型范围，
// 使范围行为不能因管理资料 code 被遗漏而静默降级为单体技能。
func TestGameDataFactsReaderCompilesSkillTargetScope(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetID := snowflake.NewTestID()
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			TargetID: &targetID, Drain: int32Pointer(50), Healing: int32Pointer(-25),
			TargetHealingNumerator: int32Pointer(1), TargetHealingDenominator: int32Pointer(2),
			SlicingBased: true, SoundBased: true, PulseBased: true, BiteBased: true,
			MinHits: int32Pointer(2), MaxHits: int32Pointer(5), CritRate: int32Pointer(2), FlinchChance: int32Pointer(30),
		},
		Version: 1,
	}
	fixture.targetsByID[targetID] = skilltarget.Target{
		ID: targetID, Code: "all-opponents", Name: "对方全体", Enabled: true, Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if scope := facts.Sides[0].Members[0].Skills[0].TargetScope; scope != battleengine.SkillTargetScopeAllAdjacentOpponents {
		t.Fatalf("技能目标范围 = %q，期望 %q", scope, battleengine.SkillTargetScopeAllAdjacentOpponents)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DrainPercent != 50 || compiled.HealingPercent != -25 {
		t.Fatalf("技能生命效果 = drain %d, healing %d，期望 50, -25", compiled.DrainPercent, compiled.HealingPercent)
	}
	if compiled.TargetHealingNumerator != 1 || compiled.TargetHealingDenominator != 2 {
		t.Fatalf("技能目标最大生命回复 = %d/%d，期望 1/2", compiled.TargetHealingNumerator, compiled.TargetHealingDenominator)
	}
	if !compiled.SlicingBased || !compiled.SoundBased || !compiled.PulseBased || !compiled.BiteBased {
		t.Fatalf("冻结的公式技能标签 = slicing:%t sound:%t pulse:%t bite:%t", compiled.SlicingBased, compiled.SoundBased, compiled.PulseBased, compiled.BiteBased)
	}
	if compiled.MinHits != 2 || compiled.MaxHits != 5 || compiled.CriticalHitStage != 2 || compiled.FlinchChancePercent != 30 {
		t.Fatalf("技能连续命中、要害和畏缩 = min %d, max %d, critical %d, flinch %d，期望 2, 5, 2, 30", compiled.MinHits, compiled.MaxHits, compiled.CriticalHitStage, compiled.FlinchChancePercent)
	}
}

// TestGameDataFactsReaderFreezesSelectedGenderCode 验证 Team 选择的性别 Identifier 会在 Battle 启动时解析为
// 启用性别资料的稳定代码。战斗引擎只比较冻结代码，不得在回合结算时读取 Current Game Data，也不能把
// Identifier 字符串或展示名称误当作性别类别。
func TestGameDataFactsReaderFreezesSelectedGenderCode(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	genderID := snowflake.NewTestID()
	fixture.metadata.Genders = append(fixture.metadata.Genders, creaturemetadata.Gender{
		ID: genderID, Code: "female", Name: "雌性", Enabled: true,
	})
	fixture.member.GenderID = &genderID
	for index := range fixture.session.Participants {
		fixture.session.Participants[index].Team.Members[0].GenderID = &genderID
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if got := facts.Sides[0].Members[0].GenderCode; got != "female" {
		t.Fatalf("冻结性别代码 = %q，期望 female", got)
	}
}

// TestGameDataFactsReaderCompilesDirectDamageRule 验证管理端技能详情中的固定伤害模式会被编译成纯
// Battle Engine 快照，而不是让运行时读取描述文本或普通威力字段猜测规则。
func TestGameDataFactsReaderCompilesDirectDamageRule(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			DamageMode: skilldetail.DamageModeFixedAmount, DamageAmount: int32Pointer(40),
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DamageMode != battleengine.SkillDamageModeFixedAmount || compiled.DamageAmount != 40 {
		t.Fatalf("直接伤害快照 = mode %q, amount %d，期望 fixedAmount, 40", compiled.DamageMode, compiled.DamageAmount)
	}
}

// TestGameDataFactsReaderCompilesDirectDamageModeVariants 验证其余直接伤害资料在 Battle 启动时被逐字段
// 冻结为引擎模式；运行时不读取技能名称、说明文本或当前数据库来推断等级、生命差值和比例伤害。
func TestGameDataFactsReaderCompilesDirectDamageModeVariants(t *testing.T) {
	t.Parallel()
	numerator, denominator, minimum := int32(1), int32(2), int32(1)
	tests := []struct {
		// name 是资料冻结规则的稳定子测试名称。
		name string
		// values 是当前技能详情中的直接伤害资料。
		values skilldetail.OptionalValues
		// mode 是资料编译后的纯引擎稳定模式。
		mode battleengine.SkillDamageMode
	}{
		{name: "使用者等级", values: skilldetail.OptionalValues{DamageMode: skilldetail.DamageModeUserLevel}, mode: battleengine.SkillDamageModeUserLevel},
		{name: "双方当前生命差值", values: skilldetail.OptionalValues{DamageMode: skilldetail.DamageModeTargetCurrentHPMinusUserCurrentHP}, mode: battleengine.SkillDamageModeTargetCurrentHPMinusUserCurrentHP},
		{name: "使用者当前生命并倒下", values: skilldetail.OptionalValues{DamageMode: skilldetail.DamageModeUserCurrentHPAndUserFaints}, mode: battleengine.SkillDamageModeUserCurrentHPAndUserFaints},
		{name: "目标当前生命比例", values: skilldetail.OptionalValues{
			DamageMode:      skilldetail.DamageModeTargetCurrentHPFraction,
			DamageNumerator: &numerator, DamageDenominator: &denominator, MinimumDamage: &minimum,
		}, mode: battleengine.SkillDamageModeTargetCurrentHPFraction},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
			fixture.detail = &skilldetail.RuleSet{
				ID: snowflake.NewTestID(), SkillID: fixture.skillID, OptionalValues: test.values, Version: 1,
			}
			facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
			if err != nil {
				t.Fatalf("ReadInitialStateFacts() error = %v", err)
			}
			compiled := facts.Sides[0].Members[0].Skills[0]
			if compiled.DamageMode != test.mode {
				t.Fatalf("直接伤害模式 = %q，期望 %q", compiled.DamageMode, test.mode)
			}
			if test.mode == battleengine.SkillDamageModeTargetCurrentHPFraction &&
				(compiled.DamageNumerator != 1 || compiled.DamageDenominator != 2 || compiled.MinimumDamage != 1) {
				t.Fatalf("目标当前生命比例快照 = %+v", compiled)
			}
		})
	}
}

// TestGameDataFactsReaderCompilesDynamicPowerRule 验证动态基础威力和精灵体重会在 Battle 启动时冻结到纯引擎
// 快照；对局开始后即使实时资料进入维护或被修改，也不会改变本局逐目标威力计算的输入。
func TestGameDataFactsReaderCompilesDynamicPowerRule(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.metadata.Creatures[0].Weight = int32Pointer(500)
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{DynamicPower: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 40,
			SpeedThresholds: []skilldetail.SpeedPowerThreshold{{MinimumRatio: 2, Power: 80}, {MinimumRatio: 1, Power: 60}},
		}},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.Weight != 500 || member.Skills[0].DynamicPower.Kind != battleengine.DynamicPowerKindUserSpeedRatioThresholds ||
		member.Skills[0].DynamicPower.FallbackPower != 40 || len(member.Skills[0].DynamicPower.SpeedThresholds) != 2 ||
		member.Skills[0].DynamicPower.SpeedThresholds[0].Power != 80 {
		t.Fatalf("动态威力成员事实 = %+v", member)
	}
}

// TestGameDataFactsReaderCompilesCurrentHPAverageRule 验证资料层的当前生命平均代码只能由变化技能
// 编译成引擎专用非伤害规则，不会被错误降级为普通公式伤害。
func TestGameDataFactsReaderCompilesCurrentHPAverageRule(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{DamageMode: skilldetail.DamageModeAverageUserAndTargetCurrentHP},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DamageClass != battleengine.DamageClassStatus || compiled.DamageMode != battleengine.SkillDamageModeAverageUserAndTargetCurrentHP {
		t.Fatalf("当前生命平均快照 = %+v", compiled)
	}
}

// TestGameDataFactsReaderCompilesOneHitKnockOutRule 验证资料层的一击必杀参数会冻结为独立引擎模式，
// 从而让运行时使用专用等级命中公式，而不是普通 Accuracy 字段或展示文本。
func TestGameDataFactsReaderCompilesOneHitKnockOutRule(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.skill.Power = nil
	baseAccuracy, sameElementAccuracy := int32(20), int32(30)
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			DamageMode:                 skilldetail.DamageModeOneHitKnockOut,
			OneHitKnockOutBaseAccuracy: &baseAccuracy,
			OneHitKnockOutSameElementUserBaseAccuracy: &sameElementAccuracy,
			OneHitKnockOutBlocksSameElementTarget:     true,
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DamageMode != battleengine.SkillDamageModeOneHitKnockOut || compiled.OneHitKnockOutBaseAccuracy != 20 ||
		compiled.OneHitKnockOutSameElementUserBaseAccuracy != 30 || !compiled.OneHitKnockOutBlocksSameElementTarget {
		t.Fatalf("一击必杀技能快照 = %+v", compiled)
	}
}

// TestGameDataFactsReaderCompilesReceivedDamageRule 验证伤害记忆资料会冻结为引擎的专用模式及完整参数。
// 对局开始后引擎只使用该快照，不会再次读取可被维护窗口修改的实时资料。
func TestGameDataFactsReaderCompilesReceivedDamageRule(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.skill.Power = nil
	numerator, denominator := int32(2), int32(1)
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			DamageMode:                                        skilldetail.DamageModeReceivedDamage,
			ReceivedDamageNumerator:                           &numerator,
			ReceivedDamageDenominator:                         &denominator,
			ReceivedDamageAcceptsPhysical:                     true,
			ReceivedDamageIgnoreNonImmuneElementEffectiveness: true,
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DamageMode != battleengine.SkillDamageModeReceivedDamage || compiled.ReceivedDamageNumerator != 2 ||
		compiled.ReceivedDamageDenominator != 1 || !compiled.ReceivedDamageAcceptsPhysical ||
		compiled.ReceivedDamageAcceptsSpecial || !compiled.ReceivedDamageIgnoreNonImmuneElementEffectiveness {
		t.Fatalf("伤害记忆技能快照 = %+v", compiled)
	}
}

// TestGameDataFactsReaderCompilesMajorStatusCures 验证三个彼此独立的主要异常净化资料字段会原样冻结到
// 纯战斗引擎快照，而不是由技能名称、目标范围或展示说明推断实际净化范围。
func TestGameDataFactsReaderCompilesMajorStatusCures(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			CuresUserSideMajorStatuses:       true,
			CuresUserMajorStatus:             true,
			CuresUserSideActiveMajorStatuses: true,
			ForceTargetSwitch:                true,
			RechargesAfterUse:                true,
			LocksAccuracyOnTarget:            true,
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	compiled := facts.Sides[0].Members[0].Skills[0]
	if compiled.DamageClass != battleengine.DamageClassStatus || !compiled.CuresUserSideMajorStatuses || !compiled.ForceTargetSwitch || !compiled.RechargesAfterUse || !compiled.LocksAccuracyOnTarget ||
		!compiled.CuresUserMajorStatus || !compiled.CuresUserSideActiveMajorStatuses {
		t.Fatalf("主要异常净化技能快照 = %+v", compiled)
	}
}

// TestGameDataFactsReaderRejectsMajorStatusCureOnDamagingSkill 验证资料层即使被绕过校验写入，也不能让
// 伤害技能取得状态净化语义；此类不变量必须在 Battle 启动前失败。
func TestGameDataFactsReaderRejectsMajorStatusCureOnDamagingSkill(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{CuresUserMajorStatus: true},
		Version:        1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望资料编译失败", err)
	}
}

// TestGameDataFactsReaderCompilesVolatileEffects 验证技能详情中的封闭易变状态会在 Battle 启动时
// 编译为纯战斗引擎快照。运行时只读取该快照，绝不读取管理资料中的展示文本或 JSON。
func TestGameDataFactsReaderCompilesVolatileEffects(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{VolatileEffects: []skilldetail.VolatileEffect{
			{
				Status: skilldetail.VolatileStatusTaunt, Target: skilldetail.VolatileEffectTargetSelectedTarget,
				ChancePercent: 80, MinTurns: 2, MaxTurns: 4,
			},
			{
				Status: skilldetail.VolatileStatusProtection, Target: skilldetail.VolatileEffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
			},
			{
				Status: skilldetail.VolatileStatusSubstitute, Target: skilldetail.VolatileEffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
			},
		}},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	effects := facts.Sides[0].Members[0].Skills[0].VolatileStatusApplications
	if len(effects) != 3 || effects[0] != (battleengine.VolatileStatusApplication{
		Status: battleengine.VolatileStatusTaunt, Target: battleengine.EffectTargetSelected, ChancePercent: 80, MinTurns: 2, MaxTurns: 4,
	}) || effects[1] != (battleengine.VolatileStatusApplication{
		Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}) || effects[2] != (battleengine.VolatileStatusApplication{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}) {
		t.Fatalf("编译后的易变状态 = %+v", effects)
	}
}

// TestGameDataFactsReaderCompilesFieldSpeedOrder 验证资料层独立的全场速度顺序效果会在 Battle 启动时编译为
// 纯引擎强类型 application，而不是以目标状态、名称或原始 JSON 的形式进入运行时。
func TestGameDataFactsReaderCompilesFieldSpeedOrder(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetID := snowflake.NewTestID()
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.targetsByID[targetID] = skilltarget.Target{ID: targetID, Code: "self", Enabled: true}
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			TargetID: &targetID,
			FieldSpeedOrder: &skilldetail.FieldSpeedOrder{
				Kind: skilldetail.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 5, ChancePercent: 100,
			},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	application := facts.Sides[0].Members[0].Skills[0].FieldSpeedOrderApplication
	if application == nil || application.Effect.Kind != battleengine.FieldSpeedOrderKindTrickRoom ||
		application.Effect.TurnsRemaining != 5 || application.ChancePercent != 100 {
		t.Fatalf("编译后的全场速度顺序效果 = %+v", application)
	}
}

// TestGameDataFactsReaderCompilesLeechSeed 验证资料层独立的寄生种子规则会在 Battle 启动时编译为纯引擎
// 强类型 application，来源槽位与回合末行为仍由引擎而不是资料文本推断。
func TestGameDataFactsReaderCompilesLeechSeed(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetID := snowflake.NewTestID()
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.targetsByID[targetID] = skilltarget.Target{ID: targetID, Code: "selected-target", Enabled: true}
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			TargetID:  &targetID,
			LeechSeed: &skilldetail.LeechSeed{ChancePercent: 100},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	application := facts.Sides[0].Members[0].Skills[0].LeechSeedApplication
	if application == nil || application.ChancePercent != 100 {
		t.Fatalf("编译后的寄生种子规则 = %+v", application)
	}
}

// TestGameDataFactsReaderCompilesWeather 验证资料层独立的普通天气会在 Battle 启动时编译为纯引擎强类型
// application；日照、降雨、沙暴和降雪的具体结算始终由引擎而不是资料文本决定。
func TestGameDataFactsReaderCompilesWeather(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetID := snowflake.NewTestID()
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.targetsByID[targetID] = skilltarget.Target{ID: targetID, Code: "self", Enabled: true}
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			TargetID: &targetID,
			Weather:  &skilldetail.Weather{Kind: skilldetail.WeatherKindSandstorm, TurnsRemaining: 5, ChancePercent: 100},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	application := facts.Sides[0].Members[0].Skills[0].WeatherApplication
	if application == nil || application.Effect.Kind != battleengine.WeatherKindSandstorm ||
		application.Effect.TurnsRemaining != 5 || application.ChancePercent != 100 {
		t.Fatalf("编译后的普通天气规则 = %+v", application)
	}
}

// TestGameDataFactsReaderCompilesGrassyTerrainWeakeningTag 验证资料层的震动类减伤标记会在 Battle 启动时
// 冻结到纯引擎快照；引擎随后依据全场场地和目标接地状态结算，绝不从资料文本推断。
func TestGameDataFactsReaderCompilesGrassyTerrainWeakeningTag(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			WeakenedByGrassyTerrain: true,
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].Skills[0].WeakenedByGrassyTerrain {
		t.Fatalf("编译后的青草场地震动减伤标记 = %+v", facts.Sides[0].Members[0].Skills[0])
	}
}

// TestGameDataFactsReaderCompilesWeatherAccuracyOverrides 验证实时资料中的天气命中覆盖在 Battle 启动时被复制为
// 纯引擎快照，并保留 0 的必中语义，避免对局运行期重新读取或解释管理端资料。
func TestGameDataFactsReaderCompilesWeatherAccuracyOverrides(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{WeatherAccuracyOverrides: []skilldetail.WeatherAccuracyOverride{{
			Weather: skilldetail.WeatherKindRain, AccuracyPercent: 0,
		}}},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	overrides := facts.Sides[0].Members[0].Skills[0].WeatherAccuracyOverrides
	if len(overrides) != 1 || overrides[0].Weather != battleengine.WeatherKindRain || overrides[0].AccuracyPercent != 0 {
		t.Fatalf("编译后的天气命中覆盖 = %+v", overrides)
	}
}

// TestGameDataFactsReaderCompilesWeatherElementOverrides 验证实时资料中的天气属性覆盖在 Battle 启动时冻结为纯
// 引擎快照，并且覆盖目标必须仍是启用属性；对局运行期不会重新读取或解释管理端资料。
func TestGameDataFactsReaderCompilesWeatherElementOverrides(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{WeatherElementOverrides: []skilldetail.WeatherElementOverride{{
			Weather: skilldetail.WeatherKindRain, ElementID: fixture.elementID,
		}}},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	overrides := facts.Sides[0].Members[0].Skills[0].WeatherElementOverrides
	if len(overrides) != 1 || overrides[0].Weather != battleengine.WeatherKindRain || overrides[0].ElementID != fixture.elementID {
		t.Fatalf("编译后的天气属性覆盖 = %+v", overrides)
	}
	fixture.detail.OptionalValues.WeatherElementOverrides[0].ElementID = snowflake.NewTestID()
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("禁用或不存在的天气属性覆盖目标 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesWeatherPowerMultipliers 验证实时资料中的天气威力倍率在 Battle 启动时复制为纯
// 战斗引擎快照，且之后修改资料数组不会影响已经冻结的对局事实。
func TestGameDataFactsReaderCompilesWeatherPowerMultipliers(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{WeatherPowerMultipliers: []skilldetail.WeatherPowerMultiplier{{
			Weather: skilldetail.WeatherKindRain, Numerator: 3, Denominator: 2,
		}}},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	multipliers := facts.Sides[0].Members[0].Skills[0].WeatherPowerMultipliers
	if len(multipliers) != 1 || multipliers[0].Weather != battleengine.WeatherKindRain || multipliers[0].Numerator != 3 || multipliers[0].Denominator != 2 {
		t.Fatalf("编译后的天气威力倍率 = %+v", multipliers)
	}
	fixture.detail.OptionalValues.WeatherPowerMultipliers[0].Numerator = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效天气威力倍率 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesChargeSkippedWeathers 验证资料层跳过蓄力天气仅在同时具备 charging 易变状态时
// 才能冻结到引擎快照；破坏该交叉约束的实时资料必须阻止 Battle 启动。
func TestGameDataFactsReaderCompilesChargeSkippedWeathers(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	charging := skilldetail.VolatileEffect{
		Status: skilldetail.VolatileStatusCharging, Target: skilldetail.VolatileEffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			VolatileEffects: []skilldetail.VolatileEffect{charging}, ChargeSkippedWeathers: []skilldetail.WeatherKind{skilldetail.WeatherKindSun},
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	weathers := facts.Sides[0].Members[0].Skills[0].ChargeSkippedWeathers
	if len(weathers) != 1 || weathers[0] != battleengine.WeatherKindSun {
		t.Fatalf("编译后的跳过蓄力天气 = %+v", weathers)
	}
	fixture.detail.VolatileEffects = nil
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("缺少 charging 状态的跳过蓄力天气 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilityWeatherDamageImmunities 验证特性详情的天气伤害免疫会在 Battle 启动时冻结到
// 成员快照；禁用特性或损坏的封闭天气资料必须阻止新对局，而不是由引擎依据特性名称猜测行为。
func TestGameDataFactsReaderCompilesAbilityWeatherDamageImmunities(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			WeatherDamageImmunities:  []abilitydetail.WeatherKind{abilitydetail.WeatherKindSandstorm},
			WeatherEffectsSuppressed: true,
			ForcedSwitchImmunity:     true,
			WeatherEndTurnHeal: &abilitydetail.WeatherEndTurnHeal{
				Weathers: []abilitydetail.WeatherKind{abilitydetail.WeatherKindRain}, HealDenominator: 16,
			},
			WeatherSpeedMultipliers: []abilitydetail.WeatherSpeedMultiplier{{
				Weather: abilitydetail.WeatherKindRain, Numerator: 2, Denominator: 1,
			}},
			SwitchInStrongWeather: &abilitydetail.SwitchInStrongWeather{Weather: abilitydetail.StrongWeatherKindHeavyRain},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	immunities := facts.Sides[0].Members[0].WeatherDamageImmunities
	if len(immunities) != 1 || immunities[0] != battleengine.WeatherKindSandstorm {
		t.Fatalf("编译后的特性天气伤害免疫 = %+v", immunities)
	}
	if !facts.Sides[0].Members[0].WeatherEffectsSuppressed {
		t.Fatalf("编译后的特性天气封锁开关 = %+v", facts.Sides[0].Members[0])
	}
	if !facts.Sides[0].Members[0].ForcedSwitchImmunity {
		t.Fatalf("编译后的特性强制换人免疫开关 = %+v", facts.Sides[0].Members[0])
	}
	healing := facts.Sides[0].Members[0].WeatherEndTurnHealing
	if healing == nil || len(healing.Weathers) != 1 || healing.Weathers[0] != battleengine.WeatherKindRain || healing.HealDenominator != 16 {
		t.Fatalf("编译后的特性天气回合末回复 = %+v", healing)
	}
	multipliers := facts.Sides[0].Members[0].WeatherSpeedMultipliers
	if len(multipliers) != 1 || multipliers[0] != (battleengine.WeatherSpeedMultiplier{Weather: battleengine.WeatherKindRain, Numerator: 2, Denominator: 1}) {
		t.Fatalf("编译后的特性天气速度倍率 = %+v", multipliers)
	}
	if actual := facts.Sides[0].Members[0].SwitchInStrongWeather; actual != battleengine.StrongWeatherKindHeavyRain {
		t.Fatalf("编译后的特性入场强天气 = %q", actual)
	}
	fixture.abilityDetail.WeatherDamageImmunities[0] = "fog"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性天气伤害免疫 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.WeatherDamageImmunities[0] = abilitydetail.WeatherKindSandstorm
	fixture.abilityDetail.WeatherEndTurnHeal.Weathers[0] = "fog"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性天气回合末回复天气 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.WeatherEndTurnHeal.Weathers[0] = abilitydetail.WeatherKindRain
	fixture.abilityDetail.WeatherSpeedMultipliers[0].Denominator = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效特性天气速度倍率 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.WeatherSpeedMultipliers[0].Denominator = 1
	fixture.abilityDetail.SwitchInStrongWeather.Weather = "eternalFog"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性入场强天气 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInStrongWeather.Weather = abilitydetail.StrongWeatherKindHeavyRain
	fixture.ability.Enabled = false
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("禁用特性 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.ability.Enabled = true
	fixture.ability.ID = snowflake.NewTestID()
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("缺失特性 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilityOpponentSwitchRestriction 验证特性主动换人限制在启动时解析启用属性，并以独立快照
// 传递给 Battle Engine。损坏的属性引用必须阻止新对局，不能在运行时退化为无条件或无规则。
func TestGameDataFactsReaderCompilesAbilityOpponentSwitchRestriction(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{OpponentSwitchRestriction: &abilitydetail.OpponentSwitchRestriction{
			RequiredTargetElementID: &fixture.elementID, RequiresGroundedTarget: true, SameEffectGrantsImmunity: true,
		}},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].OpponentSwitchRestriction
	if rule == nil || rule.RequiredTargetElementID != fixture.elementID || !rule.RequiresGroundedTarget || !rule.SameEffectGrantsImmunity {
		t.Fatalf("编译后的主动换人限制 = %+v", rule)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0].OpponentSwitchRestriction
	if frozen == nil || frozen == rule || frozen.RequiredTargetElementID != fixture.elementID {
		t.Fatalf("引擎成员中的主动换人限制 = %+v，期望独立快照", frozen)
	}
	rule.RequiredTargetElementID = 0
	if frozen.RequiredTargetElementID != fixture.elementID {
		t.Fatalf("初始状态错误共享主动换人限制指针: frozen=%+v", frozen)
	}

	invalidElementID := snowflake.NewTestID()
	fixture.abilityDetail.OpponentSwitchRestriction.RequiredTargetElementID = &invalidElementID
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未启用属性引用 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchOutRules 验证半血跨越强制自换和成功离场规则会随成员快照一起冻结，
// 且离场形态目标画像在启动期计算完成；引擎运行期不得访问实时资料来补齐任一字段。
func TestGameDataFactsReaderCompilesAbilitySwitchOutRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	alternateCreatureID := appendInitialStateFormTarget(fixture, 100, 600)
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			DamageCrossedHalfHPForceSelfSwitch: true,
			SwitchOutMajorStatusCure:           true,
			SwitchOutHealDenominator:           16,
			SwitchOutFormChange: &abilitydetail.SwitchOutFormChange{
				BaseCreatureID: fixture.creatureID, AlternateCreatureID: alternateCreatureID,
			},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if !member.DamageCrossedHalfHPForceSelfSwitch || !member.SwitchOutMajorStatusCure ||
		member.SwitchOutHealDenominator != 16 || member.SwitchOutFormChange == nil ||
		member.SwitchOutFormChange.BaseCreatureID != fixture.creatureID ||
		member.SwitchOutFormChange.AlternateCreatureID != alternateCreatureID {
		t.Fatalf("编译后的离场与半血规则 = %+v", member)
	}
	if profile, found := formProfileForCreature(member.FormProfiles, alternateCreatureID); !found ||
		profile.Weight != 600 || profile.MaxHP == 0 {
		t.Fatalf("离场形态目标未冻结为完整画像: profiles=%+v", member.FormProfiles)
	}

	fixture.abilityDetail.SwitchOutHealDenominator = 65_536
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("越界离场回复分母 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchOutHealDenominator = 16
	fixture.abilityDetail.SwitchOutFormChange.AlternateCreatureID = snowflake.NewTestID()
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("缺失离场形态目标 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInWeather 验证特性详情的普通入场天气会在 Battle 启动时冻结为
// 引擎独立规则，并拒绝未知天气或无效持续回合，不能被当作技能天气或未声明资料。
func TestGameDataFactsReaderCompilesAbilitySwitchInWeather(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInWeather: &abilitydetail.SwitchInWeather{Weather: abilitydetail.WeatherKindRain, TurnsRemaining: 5},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].SwitchInWeather
	if rule == nil || rule.Effect != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 5}) {
		t.Fatalf("编译后的特性入场普通天气 = %+v", rule)
	}
	fixture.abilityDetail.SwitchInWeather.Weather = "eternalFog"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性入场普通天气 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInWeather.Weather = abilitydetail.WeatherKindRain
	fixture.abilityDetail.SwitchInWeather.TurnsRemaining = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效特性入场普通天气持续回合 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInTerrain 验证特性详情的入场普通场地会在 Battle 启动时冻结为
// 引擎独立规则，并拒绝未知场地或无效持续回合，不能被当作技能场地或未声明资料。
func TestGameDataFactsReaderCompilesAbilitySwitchInTerrain(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInTerrain: &abilitydetail.SwitchInTerrain{Terrain: abilitydetail.TerrainKindGrassy, TurnsRemaining: 5},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].SwitchInTerrain
	if rule == nil || rule.Effect != (battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 5}) {
		t.Fatalf("编译后的特性入场普通场地 = %+v", rule)
	}
	fixture.abilityDetail.SwitchInTerrain.Terrain = "eternalFog"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性入场普通场地 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInTerrain.Terrain = abilitydetail.TerrainKindGrassy
	fixture.abilityDetail.SwitchInTerrain.TurnsRemaining = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效特性入场普通场地持续回合 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInStatStageChange 验证特性详情的入场能力阶级变化会冻结为引擎独立规则，
// 并拒绝未知目标、阶段变化或未启用的能力资料，不能退化为技能命中后的普通能力效果。
func TestGameDataFactsReaderCompilesAbilitySwitchInStatStageChange(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	attackID := snowflake.ID(0)
	for id, value := range fixture.statsByID {
		if value.Code == "attack" {
			attackID = id
			break
		}
	}
	if attackID == snowflake.ID(0) {
		t.Fatal("夹具缺少 attack 能力资料")
	}
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInStatStageChange: &abilitydetail.SwitchInStatStageChange{
				Target: abilitydetail.SwitchInStatStageTargetOpponents, StatID: attackID, StageDelta: -1,
			},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].SwitchInStatStageChange
	if rule == nil || rule.Target != battleengine.SwitchInStatStageTargetOpponents || rule.Stat != battleengine.StatAttack || rule.StageDelta != -1 {
		t.Fatalf("编译后的特性入场能力阶级变化 = %+v", rule)
	}
	fixture.abilityDetail.SwitchInStatStageChange.Target = "everyone"
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("未知特性入场能力阶级目标 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInStatStageChange.Target = abilitydetail.SwitchInStatStageTargetOpponents
	fixture.abilityDetail.SwitchInStatStageChange.StageDelta = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效特性入场能力阶级变化量 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInStatStageChange.StageDelta = -1
	fixture.statsByID[attackID] = stat.Stat{ID: attackID, Code: "attack", Enabled: false}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("禁用特性入场能力资料 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilityTerastallizationRules 验证太晶化特性规则冻结为引擎规则，
// 并拒绝零阶段变化和禁用能力资料，避免新 Battle 在运行期猜测资料含义。
func TestGameDataFactsReaderCompilesAbilityTerastallizationRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	attackID := snowflake.ID(0)
	for id, value := range fixture.statsByID {
		if value.Code == "attack" {
			attackID = id
			break
		}
	}
	if attackID == snowflake.ID(0) {
		t.Fatal("夹具缺少 attack 能力资料")
	}
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			TerastallizationStatStageChange:  &abilitydetail.TerastallizationStatStageChange{StatID: attackID, StageDelta: 1},
			TerastallizationEnvironmentClear: true,
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.TerastallizationStatStageChange == nil || member.TerastallizationStatStageChange.Stat != battleengine.StatAttack ||
		member.TerastallizationStatStageChange.StageDelta != 1 || !member.TerastallizationEnvironmentClear {
		t.Fatalf("编译后的太晶化特性规则 = %+v", member)
	}
	fixture.abilityDetail.TerastallizationStatStageChange.StageDelta = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效太晶化能力阶级变化量 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.TerastallizationStatStageChange.StageDelta = 1
	fixture.statsByID[attackID] = stat.Stat{ID: attackID, Code: "attack", Enabled: false}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("禁用太晶化能力资料 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInAllyHeal 验证特性详情的入场同侧回复会冻结为引擎独立规则，
// 并拒绝零或越界回复分母，不能退化为默认技能、天气或场地回复。
func TestGameDataFactsReaderCompilesAbilitySwitchInAllyHeal(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInAllyHeal: &abilitydetail.SwitchInAllyHeal{HealDenominator: 16},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	rule := facts.Sides[0].Members[0].SwitchInAllyHeal
	if rule == nil || rule.HealDenominator != 16 {
		t.Fatalf("编译后的特性入场同侧回复 = %+v", rule)
	}
	fixture.abilityDetail.SwitchInAllyHeal.HealDenominator = 0
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("无效特性入场同侧回复分母 error = %v，期望 ErrInitialStateCompilation", err)
	}
	fixture.abilityDetail.SwitchInAllyHeal.HealDenominator = 65_536
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("越界特性入场同侧回复分母 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInOpponentDefenseComparisonBoost 验证入场防御比较强化开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInOpponentDefenseComparisonBoost(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInOpponentDefenseComparisonBoost: true},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInOpponentDefenseComparisonBoost {
		t.Fatalf("编译后的特性入场防御比较强化 = false")
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInAllyStatStageCopy 验证入场同侧能力阶级复制开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInAllyStatStageCopy(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInAllyStatStageCopy: true},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInAllyStatStageCopy {
		t.Fatalf("编译后的特性入场同侧能力阶级复制 = false")
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInAllyStatStageReset 验证入场同侧能力阶级重置开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInAllyStatStageReset(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInAllyStatStageReset: true},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInAllyStatStageReset {
		t.Fatalf("编译后的特性入场同侧能力阶级重置 = false")
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInCopyOpponentAbility 验证入场复制对手特性开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInCopyOpponentAbility(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInCopyOpponentAbility: true},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInCopyOpponentAbility {
		t.Fatalf("编译后的特性入场复制对手特性 = false")
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInRevealOpponentHeldItems 验证入场公开对手道具开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInRevealOpponentHeldItems(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInRevealOpponentHeldItems: true, SwitchInRevealOpponentHighestPowerSkill: true,
			SwitchInTransformIntoOpponent: true, SwitchInDetectDangerousOpponentSkill: true, SwitchInDisguiseAsLastHealthyAlly: true,
		},
		Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInRevealOpponentHeldItems {
		t.Fatalf("编译后的特性入场公开对手道具 = false")
	}
	if !facts.Sides[0].Members[0].SwitchInRevealOpponentHighestPowerSkill {
		t.Fatalf("编译后的特性入场公开对手最高威力技能 = false")
	}
	if !facts.Sides[0].Members[0].SwitchInTransformIntoOpponent || !facts.Sides[0].Members[0].SwitchInDetectDangerousOpponentSkill || !facts.Sides[0].Members[0].SwitchInDisguiseAsLastHealthyAlly {
		t.Fatalf("编译后的新增入场特性规则 = %+v", facts.Sides[0].Members[0])
	}
}

// TestGameDataFactsReaderCompilesAbilitySwitchInClearAllSideDamageReductions 验证入场全阵营减伤屏障清除开关会冻结进引擎快照。
func TestGameDataFactsReaderCompilesAbilitySwitchInClearAllSideDamageReductions(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInClearAllSideDamageReductions: true},
		Version:        1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if !facts.Sides[0].Members[0].SwitchInClearAllSideDamageReductions {
		t.Fatalf("编译后的特性入场全阵营减伤屏障清除 = false")
	}
}

// TestGameDataFactsReaderCompilesAbilityFormProfiles 验证形态特性会连同每个引用精灵的数值、体重和属性一起
// 冻结到 Battle 事实，而不是在 Battle Engine 运行时重新读取 Creature Data Projection。
func TestGameDataFactsReaderCompilesAbilityFormProfiles(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetCreatureID := appendInitialStateFormTarget(fixture, 120, 900)
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{
			SwitchInFormChange: &abilitydetail.SwitchInFormChange{
				BaseCreatureID: fixture.creatureID, AlternateCreatureID: targetCreatureID, AddsMaximumHPDifference: true,
			},
			WeatherFormChange: &abilitydetail.WeatherFormChange{
				DefaultCreatureID: fixture.creatureID,
				Targets: []abilitydetail.WeatherFormTarget{{
					Weather: abilitydetail.WeatherKindSun, CreatureID: targetCreatureID,
				}},
			},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.SwitchInFormChange == nil || member.SwitchInFormChange.BaseCreatureID != fixture.creatureID ||
		member.SwitchInFormChange.AlternateCreatureID != targetCreatureID || !member.SwitchInFormChange.AddsMaximumHPDifference {
		t.Fatalf("入场形态规则 = %+v", member.SwitchInFormChange)
	}
	if member.WeatherFormChange == nil || member.WeatherFormChange.DefaultCreatureID != fixture.creatureID ||
		len(member.WeatherFormChange.Targets) != 1 || member.WeatherFormChange.Targets[0].Weather != battleengine.WeatherKindSun ||
		member.WeatherFormChange.Targets[0].CreatureID != targetCreatureID {
		t.Fatalf("天气形态规则 = %+v", member.WeatherFormChange)
	}
	target, found := formProfileForCreature(member.FormProfiles, targetCreatureID)
	if !found || target.MaxHP <= member.MaxHP || target.Weight != 900 || len(target.ElementIDs) != 1 || target.ElementIDs[0] != fixture.elementID {
		t.Fatalf("目标形态画像 = %+v, 全部画像=%+v", target, member.FormProfiles)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	if len(initial.Sides[0].Members[0].FormProfiles) != 2 || initial.Sides[0].Members[0].SwitchInFormChange == nil ||
		initial.Sides[0].Members[0].WeatherFormChange == nil {
		t.Fatalf("引擎形态快照 = %+v", initial.Sides[0].Members[0])
	}
}

// TestGameDataFactsReaderRejectsMissingAbilityFormTarget 验证特性形态规则引用了不存在的精灵时，启动阶段会拒绝
// 新 Battle，而不会把半条规则交给引擎并在实际换入时静默跳过。
func TestGameDataFactsReaderRejectsMissingAbilityFormTarget(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{SwitchInFormChange: &abilitydetail.SwitchInFormChange{
			BaseCreatureID: fixture.creatureID, AlternateCreatureID: snowflake.NewTestID(),
		}},
		Version: 1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望拒绝缺失形态", err)
	}
}

// TestGameDataFactsReaderRejectsDisabledAbilityFormTarget 验证特性形态规则不能冻结被停用的精灵资料。
func TestGameDataFactsReaderRejectsDisabledAbilityFormTarget(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetCreatureID := appendInitialStateFormTarget(fixture, 120, 900)
	fixture.metadata.Creatures[len(fixture.metadata.Creatures)-1].Enabled = false
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{WeatherFormChange: &abilitydetail.WeatherFormChange{
			DefaultCreatureID: fixture.creatureID,
			Targets:           []abilitydetail.WeatherFormTarget{{Weather: abilitydetail.WeatherKindSun, CreatureID: targetCreatureID}},
		}},
		Version: 1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望拒绝禁用形态", err)
	}
}

// TestGameDataFactsReaderCompilesTerrain 验证资料层独立的技能普通场地会在 Battle 启动时编译为纯引擎强类型 application；
// 接地边界和回合末结算仍由引擎而不是资料文本决定。
func TestGameDataFactsReaderCompilesTerrain(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	targetID := snowflake.NewTestID()
	fixture.damageClassCode = "status"
	fixture.skill.Power = nil
	fixture.targetsByID[targetID] = skilltarget.Target{ID: targetID, Code: "self", Enabled: true}
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			TargetID: &targetID,
			Terrain:  &skilldetail.Terrain{Kind: skilldetail.TerrainKindGrassy, TurnsRemaining: 5, ChancePercent: 100},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	application := facts.Sides[0].Members[0].Skills[0].TerrainApplication
	if application == nil || application.Effect.Kind != battleengine.TerrainKindGrassy ||
		application.Effect.TurnsRemaining != 5 || application.ChancePercent != 100 {
		t.Fatalf("编译后的普通场地规则 = %+v", application)
	}
}

// TestGameDataFactsReaderRejectsLeechSeedOnDamagingSkill 验证绕过管理端的伤害技能种子资料会阻止 Battle 启动，
// 防止把目标持续状态误建模为一次攻击的通用附带效果。
func TestGameDataFactsReaderRejectsLeechSeedOnDamagingSkill(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{LeechSeed: &skilldetail.LeechSeed{ChancePercent: 100}},
		Version:        1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望寄生种子资料编译失败", err)
	}
}

// TestGameDataFactsReaderRejectsWeatherOnDamagingSkill 验证绕过管理端写入伤害技能的天气资料会阻止 Battle 启动，
// 防止全场环境效果被误建模为攻击的通用目标附带效果。
func TestGameDataFactsReaderRejectsWeatherOnDamagingSkill(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			Weather: &skilldetail.Weather{Kind: skilldetail.WeatherKindRain, TurnsRemaining: 5, ChancePercent: 100},
		},
		Version: 1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望普通天气资料编译失败", err)
	}
}

// TestGameDataFactsReaderRejectsTerrainOnDamagingSkill 验证绕过管理端写入伤害技能的场地资料会阻止 Battle 启动，
// 防止全场环境效果被误建模为攻击的通用目标附带效果。
func TestGameDataFactsReaderRejectsTerrainOnDamagingSkill(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{
			Terrain: &skilldetail.Terrain{Kind: skilldetail.TerrainKindElectric, TurnsRemaining: 5, ChancePercent: 100},
		},
		Version: 1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望普通场地资料编译失败", err)
	}
}

// TestGameDataFactsReaderRejectsFieldSpeedOrderOnDamagingSkill 验证即使数据库被绕过写入全场速度顺序资料，
// 伤害技能也不能在 Battle 启动时建立它，避免把全场规则误当成一次攻击的目标附加效果。
func TestGameDataFactsReaderRejectsFieldSpeedOrderOnDamagingSkill(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{FieldSpeedOrder: &skilldetail.FieldSpeedOrder{
			Kind: skilldetail.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 5, ChancePercent: 100,
		}},
		Version: 1,
	}
	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); !errors.Is(err, battle.ErrInitialStateCompilation) {
		t.Fatalf("ReadInitialStateFacts() error = %v，期望全场效果资料编译失败", err)
	}
}

// TestGameDataFactsReaderRejectsUnknownVolatileEffect 验证数据库被绕过应用边界写入未知枚举时，
// 启动编译会失败而不是把错误规则静默忽略，防止线上 Battle 用不完整规则运行。
func TestGameDataFactsReaderRejectsUnknownVolatileEffect(t *testing.T) {
	t.Parallel()

	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{VolatileEffects: []skilldetail.VolatileEffect{{
			Status: "unknown", Target: skilldetail.VolatileEffectTargetSelectedTarget, ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}}},
		Version: 1,
	}

	if _, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session); err == nil {
		t.Fatal("ReadInitialStateFacts() 未拒绝未知易变状态")
	}
}

// initialStateDataFixture 集中提供启动读取器测试需要的真实资料领域形状，而不复用生产数据库实现。
func TestGameDataFactsReaderFreezesElementEffectivenessAndAppliesNatureToFinalStats(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	var attackID, speedID snowflake.ID
	for id, value := range fixture.statsByID {
		switch value.Code {
		case "attack":
			attackID = id
		case "speed":
			speedID = id
		}
	}
	fixture.nature = nature.Nature{ID: fixture.member.NatureID, Code: "brave", Name: "勇敢", IncreasedStatID: &attackID, DecreasedStatID: &speedID, Enabled: true, Version: 1}
	fixture.effectiveness = []elementeffectiveness.Effectiveness{{ID: snowflake.NewTestID(), AttackElementID: fixture.elementID, DefenseElementID: fixture.elementID, Numerator: 2, Denominator: 1, Enabled: true, Version: 1}}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	if len(facts.Rules.ElementEffectiveness) != 1 || facts.Rules.ElementEffectiveness[0].Numerator != 2 {
		t.Fatalf("ElementEffectiveness = %+v", facts.Rules.ElementEffectiveness)
	}
	member := facts.Sides[0].Members[0]
	if member.NatureID != fixture.nature.ID || member.Stats.Attack != 145 || member.Stats.Speed != 118 {
		t.Fatalf("Nature compiled member = %+v", member)
	}
}

type initialStateDataFixture struct {
	format        battleformat.Format
	session       battle.Battle
	member        team.Member
	elementID     snowflake.ID
	nature        nature.Nature
	effectiveness []elementeffectiveness.Effectiveness
	damageID      snowflake.ID
	// damageClassCode 是夹具中技能伤害分类的稳定 code；默认 physical，测试可切换为 status 以验证非伤害规则。
	damageClassCode string
	skillID         snowflake.ID
	creatureID      snowflake.ID
	statsByID       map[snowflake.ID]stat.Stat
	metadata        creaturemetadata.Data
	itemRules       itemrules.Projection
	ability         ability.Ability
	abilityDetail   *abilitydetail.RuleSet
	skill           skill.Skill
	detail          *skilldetail.RuleSet
	targetsByID     map[snowflake.ID]skilltarget.Target
	statChanges     []skillstatchange.Change
}

// newInitialStateDataFixture 创建拥有一个完整成员、六项基础能力和一个物理技能的最小实时资料快照。
func newInitialStateDataFixture(t *testing.T, levelRule battleformat.LevelRule) *initialStateDataFixture {
	t.Helper()
	elementID, damageID, skillID, creatureID, natureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	statCodes := []string{"hp", "attack", "defense", "special-attack", "special-defense", "speed"}
	statsByID := make(map[snowflake.ID]stat.Stat, len(statCodes))
	bindings := make([]creaturemetadata.StatBinding, 0, len(statCodes))
	memberStats := make([]team.MemberStat, 0, len(statCodes))
	for _, code := range statCodes {
		id := snowflake.NewTestID()
		statsByID[id] = stat.Stat{ID: id, Code: code, Enabled: true}
		bindings = append(bindings, creaturemetadata.StatBinding{ID: snowflake.NewTestID(), CreatureID: creatureID, StatID: id, BaseValue: 80})
		memberStats = append(memberStats, team.MemberStat{StatID: id, IndividualValue: 31, EffortValue: 252})
	}
	format := battleformat.Format{
		ID: snowflake.NewTestID(), Code: "standard-single", Name: "标准单打", Description: "测试赛制", Mode: battleformat.ModeSingle,
		RosterCount: 1, SelectCount: 1, ActiveParticipantsPerSide: 1, LevelRule: levelRule,
		Deadlines:    battleformat.Deadlines{PreviewSeconds: 30, TurnSeconds: 30, BattleSeconds: 600},
		Availability: battleformat.Availability{Challenge: true, Training: true}, Enabled: true, Version: 1,
	}
	formatSnapshot, err := json.Marshal(format)
	if err != nil {
		t.Fatalf("编码测试 BattleFormat: %v", err)
	}
	member := team.Member{
		Position: 1, CreatureID: creatureID, AbilityID: snowflake.NewTestID(), TeraElementID: elementID, NatureID: natureID, Level: 20,
		Skills: []team.MemberSkill{{Position: 1, SkillID: skillID}}, Stats: memberStats,
	}
	session := battle.Battle{
		ID: snowflake.NewTestID(), Mode: battle.BattleModePvP, SourceType: battle.BattleSourceChallenge, Status: battle.StatusRunning,
		BattleFormatID: format.ID, BattleFormatSnapshot: formatSnapshot,
		Format: battle.Format{RosterCount: 1, SelectCount: 1, ActiveParticipantsPerSide: 1, PreviewDuration: 30 * time.Second, BattleDuration: 10 * time.Minute},
		Participants: []battle.Participant{
			{Side: battle.ParticipantSideOne, Team: battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 1, Members: []team.Member{member}}},
			{Side: battle.ParticipantSideTwo, Team: battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 1, Members: []team.Member{member}}},
		},
		PreviewSubmissions: []battle.PreviewSubmission{
			{Side: battle.ParticipantSideOne, MemberPositions: []int32{1}, ActivePositions: []int32{1}},
			{Side: battle.ParticipantSideTwo, MemberPositions: []int32{1}, ActivePositions: []int32{1}},
		},
	}
	return &initialStateDataFixture{
		format: format, session: session, member: member, elementID: elementID, damageID: damageID, skillID: skillID,
		creatureID: creatureID, statsByID: statsByID,
		nature:  nature.Nature{ID: natureID, Code: "hardy", Name: "勤奋", Enabled: true, Version: 1},
		ability: ability.Ability{ID: member.AbilityID, Code: "test-ability", Name: "测试特性", Enabled: true, Version: 1},
		metadata: creaturemetadata.Data{
			Creatures: []creaturemetadata.Creature{{ID: creatureID, SpeciesID: snowflake.NewTestID(), Weight: int32Pointer(500), Enabled: true}},
			Forms:     []creaturemetadata.Form{{ID: snowflake.NewTestID(), CreatureID: creatureID, DefaultForm: true, Enabled: true, ElementIDs: []snowflake.ID{elementID}}},
			Stats:     bindings,
		},
		skill: skill.Skill{ID: skillID, Code: "tackle", Name: "撞击", OptionalValues: skill.OptionalValues{
			ElementID: &elementID, DamageClassID: &damageID, Power: int32Pointer(40), PP: int32Pointer(35), Accuracy: int32Pointer(100),
		}, Enabled: true},
		damageClassCode: "physical",
		targetsByID:     make(map[snowflake.ID]skilltarget.Target),
	}
}

// appendInitialStateFormTarget 向最小资料夹具加入一只拥有完整默认形态和六项基础数值的可切换目标精灵。
//
// 目标与当前成员共享训练输入，但使用独立的基础数值和体重，以验证 Battle 编译器确实按目标精灵资料计算
// FormProfile，而不是复制原始成员的数值。
func appendInitialStateFormTarget(fixture *initialStateDataFixture, baseValue, weight int32) snowflake.ID {
	creatureID := snowflake.NewTestID()
	fixture.metadata.Creatures = append(fixture.metadata.Creatures, creaturemetadata.Creature{
		ID: creatureID, SpeciesID: snowflake.NewTestID(), Weight: int32Pointer(weight), Enabled: true,
	})
	fixture.metadata.Forms = append(fixture.metadata.Forms, creaturemetadata.Form{
		ID: snowflake.NewTestID(), CreatureID: creatureID, DefaultForm: true, Enabled: true, ElementIDs: []snowflake.ID{fixture.elementID},
	})
	for statID := range fixture.statsByID {
		fixture.metadata.Stats = append(fixture.metadata.Stats, creaturemetadata.StatBinding{
			ID: snowflake.NewTestID(), CreatureID: creatureID, StatID: statID, BaseValue: baseValue,
		})
	}
	return creatureID
}

// formProfileForCreature 按稳定精灵 Identifier 查找一项已经冻结的形态画像。
func formProfileForCreature(profiles []battleengine.FormProfile, creatureID battleengine.Identifier) (battleengine.FormProfile, bool) {
	for _, profile := range profiles {
		if profile.CreatureID == creatureID {
			return profile, true
		}
	}
	return battleengine.FormProfile{}, false
}

// reader 将夹具自身适配为所有只读资料接口，保证测试只关注编译边界。
func (fixture *initialStateDataFixture) reader() *battle.GameDataInitialStateFactsReader {
	return battle.NewGameDataInitialStateFactsReader(
		initialStateFormatStub{fixture}, initialStateElementStub{fixture},
		initialStateElementEffectivenessStub{fixture},
		initialStateAbilityStub{fixture}, initialStateSkillStub{fixture}, initialStateDamageClassStub{fixture},
		initialStateAilmentStub{fixture}, initialStateTargetStub{fixture}, initialStateStatChangeStub{fixture},
		initialStateStatStub{fixture}, initialStateNatureStub{fixture}, initialStateCreatureStub{fixture}, initialStateItemRulesStub{fixture},
	)
}

type initialStateElementEffectivenessStub struct{ fixture *initialStateDataFixture }

func (stub initialStateElementEffectivenessStub) ListEnabled(context.Context) ([]elementeffectiveness.Effectiveness, error) {
	return append([]elementeffectiveness.Effectiveness(nil), stub.fixture.effectiveness...), nil
}

// initialStateFormatStub 适配 BattleFormat 查询接口。
type initialStateFormatStub struct{ fixture *initialStateDataFixture }

func (stub initialStateFormatStub) GetFormat(_ context.Context, id snowflake.ID) (battleformat.Format, error) {
	fixture := stub.fixture
	if id != fixture.format.ID {
		return battleformat.Format{}, battleformat.ErrFormatNotFound
	}
	return fixture.format, nil
}

// initialStateElementStub 适配属性分页查询接口。
type initialStateElementStub struct{ fixture *initialStateDataFixture }

func (stub initialStateElementStub) List(_ context.Context, query element.ListQuery) (element.Page, error) {
	fixture := stub.fixture
	return element.Page{Items: []element.Element{{ID: fixture.elementID, Code: "normal", Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

// initialStateAbilityStub 适配特性主资料查询接口。
type initialStateAbilityStub struct{ fixture *initialStateDataFixture }

func (stub initialStateAbilityStub) Get(_ context.Context, id snowflake.ID) (ability.Ability, error) {
	if id != stub.fixture.ability.ID {
		return ability.Ability{}, ability.ErrAbilityNotFound
	}
	value := stub.fixture.ability
	if stub.fixture.abilityDetail != nil {
		value.Rules, _ = battlerules.NewAbility(stub.fixture.abilityDetail.OptionalValues)
	}
	return value, nil
}

// initialStateSkillStub 适配技能查询接口。
type initialStateSkillStub struct{ fixture *initialStateDataFixture }

func (stub initialStateSkillStub) Get(_ context.Context, id snowflake.ID) (skill.Skill, error) {
	fixture := stub.fixture
	if id != fixture.skillID {
		return skill.Skill{}, skill.ErrSkillNotFound
	}
	value := fixture.skill
	if fixture.detail != nil {
		value.Rules, _ = battlerules.NewSkill(fixture.detail.OptionalValues)
	}
	return value, nil
}

// initialStateDamageClassStub 适配技能伤害分类查询接口。
type initialStateDamageClassStub struct{ fixture *initialStateDataFixture }

func (stub initialStateDamageClassStub) Get(_ context.Context, id snowflake.ID) (skilldamageclass.DamageClass, error) {
	fixture := stub.fixture
	if id != fixture.damageID {
		return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassNotFound
	}
	return skilldamageclass.DamageClass{ID: id, Code: fixture.damageClassCode, Enabled: true}, nil
}

// initialStateAilmentStub 适配技能异常查询接口。
type initialStateAilmentStub struct{ fixture *initialStateDataFixture }

func (initialStateAilmentStub) Get(context.Context, snowflake.ID) (skillailment.Ailment, error) {
	return skillailment.Ailment{}, skillailment.ErrSkillAilmentNotFound
}

// initialStateTargetStub 适配技能目标查询接口。
type initialStateTargetStub struct{ fixture *initialStateDataFixture }

func (stub initialStateTargetStub) Get(_ context.Context, id snowflake.ID) (skilltarget.Target, error) {
	value, found := stub.fixture.targetsByID[id]
	if !found {
		return skilltarget.Target{}, skilltarget.ErrSkillTargetNotFound
	}
	return value, nil
}

// initialStateStatChangeStub 适配技能数值变化查询接口。
type initialStateStatChangeStub struct{ fixture *initialStateDataFixture }

func (stub initialStateStatChangeStub) List(_ context.Context, query skillstatchange.ListQuery) (skillstatchange.Page, error) {
	return skillstatchange.Page{Items: stub.fixture.statChanges, Total: int64(len(stub.fixture.statChanges)), Page: query.Page, PageSize: query.PageSize}, nil
}

// initialStateStatStub 适配数值项查询接口。
type initialStateStatStub struct{ fixture *initialStateDataFixture }

func (stub initialStateStatStub) Get(_ context.Context, id snowflake.ID) (stat.Stat, error) {
	fixture := stub.fixture
	value, found := fixture.statsByID[id]
	if !found {
		return stat.Stat{}, stat.ErrStatNotFound
	}
	return value, nil
}

type initialStateNatureStub struct{ fixture *initialStateDataFixture }

func (stub initialStateNatureStub) Get(_ context.Context, id snowflake.ID) (nature.Nature, error) {
	if id != stub.fixture.nature.ID {
		return nature.Nature{}, nature.ErrNatureNotFound
	}
	return stub.fixture.nature, nil
}

// initialStateCreatureStub 适配复杂精灵资料查询接口。
type initialStateCreatureStub struct{ fixture *initialStateDataFixture }

func (stub initialStateCreatureStub) Get(context.Context) (creaturemetadata.Snapshot, error) {
	return creaturemetadata.Snapshot{Data: stub.fixture.metadata}, nil
}

// initialStateItemRulesStub 适配只在 Battle 启动期读取一次的道具规则资料聚合。
func (stub initialStateItemRulesStub) GetItemRules(context.Context) (itemrules.Projection, error) {
	return stub.fixture.itemRules, nil
}

// initialStateItemRulesStub 将夹具中的完整道具资料投影为启动读取器依赖。
type initialStateItemRulesStub struct{ fixture *initialStateDataFixture }

func int32Pointer(value int32) *int32 { return &value }
