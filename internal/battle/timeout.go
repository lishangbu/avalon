package battle

import (
	"fmt"
	"math/big"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// BattleTimeoutResult 根据冻结初始状态和最后一次权威状态摘要裁定整场超时结果。
//
// 裁定顺序固定为存活成员数量、精确的总剩余生命比例和 No Contest。计算不依赖当前资料、
// 随机源或 Runtime 内存，因此服务重启后由 Worker 读取持久化的初始状态与 Turn Record 仍能得到
// 相同结果。摘要必须覆盖初始状态中的每一个成员，避免损坏数据被静默解释成零生命。
func BattleTimeoutResult(initial battleengine.InitialState, summary battleengine.StateSummary) (Result, error) {
	initialState, err := battleengine.NewState(initial)
	if err != nil {
		return Result{}, fmt.Errorf("%w: 初始战斗状态", ErrInvalidBattleResult)
	}
	if summary.Result != nil {
		return Result{}, fmt.Errorf("%w: 活跃 Battle 不得使用已终局状态裁定超时", ErrInvalidBattleResult)
	}

	initialSummary := initialState.Summary()
	if len(summary.Members) != len(initialSummary.Members) {
		return Result{}, fmt.Errorf("%w: 状态摘要成员数量不一致", ErrInvalidBattleResult)
	}
	maxHPByMember := make(map[timeoutMemberKey]uint32, len(initialSummary.Members))
	for _, member := range initial.Sides {
		for _, snapshot := range member.Members {
			maxHPByMember[timeoutMemberKey{side: member.Side, position: snapshot.Position}] = snapshot.MaxHP
		}
	}
	if len(maxHPByMember) != len(initialSummary.Members) {
		return Result{}, fmt.Errorf("%w: 初始战斗成员重复", ErrInvalidBattleResult)
	}

	totals := map[ParticipantSide]timeoutSideTotal{
		ParticipantSideOne: {},
		ParticipantSideTwo: {},
	}
	seen := make(map[timeoutMemberKey]struct{}, len(summary.Members))
	for _, member := range summary.Members {
		side := ParticipantSide(member.Side)
		if side != ParticipantSideOne && side != ParticipantSideTwo {
			return Result{}, fmt.Errorf("%w: 状态摘要阵营无效", ErrInvalidBattleResult)
		}
		key := timeoutMemberKey{side: member.Side, position: member.MemberPosition}
		maxHP, found := maxHPByMember[key]
		if !found || member.CurrentHP > maxHP {
			return Result{}, fmt.Errorf("%w: 状态摘要成员无效", ErrInvalidBattleResult)
		}
		if _, duplicated := seen[key]; duplicated {
			return Result{}, fmt.Errorf("%w: 状态摘要成员重复", ErrInvalidBattleResult)
		}
		seen[key] = struct{}{}
		total := totals[side]
		total.maximumHP += uint64(maxHP)
		total.currentHP += uint64(member.CurrentHP)
		if member.CurrentHP > 0 {
			total.livingMembers++
		}
		totals[side] = total
	}
	if len(seen) != len(maxHPByMember) || totals[ParticipantSideOne].maximumHP == 0 || totals[ParticipantSideTwo].maximumHP == 0 {
		return Result{}, fmt.Errorf("%w: 状态摘要未覆盖冻结成员", ErrInvalidBattleResult)
	}

	left := totals[ParticipantSideOne]
	right := totals[ParticipantSideTwo]
	if left.livingMembers > right.livingMembers {
		return battleTimeoutWin(ParticipantSideOne), nil
	}
	if right.livingMembers > left.livingMembers {
		return battleTimeoutWin(ParticipantSideTwo), nil
	}

	comparison := compareRemainingHPRatio(left.currentHP, left.maximumHP, right.currentHP, right.maximumHP)
	if comparison > 0 {
		return battleTimeoutWin(ParticipantSideOne), nil
	}
	if comparison < 0 {
		return battleTimeoutWin(ParticipantSideTwo), nil
	}
	return Result{Reason: TerminalReasonNoContest}, nil
}

type timeoutMemberKey struct {
	side     battleengine.Side
	position battleengine.MemberPosition
}

type timeoutSideTotal struct {
	livingMembers uint32
	currentHP     uint64
	maximumHP     uint64
}

func battleTimeoutWin(side ParticipantSide) Result {
	return Result{WinnerSide: side, Reason: TerminalReasonBattleTimeout}
}

func compareRemainingHPRatio(leftCurrent, leftMaximum, rightCurrent, rightMaximum uint64) int {
	left := new(big.Int).Mul(new(big.Int).SetUint64(leftCurrent), new(big.Int).SetUint64(rightMaximum))
	right := new(big.Int).Mul(new(big.Int).SetUint64(rightCurrent), new(big.Int).SetUint64(leftMaximum))
	return left.Cmp(right)
}
