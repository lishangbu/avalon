package rpg

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrProfessionUnavailable 表示目标职业不属于角色、已停用或激活集合形状无效。
	ErrProfessionUnavailable = errors.New("职业当前不可激活")
	// ErrProfessionChangeInBattle 表示 Battle Reservation 阻止角色改变职业资格。
	ErrProfessionChangeInBattle = errors.New("PlayerCharacter 正在 Battle 中")
)

// ActiveProfession 是 PlayerCharacter 当前参与装备与规则资格判定的一项职业成长事实。
type ActiveProfession struct {
	// ProfessionID 是激活的 Profession Catalog Identifier。
	ProfessionID snowflake.ID
	// Name 是当前职业资料的简体中文名称。
	Name string
	// Level 与 Experience 是保留在角色职业关系中的成长事实。
	Level      int32
	Experience int64
	// Version 是该职业成长关系的乐观版本。
	Version int64
}

// ReplaceActiveProfessionsCommand 原子替换当前参与资格判定的职业集合。
type ReplaceActiveProfessionsCommand struct {
	// AccountID 用于解析当前活动 PlayerCharacter。
	AccountID snowflake.ID
	// ProfessionIDs 是角色已经拥有且希望激活的完整非空集合。
	ProfessionIDs []snowflake.ID
	// IdempotencyKey 使网络重试返回首次职业集合。
	IdempotencyKey string
	// Now 是职业关系与 Outbox 使用的统一 UTC 时间。
	Now time.Time
}

// ProfessionRepository 定义玩家职业读取与激活集合替换的关系型持久化端口。
type ProfessionRepository interface {
	GetActiveProfessions(context.Context, snowflake.ID) ([]ActiveProfession, error)
	ReplaceActiveProfessions(context.Context, ReplaceActiveProfessionsCommand) ([]ActiveProfession, error)
}
