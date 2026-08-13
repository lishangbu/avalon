package battle

import (
	"fmt"
	"strings"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// FirstAvailableBot 是首版确定性 Bot：每个槽位优先使用第一个有 PP 的技能，倒下时换入第一个可战斗后备。
//
// 它刻意不读取随机源、系统时钟或外部资料，因此同一权威 State 总会生成相同选择。更复杂策略必须
// 以新的 Code 或 Version 注册，不能原地改变本实现的行为语义。
type FirstAvailableBot struct {
	// code 是写入 Training Battle Participant 快照的稳定策略代码。
	code string
	// version 是该稳定策略实现的不可变版本。
	version uint32
}

// NewFirstAvailableBot 创建一个用于练习赛的确定性基础 Bot 策略。
func NewFirstAvailableBot(code string, version uint32) (*FirstAvailableBot, error) {
	if strings.TrimSpace(code) == "" || version == 0 {
		return nil, ErrBotStrategyUnavailable
	}
	return &FirstAvailableBot{code: code, version: version}, nil
}

// Code 返回冻结到 Battle Participant 的稳定策略代码。
func (bot *FirstAvailableBot) Code() string {
	if bot == nil {
		return ""
	}
	return bot.code
}

// Version 返回冻结到 Battle Participant 的不可变策略版本。
func (bot *FirstAvailableBot) Version() uint32 {
	if bot == nil {
		return 0
	}
	return bot.version
}

// Plan 按当前纯引擎状态为 Bot Side 的全部场上槽位生成完整确定性行动。
func (bot *FirstAvailableBot) Plan(state battleengine.State, side battleengine.Side) ([]battleengine.Action, error) {
	if bot == nil || !side.Valid() {
		return nil, ErrBotStrategyUnavailable
	}
	snapshot := state.Snapshot()
	self, found := sideSnapshot(snapshot, side)
	if !found {
		return nil, ErrBotStrategyUnavailable
	}
	opponent, found := sideSnapshot(snapshot, oppositeBattleSide(side))
	if !found || len(opponent.ActiveMembers) == 0 {
		return nil, ErrBotStrategyUnavailable
	}
	actions := make([]battleengine.Action, 0, len(self.ActiveMembers))
	for index, position := range self.ActiveMembers {
		member, found := memberSnapshot(self.Members, position)
		if !found {
			return nil, ErrBotStrategyUnavailable
		}
		slot := battleengine.SlotRef{Side: side, Position: battleengine.SlotPosition(index + 1)}
		if member.CurrentHP == 0 {
			next, found := firstAvailableReserve(self)
			if !found {
				return nil, fmt.Errorf("%w: Bot 没有可换入成员", ErrBotStrategyUnavailable)
			}
			actions = append(actions, battleengine.Action{
				Kind: battleengine.ActionKindSwitch, Actor: slot,
				Switch: &battleengine.SwitchAction{MemberPosition: next.Position},
			})
			continue
		}
		skill, found := firstAvailableSkill(member)
		if !found {
			return nil, fmt.Errorf("%w: Bot 场上成员没有可用技能", ErrBotStrategyUnavailable)
		}
		actions = append(actions, battleengine.Action{
			Kind: battleengine.ActionKindUseSkill, Actor: slot,
			UseSkill: &battleengine.UseSkillAction{
				SkillPosition: skill.Position,
				Target:        battleengine.SlotRef{Side: oppositeBattleSide(side), Position: 1},
			},
		})
	}
	return actions, nil
}

func sideSnapshot(snapshot battleengine.StateSnapshot, side battleengine.Side) (battleengine.SideSnapshot, bool) {
	for _, candidate := range snapshot.Sides {
		if candidate.Side == side {
			return candidate, true
		}
	}
	return battleengine.SideSnapshot{}, false
}

func memberSnapshot(members []battleengine.MemberSnapshot, position battleengine.MemberPosition) (battleengine.MemberSnapshot, bool) {
	for _, member := range members {
		if member.Position == position {
			return member, true
		}
	}
	return battleengine.MemberSnapshot{}, false
}

func firstAvailableReserve(side battleengine.SideSnapshot) (battleengine.MemberSnapshot, bool) {
	active := make(map[battleengine.MemberPosition]struct{}, len(side.ActiveMembers))
	for _, position := range side.ActiveMembers {
		active[position] = struct{}{}
	}
	for _, member := range side.Members {
		if _, onField := active[member.Position]; !onField && member.CurrentHP > 0 {
			return member, true
		}
	}
	return battleengine.MemberSnapshot{}, false
}

func firstAvailableSkill(member battleengine.MemberSnapshot) (battleengine.SkillSnapshot, bool) {
	for _, skill := range member.Skills {
		if skill.RemainingPP > 0 {
			return skill, true
		}
	}
	return battleengine.SkillSnapshot{}, false
}

func oppositeBattleSide(side battleengine.Side) battleengine.Side {
	if side == battleengine.SideOne {
		return battleengine.SideTwo
	}
	return battleengine.SideOne
}
