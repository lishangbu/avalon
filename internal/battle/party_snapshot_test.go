package battle

import (
	"encoding/json"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/team"
)

// TestPartyBattleSnapshotJSONAndClone 验证 Encounter Party 冻结事实可稳定持久化且深复制后互不影响。
func TestPartyBattleSnapshotJSONAndClone(t *testing.T) {
	t.Parallel()

	snapshot := PartyBattleSnapshot{
		PartyID: snowflake.MustParse("1048576010"),
		Version: 7,
		Team: TeamSnapshot{SourceTeamID: snowflake.MustParse("1048576010"), SourceTeamVersion: 7, Members: []team.Member{{
			Position: 1, CreatureID: snowflake.MustParse("1048576012"), AbilityID: snowflake.MustParse("1048576013"),
			TeraElementID: snowflake.MustParse("1048576014"), NatureID: snowflake.MustParse("1048576015"), Level: 5,
			Skills: []team.MemberSkill{{Position: 1, SkillID: snowflake.MustParse("1048576016")}},
		}}},
		Members: []PartyBattleSnapshotMember{{
			Position: 1, PlayerCharacterCreatureID: snowflake.MustParse("1048576011"), CurrentHP: 45, MaximumHP: 123,
		}},
		Loot: &EncounterLootSnapshot{
			LootTableID: snowflake.MustParse("1048576017"), LootEntryID: snowflake.MustParse("1048576018"),
			ItemID: snowflake.MustParse("1048576019"), Quantity: 2, RandomAlgorithm: "hmac-sha256-v1",
			EntryDrawNumber: 3, QuantityDrawNumber: 4,
		},
	}
	raw, err := json.Marshal(snapshot)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	var decoded PartyBattleSnapshot
	if err = json.Unmarshal(raw, &decoded); err != nil {
		t.Fatalf("json.Unmarshal() error = %v", err)
	}
	if decoded.PartyID != snapshot.PartyID || decoded.Version != snapshot.Version || decoded.Team.SourceTeamID != snapshot.Team.SourceTeamID ||
		decoded.Team.SourceTeamVersion != snapshot.Team.SourceTeamVersion || len(decoded.Members) != 1 || decoded.Members[0] != snapshot.Members[0] {
		t.Fatalf("decoded snapshot = %+v", decoded)
	}

	cloned := cloneParticipant(Participant{Party: &snapshot})
	cloned.Party.Members[0].MaximumHP = 1
	cloned.Party.Team.Members[0].Position = 2
	cloned.Party.Loot.Quantity = 99
	if snapshot.Members[0].MaximumHP != 123 {
		t.Fatalf("cloneParticipant() 共享了 Party Members 切片")
	}
	if snapshot.Team.Members[0].Position != 1 {
		t.Fatalf("cloneParticipant() 共享了 Party Team 成员切片")
	}
	if snapshot.Loot.Quantity != 2 {
		t.Fatalf("cloneParticipant() 共享了 Encounter Loot 指针")
	}
}

// TestPartyCurrentHPClampsFrozenInstanceHealth 验证 Encounter 起始生命使用冻结实例值且不超过实时计算上限。
func TestPartyCurrentHPClampsFrozenInstanceHealth(t *testing.T) {
	t.Parallel()

	snapshot := &PartyBattleSnapshot{Members: []PartyBattleSnapshotMember{{Position: 1, CurrentHP: 80, MaximumHP: 100}}}
	if current, ok := partyCurrentHP(snapshot, 1, 60); !ok || current != 48 {
		t.Fatalf("partyCurrentHP() = %d, %v; want 48, true", current, ok)
	}
	if _, ok := partyCurrentHP(snapshot, 2, 60); ok {
		t.Fatal("partyCurrentHP() 接受了不存在的位置")
	}
}

// TestEncounterBotDefinitionBuildsRuntimeStrategy 验证野生 Encounter 冻结定义可重建确定性运行时策略。
func TestEncounterBotDefinitionBuildsRuntimeStrategy(t *testing.T) {
	t.Parallel()

	definition := BotStrategyDefinition{SchemaVersion: 1, DisplayName: "野生对手", Planner: BotPlannerDefinition{Kind: "first_available", FallbackKind: "first_available"}, Generator: BotTeamGeneratorDefinition{Kind: "template", Members: []team.Member{{Position: 1, CreatureID: snowflake.NewTestID(), AbilityID: snowflake.NewTestID(), TeraElementID: snowflake.NewTestID(), NatureID: snowflake.NewTestID(), Level: 5, Skills: []team.MemberSkill{{Position: 1, SkillID: snowflake.NewTestID()}}}}}, Budget: BotDecisionBudget{MaxMembers: 6, MaxSkillsPerMember: 4, MaxDecisionMillis: 50}}
	raw, err := json.Marshal(definition)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	_, canonical, err := DecodeBotStrategyDefinition(raw)
	if err != nil {
		t.Fatalf("DecodeBotStrategyDefinition() error = %v", err)
	}
	strategy, err := NewBotStrategyFromFrozenDefinition(Participant{IsBot: true, BotCode: "wild-encounter", BotStrategyVersion: 1, BotDefinition: canonical})
	if err != nil || strategy.Code() != "wild-encounter" || strategy.Version() != 1 {
		t.Fatalf("NewBotStrategyFromFrozenDefinition() = %v, %v", strategy, err)
	}
}
