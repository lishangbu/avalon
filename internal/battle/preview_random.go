package battle

import (
	"crypto/sha256"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"sort"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const automaticPreviewAlgorithm = "avalon.battle.preview-sha256-counter.v1"

// automaticPreviewTrace 保存在 Battle 中的自动 Preview 选择完整可重放轨迹。
//
// 种子由稳定 Battle Identifier、Participant Side 和算法版本派生，数据库不保存任何秘密随机材料。Draws
// 记录每次有界抽样的输入和输出，历史工具可以重放并验证最终的成员与初始上场顺序。
type automaticPreviewTrace struct {
	// Algorithm 是不可变的选择算法标识；未来算法变更必须使用新标识。
	Algorithm string `json:"algorithm"`
	// SeedSHA256 是 Identifier、Side 与算法版本派生种子的 SHA-256 十六进制摘要。
	SeedSHA256 string `json:"seedSha256"`
	// Draws 是按实际执行顺序保存的全部有界抽样结果。
	Draws []automaticPreviewDraw `json:"draws"`
}

// automaticPreviewDraw 是一次确定性有界抽样的可审计事实。
type automaticPreviewDraw struct {
	// Counter 是计算本次 SHA-256 块使用的从零开始计数器。
	Counter uint64 `json:"counter"`
	// Bound 是本次抽样的开区间上限，取值至少为 1。
	Bound uint32 `json:"bound"`
	// Value 是落在 [0, Bound) 内的确定性抽样结果。
	Value uint32 `json:"value"`
}

// automaticPreviewSource 提供只服务于 Preview 到期补选的确定性有界随机数。
type automaticPreviewSource struct {
	// seed 是由 Battle 身份和 Participant Side 派生的固定 256 位输入。
	seed [sha256.Size]byte
	// counter 区分同一选择流程内的连续 SHA-256 块。
	counter uint64
	// draws 保存已经实际消耗的抽样轨迹。
	draws []automaticPreviewDraw
}

// automaticExpiredPreview 为一个尚未锁定 Preview 的 Participant 生成合法、确定且可验证的自动选择。
func automaticExpiredPreview(
	battleID snowflake.ID,
	participant Participant,
	format Format,
	submittedAt time.Time,
) (PreviewSubmission, error) {
	if battleID == snowflake.ID(0) || !validBattleFormat(format) || participant.Side != ParticipantSideOne && participant.Side != ParticipantSideTwo {
		return PreviewSubmission{}, ErrInvalidBattle
	}
	positions := make([]int32, 0, len(participant.Team.Members))
	for _, member := range participant.Team.Members {
		positions = append(positions, member.Position)
	}
	sort.Slice(positions, func(left, right int) bool { return positions[left] < positions[right] })
	if len(positions) < int(format.SelectCount) {
		return PreviewSubmission{}, ErrInvalidBattle
	}

	source := newAutomaticPreviewSource(battleID, participant.Side)
	selected := chooseRandomPositions(source, positions, int(format.SelectCount))
	active := chooseRandomPositions(source, selected, int(format.ActiveParticipantsPerSide))
	// RandomTrace 与 Turn Record 一致使用 JSON 数组，允许未来在不改写已有事实的情况下追加独立的
	// 选择阶段或算法记录；当前版本每次自动补选恰好写入一条轨迹。
	trace, err := json.Marshal([]automaticPreviewTrace{{
		Algorithm: automaticPreviewAlgorithm, SeedSHA256: hex.EncodeToString(source.seed[:]), Draws: source.draws,
	}})
	if err != nil {
		return PreviewSubmission{}, fmt.Errorf("编码自动 Preview 随机轨迹: %w", err)
	}
	return PreviewSubmission{
		Side: participant.Side, MemberPositions: selected, ActivePositions: active, SubmittedAt: submittedAt.UTC(), RandomTrace: trace,
	}, nil
}

// newAutomaticPreviewSource 从冻结身份构造稳定的自动 Preview 随机源。
func newAutomaticPreviewSource(battleID snowflake.ID, side ParticipantSide) *automaticPreviewSource {
	input := make([]byte, 0, len(automaticPreviewAlgorithm)+8+1)
	input = append(input, automaticPreviewAlgorithm...)
	var identifier [8]byte
	binary.BigEndian.PutUint64(identifier[:], uint64(battleID))
	input = append(input, identifier[:]...)
	input = append(input, byte(side))
	return &automaticPreviewSource{seed: sha256.Sum256(input)}
}

// chooseRandomPositions 在不改写输入切片的前提下，以局部 Fisher-Yates 抽样选择固定数量的位置。
func chooseRandomPositions(source *automaticPreviewSource, positions []int32, count int) []int32 {
	candidates := append([]int32(nil), positions...)
	for index := 0; index < count; index++ {
		selected := index + int(source.next(uint32(len(candidates)-index)))
		candidates[index], candidates[selected] = candidates[selected], candidates[index]
	}
	return append([]int32(nil), candidates[:count]...)
}

// next 返回 [0, bound) 内的一个确定性值并记录完整抽样轨迹。
func (source *automaticPreviewSource) next(bound uint32) uint32 {
	if source == nil || bound == 0 {
		panic("自动 Preview 随机源边界无效")
	}
	input := make([]byte, sha256.Size+8)
	copy(input, source.seed[:])
	binary.BigEndian.PutUint64(input[sha256.Size:], source.counter)
	digest := sha256.Sum256(input)
	value := uint32(binary.BigEndian.Uint64(digest[:8]) % uint64(bound))
	source.draws = append(source.draws, automaticPreviewDraw{Counter: source.counter, Bound: bound, Value: value})
	source.counter++
	return value
}
