// Package store 提供游戏资料应用服务的 PostgreSQL 持久化适配器。
package store

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/platform/database"
)

// Store 在显式事务中持久化实时资料、审计和幂等响应。
type Store struct {
	pool  *database.Pool
	newID snowflake.Source
}

// WithinBattleRules 执行 BattleFormat 与规则组件 CRUD 所需的 PostgreSQL 事务。
func (s *Store) WithinBattleRules(ctx context.Context, work func(battleformat.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&battleRuleTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

type transactionStore struct {
	parent   *Store
	client   *avalonent.Client
	executor database.Transaction
}

// New 使用共享数据库连接池和显式 Snowflake Identifier 生成器创建游戏资料存储。
func New(pool *database.Pool, newID snowflake.Source) *Store {
	return &Store{pool: pool, newID: newID}
}

// WithinElement 执行由属性资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinElement(ctx context.Context, work func(element.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&transactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinElementEffectiveness 执行由属性克制资料服务划定范围的 PostgreSQL 事务。
func (s *Store) WithinElementEffectiveness(ctx context.Context, work func(elementeffectiveness.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&elementEffectivenessTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinNature 执行由 Nature 资料服务划定范围的 PostgreSQL 事务。
func (s *Store) WithinNature(ctx context.Context, work func(nature.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&natureTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinAbility 执行由特性资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinAbility(ctx context.Context, work func(ability.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&abilityTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinItemCategory 执行由道具分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinItemCategory(ctx context.Context, work func(itemcategory.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&itemCategoryTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinItem 执行由道具资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinItem(ctx context.Context, work func(item.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&itemTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinStat 执行由数值项资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinStat(ctx context.Context, work func(stat.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&statTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillDamageClass 执行由技能伤害分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillDamageClass(ctx context.Context, work func(skilldamageclass.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillDamageClassTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkill 执行由技能主体应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkill(ctx context.Context, work func(skill.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillAilment 执行由技能异常应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillAilment(ctx context.Context, work func(skillailment.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillAilmentTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillCategory 执行由技能元分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillCategory(ctx context.Context, work func(skillcategory.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillCategoryTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillTarget 执行由技能目标应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillTarget(ctx context.Context, work func(skilltarget.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillTargetTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillLearnMethod 执行由技能学习方式应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillLearnMethod(ctx context.Context, work func(skilllearnmethod.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillLearnMethodTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillStatChange 执行由技能数值变化应用服务明确划定范围的 PostgreSQL 事务。
func (s *Store) WithinSkillStatChange(ctx context.Context, work func(skillstatchange.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillStatChangeTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}
