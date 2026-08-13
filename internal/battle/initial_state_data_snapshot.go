package battle

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
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

// initialStateDataSnapshot 是一次 Battle 启动所需的不可变资料投影。
//
// 读取 adapter 只在快照构建阶段使用；后续各编译 module 只消费此值，不再直接依赖数据库查询。
type initialStateDataSnapshot struct {
	metadata      creaturemetadata.Data
	itemRules     itemrules.Projection
	skills        map[snowflake.ID]skill.Skill
	stats         map[snowflake.ID]stat.Stat
	natures       map[snowflake.ID]nature.Nature
	abilities     map[snowflake.ID]ability.Ability
	abilityDetail map[snowflake.ID]*abilitydetail.RuleSet
	damageClasses map[snowflake.ID]skilldamageclass.DamageClass
	details       map[snowflake.ID]skilldetail.RuleSet
	statChanges   map[snowflake.ID][]skillstatchange.Change
	ailments      map[snowflake.ID]skillailment.Ailment
	targets       map[snowflake.ID]skilltarget.Target
}

func loadInitialStateDataSnapshot(ctx context.Context, reader *GameDataInitialStateFactsReader, session Battle, metadata creaturemetadata.Data, itemRules itemrules.Projection) (initialStateDataSnapshot, error) {
	snapshot := initialStateDataSnapshot{
		metadata: metadata, itemRules: itemRules,
		skills: make(map[snowflake.ID]skill.Skill), stats: make(map[snowflake.ID]stat.Stat), natures: make(map[snowflake.ID]nature.Nature),
		abilities: make(map[snowflake.ID]ability.Ability), abilityDetail: make(map[snowflake.ID]*abilitydetail.RuleSet),
		damageClasses: make(map[snowflake.ID]skilldamageclass.DamageClass), details: make(map[snowflake.ID]skilldetail.RuleSet),
		statChanges: make(map[snowflake.ID][]skillstatchange.Change), ailments: make(map[snowflake.ID]skillailment.Ailment), targets: make(map[snowflake.ID]skilltarget.Target),
	}
	for _, participant := range session.Participants {
		for _, member := range participant.Team.Members {
			if err := loadInitialStateMemberData(ctx, reader, &snapshot, member); err != nil {
				return initialStateDataSnapshot{}, err
			}
		}
	}
	return snapshot, nil
}

func loadInitialStateMemberData(ctx context.Context, reader *GameDataInitialStateFactsReader, snapshot *initialStateDataSnapshot, member team.Member) error {
	if member.AbilityID != snowflake.ID(0) {
		value, err := reader.abilities.Get(ctx, member.AbilityID)
		if err != nil || !value.Enabled || value.ID != member.AbilityID {
			return ErrInitialStateCompilation
		}
		snapshot.abilities[member.AbilityID] = value
		rules, valid := value.Rules.Values()
		if !valid {
			return ErrInitialStateCompilation
		}
		detail := abilitydetail.RuleSet{AbilityID: member.AbilityID, OptionalValues: rules, Version: value.Version}
		snapshot.abilityDetail[member.AbilityID] = &detail
	}
	if member.NatureID != snowflake.ID(0) {
		value, err := reader.natures.Get(ctx, member.NatureID)
		if err != nil || !value.Enabled || value.ID != member.NatureID {
			return ErrInitialStateCompilation
		}
		snapshot.natures[member.NatureID] = value
	}
	for _, memberStat := range member.Stats {
		if memberStat.StatID == snowflake.ID(0) {
			continue
		}
		value, err := reader.stats.Get(ctx, memberStat.StatID)
		if err != nil || !value.Enabled || value.ID != memberStat.StatID {
			return ErrInitialStateCompilation
		}
		snapshot.stats[memberStat.StatID] = value
	}
	for _, memberSkill := range member.Skills {
		if memberSkill.SkillID == snowflake.ID(0) || snapshot.skills[memberSkill.SkillID].ID != snowflake.ID(0) {
			continue
		}
		value, err := reader.skills.Get(ctx, memberSkill.SkillID)
		if err != nil || !value.Enabled || value.ID != memberSkill.SkillID || value.DamageClassID == nil {
			return ErrInitialStateCompilation
		}
		snapshot.skills[memberSkill.SkillID] = value
		damageClass, err := reader.damageClasses.Get(ctx, *value.DamageClassID)
		if err != nil || !damageClass.Enabled || damageClass.ID != *value.DamageClassID {
			return ErrInitialStateCompilation
		}
		snapshot.damageClasses[*value.DamageClassID] = damageClass
		rules, valid := value.Rules.Values()
		if !valid {
			return ErrInitialStateCompilation
		}
		detail := skilldetail.RuleSet{SkillID: memberSkill.SkillID, OptionalValues: rules, Version: value.Version}
		snapshot.details[memberSkill.SkillID] = detail
		if detail.AilmentID != nil {
			ailment, err := reader.ailments.Get(ctx, *detail.AilmentID)
			if err != nil || !ailment.Enabled || ailment.ID != *detail.AilmentID {
				return ErrInitialStateCompilation
			}
			snapshot.ailments[*detail.AilmentID] = ailment
		}
		if detail.TargetID != nil {
			target, err := reader.targets.Get(ctx, *detail.TargetID)
			if err != nil || !target.Enabled || target.ID != *detail.TargetID {
				return ErrInitialStateCompilation
			}
			snapshot.targets[*detail.TargetID] = target
		}
		changes, err := listInitialStateStatChanges(ctx, reader.statChanges, memberSkill.SkillID)
		if err != nil {
			return err
		}
		snapshot.statChanges[memberSkill.SkillID] = changes
	}
	return nil
}

func listInitialStateStatChanges(ctx context.Context, query InitialStateDataStatChangeQuery, skillID snowflake.ID) ([]skillstatchange.Change, error) {
	var result []skillstatchange.Change
	for page := int32(1); ; page++ {
		current, err := query.List(ctx, skillstatchange.ListQuery{Page: page, PageSize: initialStateDataPageSize, SkillID: &skillID})
		if err != nil {
			return nil, err
		}
		for _, value := range current.Items {
			if value.SkillID != skillID {
				return nil, ErrInitialStateCompilation
			}
			result = append(result, value)
		}
		if int64(len(result)) >= current.Total || len(current.Items) == 0 {
			return result, nil
		}
	}
}
