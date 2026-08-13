package creaturemetadata_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
)

func TestValidateReferencesRejectsNonReadySkinAsset(t *testing.T) {
	t.Parallel()
	assetID := snowflake.NewTestID()
	service := creaturemetadata.NewService(&creatureMetadataStoreStub{replaced: creaturemetadata.Snapshot{Data: creaturemetadata.Data{
		Skins: []creaturemetadata.Skin{{ID: snowflake.NewTestID(), CreatureID: snowflake.NewTestID(), Code: "default", Name: "默认", AssetID: &assetID, Enabled: true}},
	}}})
	err := service.ValidateReferences(context.Background(), creatureReferenceCatalogStub{})
	if !errors.Is(err, creaturemetadata.ErrInvalidCreatureMetadataReference) {
		t.Fatalf("ValidateReferences() error = %v, want invalid external reference", err)
	}
}

// TestValidateReferencesLooksUpEachExternalIdentityOnce 验证大体量关系中重复引用相同外部资料时只查询一次，
// 使完整维护校验的成本随唯一资料身份增长，而不是随十万级技能学习关系线性执行数据库往返。
func TestValidateReferencesLooksUpEachExternalIdentityOnce(t *testing.T) {
	t.Parallel()
	elementID, statID, skillID, methodID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	abilityID, itemID, assetID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	service := creaturemetadata.NewService(&creatureMetadataStoreStub{replaced: creaturemetadata.Snapshot{Data: creaturemetadata.Data{
		Forms: []creaturemetadata.Form{
			{ElementIDs: []snowflake.ID{elementID}}, {ElementIDs: []snowflake.ID{elementID}},
		},
		Stats: []creaturemetadata.StatBinding{{StatID: statID}, {StatID: statID}},
		SkillLearns: []creaturemetadata.SkillLearn{
			{SkillID: skillID, LearnMethodID: methodID}, {SkillID: skillID, LearnMethodID: methodID},
		},
		Abilities: []creaturemetadata.AbilityBinding{{AbilityID: abilityID}, {AbilityID: abilityID}},
		HeldItems: []creaturemetadata.HeldItem{{ItemID: itemID}, {ItemID: itemID}},
		Skins:     []creaturemetadata.Skin{{AssetID: &assetID}, {AssetID: &assetID}},
	}}})
	catalog := &countingCreatureReferenceCatalog{calls: make(map[string]int)}
	if err := service.ValidateReferences(context.Background(), catalog); err != nil {
		t.Fatalf("ValidateReferences() error = %v", err)
	}
	for _, kind := range []string{"element", "stat", "skill", "method", "ability", "item", "asset"} {
		if catalog.calls[kind] != 1 {
			t.Errorf("%s lookup count = %d, want 1", kind, catalog.calls[kind])
		}
	}
}

type creatureReferenceCatalogStub struct{}

func (creatureReferenceCatalogStub) ElementEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) StatEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) SkillEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) SkillLearnMethodEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) AbilityEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) ItemEnabled(context.Context, snowflake.ID) (bool, error) {
	return true, nil
}
func (creatureReferenceCatalogStub) AssetReady(context.Context, snowflake.ID) (bool, error) {
	return false, nil
}

// countingCreatureReferenceCatalog 记录每类外部资料实际查询次数并统一返回启用。
type countingCreatureReferenceCatalog struct{ calls map[string]int }

func (catalog *countingCreatureReferenceCatalog) enabled(kind string) (bool, error) {
	catalog.calls[kind]++
	return true, nil
}
func (catalog *countingCreatureReferenceCatalog) ElementEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("element")
}
func (catalog *countingCreatureReferenceCatalog) StatEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("stat")
}
func (catalog *countingCreatureReferenceCatalog) SkillEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("skill")
}
func (catalog *countingCreatureReferenceCatalog) SkillLearnMethodEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("method")
}
func (catalog *countingCreatureReferenceCatalog) AbilityEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("ability")
}
func (catalog *countingCreatureReferenceCatalog) ItemEnabled(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("item")
}
func (catalog *countingCreatureReferenceCatalog) AssetReady(context.Context, snowflake.ID) (bool, error) {
	return catalog.enabled("asset")
}

type creatureMetadataStoreStub struct {
	replaced creaturemetadata.Snapshot
}

func (s *creatureMetadataStoreStub) GetCreatureMetadata(context.Context) (creaturemetadata.Snapshot, error) {
	return s.replaced, nil
}
