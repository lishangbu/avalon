// Package teamcatalog 将各类实时游戏资料投影为 Team 领域所需的最小只读引用目录。
//
// 本包只负责跨资料类型的一致性读取，不合并各资料领域的写模型，也不提供通用 CRUD 抽象。
package teamcatalog

import (
	"context"

	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/team"
)

const referencePageSize int32 = 100

// ElementQuery 提供 Team 校验所需的属性资料分页查询。
type ElementQuery interface {
	List(context.Context, element.ListQuery) (element.Page, error)
}

// AbilityQuery 提供 Team 校验所需的特性资料分页查询。
type AbilityQuery interface {
	List(context.Context, ability.ListQuery) (ability.Page, error)
}

// ItemQuery 提供 Team 校验所需的道具资料分页查询。
type ItemQuery interface {
	List(context.Context, item.ListQuery) (item.Page, error)
}

// SkillQuery 提供 Team 校验所需的技能资料分页查询。
type SkillQuery interface {
	List(context.Context, skill.ListQuery) (skill.Page, error)
}

// StatQuery 提供 Team 校验所需的数值项资料分页查询。
type StatQuery interface {
	List(context.Context, stat.ListQuery) (stat.Page, error)
}

// NatureQuery 提供 Team 校验所需的 Nature 资料分页查询。
type NatureQuery interface {
	List(context.Context, nature.ListQuery) (nature.Page, error)
}

// CreatureMetadataReader 读取精灵主体、形态和可用关系的完整实时快照。
type CreatureMetadataReader interface {
	Get(context.Context) (creaturemetadata.Snapshot, error)
}

// Reader 按请求组装 Team 所需的当前启用引用目录。
type Reader struct {
	// elements 提供属性引用。
	elements ElementQuery
	// abilities 提供特性引用。
	abilities AbilityQuery
	// items 提供道具引用。
	items ItemQuery
	// skills 提供技能引用。
	skills SkillQuery
	// stats 提供数值项引用。
	stats StatQuery
	// natures 提供 Nature 引用。
	natures NatureQuery
	// creatureMetadata 提供精灵及其关系的独立复杂资料结构。
	creatureMetadata CreatureMetadataReader
}

// NewReader 使用各资料领域的显式查询服务创建 Team 实时目录读取器。
func NewReader(
	elements ElementQuery,
	abilities AbilityQuery,
	items ItemQuery,
	skills SkillQuery,
	stats StatQuery,
	natures NatureQuery,
	creatureMetadata CreatureMetadataReader,
) *Reader {
	return &Reader{
		elements: elements, abilities: abilities, items: items,
		skills: skills, stats: stats, natures: natures, creatureMetadata: creatureMetadata,
	}
}

// Current 读取全部启用引用。部署方在停机维护期间修改资料，因此在线请求无需维护门禁或全局修订重试。
func (reader *Reader) Current(ctx context.Context) (team.ReferenceCatalog, error) {
	elements, err := reader.readElements(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	abilities, err := reader.readAbilities(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	items, err := reader.readItems(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	skills, err := reader.readSkills(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	stats, err := reader.readStats(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	natures, err := reader.readNatures(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}
	metadata, err := reader.creatureMetadata.Get(ctx)
	if err != nil {
		return team.ReferenceCatalog{}, err
	}

	return team.ReferenceCatalog{
		Elements: elements, Abilities: abilities, Items: items, Skills: skills, Stats: stats, Natures: natures,
		CreatureMetadata: metadata.Data,
	}, nil
}

func (reader *Reader) readElements(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.elements.List(ctx, element.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

func (reader *Reader) readAbilities(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.abilities.List(ctx, ability.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

func (reader *Reader) readItems(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.items.List(ctx, item.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

func (reader *Reader) readSkills(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.skills.List(ctx, skill.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

func (reader *Reader) readStats(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.stats.List(ctx, stat.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

func (reader *Reader) readNatures(ctx context.Context) ([]team.Reference, error) {
	return readReferences(ctx, func(page int32) (referencePage, error) {
		enabled := true
		result, err := reader.natures.List(ctx, nature.ListQuery{Page: page, PageSize: referencePageSize, Enabled: &enabled})
		if err != nil {
			return referencePage{}, err
		}
		items := make([]team.Reference, len(result.Items))
		for index, value := range result.Items {
			items[index] = team.Reference{ID: value.ID, Code: value.Code, Enabled: value.Enabled}
		}
		return referencePage{Items: items, Total: result.Total}, nil
	})
}

// referencePage 是不同资料分页结果映射到统一最小引用后的内部技术结构。
type referencePage struct {
	// Items 是当前页的启用引用。
	Items []team.Reference
	// Total 是 Query 在当前查询条件下返回的总条数。
	Total int64
}

func readReferences(ctx context.Context, readPage func(int32) (referencePage, error)) ([]team.Reference, error) {
	result := make([]team.Reference, 0)
	for page := int32(1); ; page++ {
		if err := ctx.Err(); err != nil {
			return nil, err
		}
		current, err := readPage(page)
		if err != nil {
			return nil, err
		}
		result = append(result, current.Items...)
		if int64(len(result)) >= current.Total || len(current.Items) == 0 {
			return result, nil
		}
	}
}
