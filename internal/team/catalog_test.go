package team_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/team"
)

func TestCatalogValidatorAcceptsCurrentLiveReferences(t *testing.T) {
	t.Parallel()

	creatureID, speciesID, formID, genderID, skinID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	abilityID, skillID, itemID, elementID, statID, natureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	validator := team.NewCatalogValidator(&catalogReaderStub{catalog: team.ReferenceCatalog{
		Abilities: []team.Reference{{ID: abilityID, Enabled: true}},
		Skills:    []team.Reference{{ID: skillID, Enabled: true}},
		Items:     []team.Reference{{ID: itemID, Enabled: true}},
		Elements:  []team.Reference{{ID: elementID, Enabled: true}},
		Stats:     []team.Reference{{ID: statID, Enabled: true}},
		Natures:   []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Genders: []creaturemetadata.Gender{{ID: genderID, Code: "female", Enabled: true}},
			Species: []creaturemetadata.Species{{ID: speciesID, Enabled: true}},
			Creatures: []creaturemetadata.Creature{{
				ID: creatureID, SpeciesID: speciesID, GenderRatio: &creaturemetadata.GenderRatio{FemaleEighths: 8}, Enabled: true,
			}},
			Forms:     []creaturemetadata.Form{{ID: formID, CreatureID: creatureID, Enabled: true}},
			Skins:     []creaturemetadata.Skin{{ID: skinID, CreatureID: creatureID, Enabled: true}},
			Stats:     []creaturemetadata.StatBinding{{ID: snowflake.NewTestID(), CreatureID: creatureID, StatID: statID}},
			Abilities: []creaturemetadata.AbilityBinding{{ID: snowflake.NewTestID(), CreatureID: creatureID, AbilityID: abilityID}},
			SkillLearns: []creaturemetadata.SkillLearn{{
				ID: snowflake.NewTestID(), CreatureID: creatureID, SkillID: skillID, LearnMethodID: snowflake.NewTestID(),
			}},
		},
	}})
	err := validator.ValidateCurrent(context.Background(), []team.Member{{
		CreatureID: creatureID, FormID: &formID, GenderID: &genderID, SkinID: &skinID,
		AbilityID: abilityID, ItemID: &itemID, TeraElementID: elementID, NatureID: natureID,
		Skills: []team.MemberSkill{{Position: 1, SkillID: skillID}},
		Stats:  []team.MemberStat{{StatID: statID, IndividualValue: 31, EffortValue: 252}},
	}})
	if err != nil {
		t.Fatalf("ValidateCurrent() error = %v", err)
	}
}

// TestCatalogValidatorAcceptsGenderPresentInSpeciesRatio 验证成员可以选择种族性别比率中占比非零的性别。
func TestCatalogValidatorAcceptsGenderPresentInSpeciesRatio(t *testing.T) {
	t.Parallel()

	creatureID, speciesID, genderID, abilityID, elementID, natureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	validator := team.NewCatalogValidator(&catalogReaderStub{catalog: team.ReferenceCatalog{
		Abilities: []team.Reference{{ID: abilityID, Enabled: true}},
		Elements:  []team.Reference{{ID: elementID, Enabled: true}},
		Natures:   []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Genders: []creaturemetadata.Gender{{ID: genderID, Code: "female", Enabled: true}},
			Species: []creaturemetadata.Species{{ID: speciesID, Enabled: true}},
			Creatures: []creaturemetadata.Creature{{
				ID: creatureID, SpeciesID: speciesID,
				GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: 4, FemaleEighths: 4}, Enabled: true,
			}},
			Abilities: []creaturemetadata.AbilityBinding{{ID: snowflake.NewTestID(), CreatureID: creatureID, AbilityID: abilityID}},
		},
	}})

	issues, err := validator.CheckCurrent(context.Background(), []team.Member{{
		Position: 1, CreatureID: creatureID, GenderID: &genderID, AbilityID: abilityID, TeraElementID: elementID, NatureID: natureID,
	}})
	if err != nil {
		t.Fatalf("CheckCurrent() error = %v", err)
	}
	if len(issues) != 0 {
		t.Fatalf("CheckCurrent() issues = %+v，期望允许性别比率中占比非零的雌性", issues)
	}
}

func TestCatalogValidatorRejectsGenderAndStatFromAnotherCreature(t *testing.T) {
	t.Parallel()

	creatureID, otherCreatureID := snowflake.NewTestID(), snowflake.NewTestID()
	speciesID, otherSpeciesID := snowflake.NewTestID(), snowflake.NewTestID()
	genderID, fixedGenderID, statID, abilityID, elementID, natureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	validator := team.NewCatalogValidator(&catalogReaderStub{catalog: team.ReferenceCatalog{
		Abilities: []team.Reference{{ID: abilityID, Enabled: true}},
		Elements:  []team.Reference{{ID: elementID, Enabled: true}},
		Stats:     []team.Reference{{ID: statID, Enabled: true}},
		Natures:   []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Genders: []creaturemetadata.Gender{{ID: genderID, Code: "female", Enabled: true}, {ID: fixedGenderID, Code: "male", Enabled: true}},
			Species: []creaturemetadata.Species{
				{ID: speciesID, Enabled: true},
				{ID: otherSpeciesID, Enabled: true},
			},
			Creatures: []creaturemetadata.Creature{
				{ID: creatureID, SpeciesID: speciesID, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: 8}, Enabled: true},
				{ID: otherCreatureID, SpeciesID: otherSpeciesID, GenderRatio: &creaturemetadata.GenderRatio{FemaleEighths: 8}, Enabled: true},
			},
			Stats: []creaturemetadata.StatBinding{{ID: snowflake.NewTestID(), CreatureID: otherCreatureID, StatID: statID}},
			Abilities: []creaturemetadata.AbilityBinding{{
				ID: snowflake.NewTestID(), CreatureID: creatureID, AbilityID: abilityID,
			}},
		},
	}})
	issues, err := validator.CheckCurrent(context.Background(), []team.Member{{
		Position: 1, CreatureID: creatureID, GenderID: &genderID, AbilityID: abilityID, TeraElementID: elementID, NatureID: natureID,
		Stats: []team.MemberStat{{StatID: statID}},
	}})
	if err != nil {
		t.Fatalf("CheckCurrent() error = %v", err)
	}
	if len(issues) != 2 || issues[0].Field != "genderId" || issues[1].Field != "stats.statId" {
		t.Fatalf("CheckCurrent() issues = %+v", issues)
	}
}

