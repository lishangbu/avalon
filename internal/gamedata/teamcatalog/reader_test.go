package teamcatalog_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/gamedata/teamcatalog"
)

func TestReaderBuildsCurrentCatalog(t *testing.T) {
	t.Parallel()

	source := newSourceStub()
	reader := newReader(source)
	catalog, err := reader.Current(context.Background())
	if err != nil {
		t.Fatalf("Current() error = %v", err)
	}
	if len(catalog.Elements) != 1 || catalog.Elements[0].ID != source.elementID ||
		len(catalog.Abilities) != 1 || catalog.Abilities[0].ID != source.abilityID ||
		len(catalog.Items) != 1 || catalog.Items[0].ID != source.itemID ||
		len(catalog.Skills) != 1 || catalog.Skills[0].ID != source.skillID ||
		len(catalog.Stats) != 1 || catalog.Stats[0].ID != source.statID ||
		len(catalog.CreatureMetadata.Creatures) != 1 {
		t.Fatalf("Current() catalog = %+v", catalog)
	}
}

// sourceStub 同时实现各资料领域的只读端口，集中记录测试所需的确定性资料。
type sourceStub struct {
	// 以下字段分别是各独立资料类型的稳定身份。
	elementID snowflake.ID
	abilityID snowflake.ID
	itemID    snowflake.ID
	skillID   snowflake.ID
	statID    snowflake.ID
	natureID  snowflake.ID
	creature  creaturemetadata.Creature
}

func newSourceStub() *sourceStub {
	return &sourceStub{
		elementID: snowflake.NewTestID(), abilityID: snowflake.NewTestID(), itemID: snowflake.NewTestID(), skillID: snowflake.NewTestID(), statID: snowflake.NewTestID(), natureID: snowflake.NewTestID(),
		creature: creaturemetadata.Creature{ID: snowflake.NewTestID(), SpeciesID: snowflake.NewTestID(), Enabled: true},
	}
}

func newReader(source *sourceStub) *teamcatalog.Reader {
	return teamcatalog.NewReader(
		source,
		abilityQueryStub{source: source},
		itemQueryStub{source: source},
		skillQueryStub{source: source},
		statQueryStub{source: source},
		natureQueryStub{source: source},
		creatureReaderStub{source: source},
	)
}

type natureQueryStub struct{ source *sourceStub }

func (stub natureQueryStub) List(_ context.Context, query nature.ListQuery) (nature.Page, error) {
	return nature.Page{Items: []nature.Nature{{ID: stub.source.natureID, Code: "hardy", Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

// creatureQueryStub 将复杂精灵资料查询适配到独立的 Get 方法签名。
type creatureReaderStub struct{ source *sourceStub }

func (stub creatureReaderStub) Get(context.Context) (creaturemetadata.Snapshot, error) {
	return creaturemetadata.Snapshot{Data: creaturemetadata.Data{Creatures: []creaturemetadata.Creature{stub.source.creature}}}, nil
}

func (stub *sourceStub) List(_ context.Context, query element.ListQuery) (element.Page, error) {
	return element.Page{Items: []element.Element{{ID: stub.elementID, Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *sourceStub) ListAbilities(_ context.Context, query ability.ListQuery) (ability.Page, error) {
	return ability.Page{Items: []ability.Ability{{ID: stub.abilityID, Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *sourceStub) ListItems(_ context.Context, query item.ListQuery) (item.Page, error) {
	return item.Page{Items: []item.Item{{ID: stub.itemID, Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *sourceStub) ListSkills(_ context.Context, query skill.ListQuery) (skill.Page, error) {
	return skill.Page{Items: []skill.Skill{{ID: stub.skillID, Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *sourceStub) ListStats(_ context.Context, query stat.ListQuery) (stat.Page, error) {
	return stat.Page{Items: []stat.Stat{{ID: stub.statID, Enabled: true}}, Total: 1, Page: query.Page, PageSize: query.PageSize}, nil
}

// abilityQueryStub 解决不同资料领域均使用 List 方法时 Go 无法在一个类型上重载的问题。
type abilityQueryStub struct{ source *sourceStub }

func (stub abilityQueryStub) List(ctx context.Context, query ability.ListQuery) (ability.Page, error) {
	return stub.source.ListAbilities(ctx, query)
}

// itemQueryStub 为道具列表提供独立接口适配器。
type itemQueryStub struct{ source *sourceStub }

func (stub itemQueryStub) List(ctx context.Context, query item.ListQuery) (item.Page, error) {
	return stub.source.ListItems(ctx, query)
}

// skillQueryStub 为技能列表提供独立接口适配器。
type skillQueryStub struct{ source *sourceStub }

func (stub skillQueryStub) List(ctx context.Context, query skill.ListQuery) (skill.Page, error) {
	return stub.source.ListSkills(ctx, query)
}

// statQueryStub 为数值项列表提供独立接口适配器。
type statQueryStub struct{ source *sourceStub }

func (stub statQueryStub) List(ctx context.Context, query stat.ListQuery) (stat.Page, error) {
	return stub.source.ListStats(ctx, query)
}
