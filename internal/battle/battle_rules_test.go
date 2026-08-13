package battle

import (
	"context"
	"encoding/json"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/team"
)

// TestBattleFormatRuleCompilerRejectsDuplicateSpeciesAtTeamAdmission 验证 Clause 在创建 Challenge 或
// Training 前就拒绝重复精灵，而不是等到 Preview 或对战启动后才报告错误。
func TestBattleFormatRuleCompilerRejectsDuplicateSpeciesAtTeamAdmission(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("NewDefaultRegistry() error = %v", err)
	}
	clauseID, creatureID, alternateCreatureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	speciesID := snowflake.NewTestID()
	compiler := NewBattleFormatRuleCompiler(battleRuleComponentStub{clauses: map[snowflake.ID]battleformat.Clause{
		clauseID: {ID: clauseID, Enabled: true, Definition: effect.Definition{
			Kind: effect.KindUniqueSpeciesClause, SchemaVersion: 1, Parameters: json.RawMessage(`{}`),
		}},
	}}, battleRuleCatalogStub{catalog: team.ReferenceCatalog{CreatureMetadata: creaturemetadata.Data{
		Creatures: []creaturemetadata.Creature{
			{ID: creatureID, SpeciesID: speciesID, Enabled: true},
			{ID: alternateCreatureID, SpeciesID: speciesID, Enabled: true},
		},
	}}}, registry)

	err = compiler.ValidateTeam(context.Background(), battleformat.Format{ID: snowflake.NewTestID(), ClauseIDs: []snowflake.ID{clauseID}}, team.Team{
		Members: []team.Member{{CreatureID: creatureID}, {CreatureID: alternateCreatureID}},
	})
	if !errors.Is(err, ErrBattleFormatTeamRuleViolation) {
		t.Fatalf("ValidateTeam() error = %v, want ErrBattleFormatTeamRuleViolation", err)
	}
}

// TestBattleFormatRuleCompilerRejectsDeniedStableCode 验证名单限制按实时 Stable Code 而不是不透明 Identifier
// 判断，从而在资料重建后仍能保持确定的赛制语义。
func TestBattleFormatRuleCompilerRejectsDeniedStableCode(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("NewDefaultRegistry() error = %v", err)
	}
	restrictionID, creatureID := snowflake.NewTestID(), snowflake.NewTestID()
	compiler := NewBattleFormatRuleCompiler(battleRuleComponentStub{restrictions: map[snowflake.ID]battleformat.Restriction{
		restrictionID: {ID: restrictionID, Enabled: true, Definition: effect.Definition{
			Kind: effect.KindStableCodeListRestriction, SchemaVersion: 1,
			Parameters: json.RawMessage(`{"mode":"deny","resourceType":"creature","stableCodes":["bulbasaur"]}`),
		}},
	}}, battleRuleCatalogStub{catalog: team.ReferenceCatalog{CreatureMetadata: creaturemetadata.Data{
		Creatures: []creaturemetadata.Creature{{ID: creatureID, Code: "bulbasaur", Enabled: true}},
	}}}, registry)

	err = compiler.ValidateTeam(context.Background(), battleformat.Format{ID: snowflake.NewTestID(), RestrictionIDs: []snowflake.ID{restrictionID}}, team.Team{
		Members: []team.Member{{CreatureID: creatureID}},
	})
	if !errors.Is(err, ErrBattleFormatTeamRuleViolation) {
		t.Fatalf("ValidateTeam() error = %v, want ErrBattleFormatTeamRuleViolation", err)
	}
}