func TestCatalogValidatorReportsEveryIncompatibleMemberField(t *testing.T) {
	t.Parallel()

	creatureID := snowflake.NewTestID()
	missingAbilityID := snowflake.NewTestID()
	missingSkillID := snowflake.NewTestID()
	natureID := snowflake.NewTestID()
	validator := team.NewCatalogValidator(&catalogReaderStub{catalog: team.ReferenceCatalog{
		Elements: []team.Reference{{ID: snowflake.NewTestID(), Enabled: true}},
		Natures:  []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Creatures: []creaturemetadata.Creature{{ID: creatureID, Enabled: true}},
		},
	}})
	issues, err := validator.CheckCurrent(context.Background(), []team.Member{{
		Position: 2, CreatureID: creatureID, AbilityID: missingAbilityID,
		TeraElementID: snowflake.NewTestID(), NatureID: natureID, Skills: []team.MemberSkill{{Position: 1, SkillID: missingSkillID}},
	}})
	if err != nil {
		t.Fatalf("CheckCurrent() error = %v", err)
	}
	if len(issues) != 3 || issues[0].MemberPosition != 2 || issues[0].Field != "abilityId" ||
		issues[1].Field != "teraElementId" || issues[2].Field != "skillIds" {
		t.Fatalf("CheckCurrent() issues = %+v", issues)
	}
}

// TestCatalogValidatorWrapsCurrentGameDataCompatibilityIssues 验证当前实时资料拒绝 Team 时，
// 调用方既能继续通过领域哨兵分支，也能取得逐字段的结构化兼容问题。
func TestCatalogValidatorWrapsCurrentGameDataCompatibilityIssues(t *testing.T) {
	t.Parallel()

	creatureID := snowflake.NewTestID()
	abilityID := snowflake.NewTestID()
	elementID := snowflake.NewTestID()
	missingSkillID := snowflake.NewTestID()
	natureID := snowflake.NewTestID()
	validator := team.NewCatalogValidator(&catalogReaderStub{catalog: team.ReferenceCatalog{
		Abilities: []team.Reference{{ID: abilityID, Enabled: true}},
		Elements:  []team.Reference{{ID: elementID, Enabled: true}},
		Natures:   []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Creatures: []creaturemetadata.Creature{{ID: creatureID, Enabled: true}},
			Abilities: []creaturemetadata.AbilityBinding{{
				ID: snowflake.NewTestID(), CreatureID: creatureID, AbilityID: abilityID,
			}},
		},
	}})

	err := validator.ValidateCurrent(context.Background(), []team.Member{{
		Position: 2, CreatureID: creatureID, AbilityID: abilityID, TeraElementID: elementID, NatureID: natureID,
		Skills: []team.MemberSkill{{Position: 1, SkillID: missingSkillID}},
	}})
	if !errors.Is(err, team.ErrTeamReferenceInvalid) {
		t.Fatalf("ValidateCurrent() error = %v，期望保持 ErrTeamReferenceInvalid", err)
	}
	var compatibility *team.CompatibilityError
	if !errors.As(err, &compatibility) {
		t.Fatalf("ValidateCurrent() error = %T，期望包含 CompatibilityError", err)
	}
	issues := compatibility.Issues()
	if len(issues) != 1 || issues[0].MemberPosition != 2 || issues[0].Field != "skillIds" ||
		issues[0].Code != "reference_unavailable" || issues[0].ReferenceID != missingSkillID {
		t.Fatalf("CompatibilityError issues = %+v", issues)
	}
}

// TestCatalogValidatorPreservesUnexpectedReferenceReaderFailure 验证读取实时资料时发生的数据库、解码或
// Context 等基础设施故障不会被伪装为客户端可重试的资料冲突；传输层必须将其记录并返回服务器错误。
func TestCatalogValidatorPreservesUnexpectedReferenceReaderFailure(t *testing.T) {
	t.Parallel()

	want := errors.New("读取实时资料的数据库连接中断")
	validator := team.NewCatalogValidator(&catalogReaderStub{err: want})
	_, err := validator.CheckCurrent(context.Background(), nil)
	if !errors.Is(err, want) {
		t.Fatalf("CheckCurrent() error = %v，期望保留原始读取错误", err)
	}
	if errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("CheckCurrent() error = %v，不应错误映射为 ErrTeamCatalogUnavailable", err)
	}
}

// catalogReaderStub 为领域校验器提供确定性的实时资料快照，避免单元测试依赖数据库。
type catalogReaderStub struct {
	// catalog 是当前全局资料修订下的引用目录。
	catalog team.ReferenceCatalog
	// err 是读取目录时返回的模拟基础设施错误。
	err error
}

func (s *catalogReaderStub) Current(context.Context) (team.ReferenceCatalog, error) {
	return s.catalog, s.err
}
