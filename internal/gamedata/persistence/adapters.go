// Package persistence 提供游戏资料应用服务的 PostgreSQL 持久化适配器。
package persistence

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

// Adapters 汇集游戏资料组装层共享的 PostgreSQL 持久化适配器。
type Adapters struct {
	pool  *database.Pool
	newID snowflake.Source
}

// WithinBattleRules 执行 BattleFormat 与规则组件 CRUD 所需的 PostgreSQL 事务。
func (s *Adapters) WithinBattleRules(ctx context.Context, work func(battleformat.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&battleRuleTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

type transactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// NewAdapters 使用共享数据库连接池和显式 Snowflake Identifier 生成器创建游戏资料适配器。
func NewAdapters(pool *database.Pool, newID snowflake.Source) *Adapters {
	return &Adapters{pool: pool, newID: newID}
}

// WithinElement 执行由属性资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinElement(ctx context.Context, work func(element.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&transactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinElementEffectiveness 执行由属性克制资料服务划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinElementEffectiveness(ctx context.Context, work func(elementeffectiveness.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&elementEffectivenessTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinNature 执行由 Nature 资料服务划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinNature(ctx context.Context, work func(nature.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&natureTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinAbility 执行由特性资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinAbility(ctx context.Context, work func(ability.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&abilityTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinItemCategory 执行由道具分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinItemCategory(ctx context.Context, work func(itemcategory.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&itemCategoryTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinItem 执行由道具资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinItem(ctx context.Context, work func(item.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&itemTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinStat 执行由数值项资料应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinStat(ctx context.Context, work func(stat.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&statTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillDamageClass 执行由技能伤害分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillDamageClass(ctx context.Context, work func(skilldamageclass.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillDamageClassTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkill 执行由技能主体应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkill(ctx context.Context, work func(skill.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillAilment 执行由技能异常应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillAilment(ctx context.Context, work func(skillailment.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillAilmentTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillCategory 执行由技能元分类应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillCategory(ctx context.Context, work func(skillcategory.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillCategoryTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillTarget 执行由技能目标应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillTarget(ctx context.Context, work func(skilltarget.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillTargetTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillLearnMethod 执行由技能学习方式应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillLearnMethod(ctx context.Context, work func(skilllearnmethod.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillLearnMethodTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// WithinSkillStatChange 执行由技能数值变化应用服务明确划定范围的 PostgreSQL 事务。
func (s *Adapters) WithinSkillStatChange(ctx context.Context, work func(skillstatchange.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&skillStatChangeTransactionRepository{parent: s, client: s.pool.Client(transactionCtx), executor: database.Executor(transactionCtx, s.pool)})
	})
}