// TestBattleFormatRuleCompilerFreezesLevelNormalizationMechanic 验证特殊机制被编译为强类型快照，
// 并且必须与 BattleFormat 的显式等级规则保持相同语义，拒绝二义配置。
func TestBattleFormatRuleCompilerFreezesLevelNormalizationMechanic(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("NewDefaultRegistry() error = %v", err)
	}
	mechanicID := snowflake.NewTestID()
	compiler := NewBattleFormatRuleCompiler(battleRuleComponentStub{mechanics: map[snowflake.ID]battleformat.Mechanic{
		mechanicID: {ID: mechanicID, Enabled: true, Definition: effect.Definition{
			Kind: effect.KindLevelNormalizationMechanic, SchemaVersion: 1, Parameters: json.RawMessage(`{"level":50}`),
		}},
	}}, nil, registry)
	level := int32(50)
	rules, err := compiler.CompileRules(context.Background(), battleformat.Format{
		ID: snowflake.NewTestID(), MechanicIDs: []snowflake.ID{mechanicID}, LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: &level},
	})
	if err != nil {
		t.Fatalf("CompileRules() error = %v", err)
	}
	if rules.NormalizedLevel != 50 {
		t.Fatalf("NormalizedLevel = %d, want 50", rules.NormalizedLevel)
	}
	level = 100
	if _, err := compiler.CompileRules(context.Background(), battleformat.Format{
		ID: snowflake.NewTestID(), MechanicIDs: []snowflake.ID{mechanicID}, LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: &level},
	}); !errors.Is(err, ErrBattleFormatRuleCompilation) {
		t.Fatalf("mismatched CompileRules() error = %v, want ErrBattleFormatRuleCompilation", err)
	}
}

// TestBattleFormatRuleCompilerFreezesTerastallizationMechanic 验证赛制特殊机制被编译为引擎命令校验所需的明确开关。
func TestBattleFormatRuleCompilerFreezesTerastallizationMechanic(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("NewDefaultRegistry() error = %v", err)
	}
	mechanicID := snowflake.NewTestID()
	compiler := NewBattleFormatRuleCompiler(battleRuleComponentStub{mechanics: map[snowflake.ID]battleformat.Mechanic{
		mechanicID: {ID: mechanicID, Enabled: true, Definition: effect.Definition{
			Kind: effect.KindTerastallizationMechanic, SchemaVersion: 1, Parameters: json.RawMessage(`{}`),
		}},
	}}, nil, registry)
	rules, err := compiler.CompileRules(context.Background(), battleformat.Format{ID: snowflake.NewTestID(), MechanicIDs: []snowflake.ID{mechanicID}})
	if err != nil {
		t.Fatalf("CompileRules() error = %v", err)
	}
	if !rules.TerastallizationEnabled {
		t.Fatalf("TerastallizationEnabled = false，期望冻结赛制许可")
	}
}

// battleRuleComponentStub 为规则编译测试提供独立的规则组件查询。
type battleRuleComponentStub struct {
	// clauses 按 Identifier 保存 Clause 测试资料。
	clauses map[snowflake.ID]battleformat.Clause
	// restrictions 按 Identifier 保存 Restriction 测试资料。
	restrictions map[snowflake.ID]battleformat.Restriction
	// mechanics 按 Identifier 保存 Mechanic 测试资料。
	mechanics map[snowflake.ID]battleformat.Mechanic
}

// GetClause 返回预设 Clause。
func (stub battleRuleComponentStub) GetClause(_ context.Context, id snowflake.ID) (battleformat.Clause, error) {
	return stub.clauses[id], nil
}

// GetRestriction 返回预设 Restriction。
func (stub battleRuleComponentStub) GetRestriction(_ context.Context, id snowflake.ID) (battleformat.Restriction, error) {
	return stub.restrictions[id], nil
}

// GetMechanic 返回预设 Mechanic。
func (stub battleRuleComponentStub) GetMechanic(_ context.Context, id snowflake.ID) (battleformat.Mechanic, error) {
	return stub.mechanics[id], nil
}

// battleRuleCatalogStub 为名单限制测试提供确定性实时资料目录。
type battleRuleCatalogStub struct {
	// catalog 是要返回的同修订资料目录。
	catalog team.ReferenceCatalog
}

// Current 返回预设资料目录。
func (stub battleRuleCatalogStub) Current(context.Context) (team.ReferenceCatalog, error) {
	return stub.catalog, nil
}
