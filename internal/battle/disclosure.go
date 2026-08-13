package battle

import (
	"encoding/json"
	"fmt"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// DisclosureView 是某一真人 Participant 可通过 RPC 重同步或实时连接读取的安全战斗视图。
//
// 它不是 Turn Record 的替代品：Turn Record 保留完整权威事实供重放和分析，而本视图只保存当前
// 接收者已经获准知道的状态。对手离场成员和其剩余 PP 始终不出现在本视图中。
type DisclosureView struct {
	// SchemaVersion 是视图 JSON 结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// StateVersion 是生成本视图后 Battle 已提交的连续权威状态版本。
	StateVersion int64 `json:"stateVersion"`
	// TurnNumber 是纯战斗引擎已完成结算的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Result 是已公开的终局事实；战斗仍进行时为 nil。
	Result *battleengine.BattleResult `json:"result,omitempty"`
	// InitialEvents 是 Battle 启动时发生且已向双方公开的结构化事件。
	//
	// 它不属于任何 Turn Record，也不消耗状态版本；该切片由 Battle 启动事务写入披露账本并在后续
	// 状态投影中保留，使重连客户端能重新取得初始入场阶段已经公开的事实。
	InitialEvents []json.RawMessage `json:"initialEvents,omitempty"`
	// Members 是接收者可见成员的稳定排序状态；己方完整，对手仅保留当前场上成员。
	Members []DisclosureMember `json:"members"`
}

// DisclosureMember 是一个已向指定 Participant 披露的成员当前状态。
type DisclosureMember struct {
	// Side 是该成员所属的固定 Battle 阵营位置。
	Side ParticipantSide `json:"side"`
	// MemberPosition 是该成员在冻结 Team Snapshot 中的位置。
	MemberPosition battleengine.MemberPosition `json:"memberPosition"`
	// SlotPosition 是该成员当前占用的场上槽位；己方后备成员使用 0。
	SlotPosition battleengine.SlotPosition `json:"slotPosition"`
	// CurrentHP 是已经向接收者可见的当前生命值。
	CurrentHP uint32 `json:"currentHp"`
	// MajorStatus 是已公开的主要异常状态；空字符串表示无主要异常。
	MajorStatus battleengine.MajorStatus `json:"majorStatus,omitempty"`
	// BadPoisonCounter 是可见成员的剧毒连续伤害倍率；非剧毒为 0。
	BadPoisonCounter int32 `json:"badPoisonCounter"`
	// SleepTurnsRemaining 是可见成员的睡眠剩余次数；非睡眠为 0。
	SleepTurnsRemaining int32 `json:"sleepTurnsRemaining"`
	// StatStages 是可见成员当前公开的能力阶级。
	StatStages map[battleengine.Stat]int8 `json:"statStages"`
	// RemainingPP 只向己方 Participant 返回，避免直接披露对手的完整技能资源。
	RemainingPP []uint8 `json:"remainingPp,omitempty"`
}

// ParticipantViewFor 把完整权威状态摘要投影为指定真人 Participant 的最小安全视图。
//
// 该投影采用保守披露：对手只有当前场上成员可见，换下的成员再次隐藏。这样即使未来规则增加
// 更精细的“曾经见过”语义，也不会意外把未知信息提前写入持久化 Disclosure Ledger。
func ParticipantViewFor(
	state battleengine.StateSummary,
	viewer ParticipantSide,
	stateVersion int64,
) (DisclosureView, error) {
	if (viewer != ParticipantSideOne && viewer != ParticipantSideTwo) || stateVersion < 0 {
		return DisclosureView{}, fmt.Errorf("%w: 披露视图参数无效", ErrInvalidBattle)
	}
	viewerSide := battleSide(viewer)
	view := DisclosureView{
		SchemaVersion: 1, StateVersion: stateVersion, TurnNumber: state.TurnNumber,
		Result: cloneBattleResult(state.Result), Members: make([]DisclosureMember, 0, len(state.Members)),
	}
	for _, member := range state.Members {
		if member.Side != viewerSide && member.SlotPosition == 0 {
			continue
		}
		visible := DisclosureMember{
			Side: sideFromBattle(member.Side), MemberPosition: member.MemberPosition, SlotPosition: member.SlotPosition,
			CurrentHP: member.CurrentHP, MajorStatus: member.MajorStatus, BadPoisonCounter: member.BadPoisonCounter,
			SleepTurnsRemaining: member.SleepTurnsRemaining, StatStages: cloneStatStages(member.StatStages),
		}
		if member.Side == viewerSide {
			visible.RemainingPP = append([]uint8(nil), member.RemainingPP...)
		}
		view.Members = append(view.Members, visible)
	}
	return view, nil
}

func sideFromBattle(side battleengine.Side) ParticipantSide {
	if side == battleengine.SideOne {
		return ParticipantSideOne
	}
	return ParticipantSideTwo
}

func cloneBattleResult(source *battleengine.BattleResult) *battleengine.BattleResult {
	if source == nil {
		return nil
	}
	cloned := *source
	return &cloned
}

func cloneStatStages(source map[battleengine.Stat]int8) map[battleengine.Stat]int8 {
	if source == nil {
		return nil
	}
	cloned := make(map[battleengine.Stat]int8, len(source))
	for stat, stage := range source {
		cloned[stat] = stage
	}
	return cloned
}
