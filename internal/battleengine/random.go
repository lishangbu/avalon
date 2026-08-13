// Package battleengine 提供与传输、存储和并发模型无关的纯战斗计算能力。
package battleengine

import (
	"errors"
	"fmt"
	"math"
	"strings"
)

const (
	// RandomAlgorithmSplitMix64V1 是首版跨语言确定性随机算法标识。
	//
	// 已发布的算法标识及其位运算语义不可原地修改；算法升级必须使用新标识，
	// 以保证历史战斗可以按原始随机轨迹稳定重放。
	RandomAlgorithmSplitMix64V1 = "splitmix64-v1"
)

var (
	// ErrUnsupportedRandomAlgorithm 表示调用方请求了当前引擎不支持的随机算法版本。
	ErrUnsupportedRandomAlgorithm = errors.New("不支持的随机算法")
	// ErrInvalidRandomRequest 表示随机上界或用途等调用参数不满足契约。
	ErrInvalidRandomRequest = errors.New("无效的随机请求")
	// ErrInvalidRandomTrace 表示持久化随机轨迹自身不完整或不合法。
	ErrInvalidRandomTrace = errors.New("无效的随机轨迹")
	// ErrRandomTraceDiverged 表示重放请求与记录轨迹首次出现不一致。
	ErrRandomTraceDiverged = errors.New("随机轨迹发生偏离")
	// ErrUnsupportedRandomInput 表示回合结算收到了引擎无法识别的随机输入实现。
	ErrUnsupportedRandomInput = errors.New("不支持的随机输入")
)

// RandomInput 是回合结算支持的显式随机输入边界。
//
// 调用方可以传入 RandomSource 生成并记录新轨迹，也可以传入 TracedRandom 严格重放
// 已持久化轨迹。未导出的标记方法把实现集合限制在本包已审计的确定性类型中。
type RandomInput interface {
	battleRandomInput()
}

// RandomTraceEntry 记录一次可重放的随机取值。
//
// 字段名称和语义属于持久化回放契约，必须与已经批准的历史轨迹严格一致。
// Sequence 在每回合从 1 开始连续递增，Value 始终位于 [0, Bound) 区间。
type RandomTraceEntry struct {
	// Sequence 是本回合内从 1 开始连续递增的随机消费序号。
	Sequence int32 `json:"sequence"`
	// Bound 是本次随机请求的排他上界，必须大于 0。
	Bound int32 `json:"bound"`
	// Reason 是说明随机值用途的稳定标识，例如 accuracy 或 damage-roll。
	Reason string `json:"reason"`
	// Value 是本次随机结果，取值范围始终为 [0, Bound)。
	Value int32 `json:"value"`
}

// RandomSource 是使用确定性算法生成随机值的不可变值对象。
//
// Next 不会修改接收者；调用方必须保存返回的下一状态。这使战斗计算能够保持纯函数
// 边界，并避免重试、回放或分支推演共享可变随机状态。
type RandomSource struct {
	// algorithm 固定本随机源使用的算法版本，防止历史战斗随实现升级漂移。
	algorithm string
	// state 保存下一次 SplitMix64 混合前的 64 位内部状态。
	state uint64
	// sequence 保存本回合已经成功生成的随机轨迹项数量。
	sequence int32
	// replaying 表示当前值是否是为统一回合边界创建的严格轨迹回放适配器。
	replaying bool
	// replay 保存回放模式下已校验的随机轨迹及其下一消费位置。
	replay TracedRandom
}

// RandomSourceSnapshot 是可随 Battle 权威状态原子持久化的确定性随机游标。
type RandomSourceSnapshot struct {
	Algorithm string `json:"algorithm"`
	State     uint64 `json:"state"`
}

// Snapshot 返回下一回合可继续使用的非回放随机源快照。
func (source RandomSource) Snapshot() (RandomSourceSnapshot, error) {
	if source.replaying || source.algorithm != RandomAlgorithmSplitMix64V1 {
		return RandomSourceSnapshot{}, ErrUnsupportedRandomInput
	}
	return RandomSourceSnapshot{Algorithm: source.algorithm, State: source.state}, nil
}

// RestoreRandomSource 从持久化快照恢复下一回合的确定性随机源。
func RestoreRandomSource(snapshot RandomSourceSnapshot) (RandomSource, error) {
	return NewRandomSource(snapshot.Algorithm, snapshot.State)
}

// battleRandomInput 将 RandomSource 标记为引擎支持的随机输入实现。
func (RandomSource) battleRandomInput() {}

// NewRandomSource 使用明确的算法版本和种子创建确定性随机源。
func NewRandomSource(algorithm string, seed uint64) (RandomSource, error) {
	if algorithm != RandomAlgorithmSplitMix64V1 {
		return RandomSource{}, fmt.Errorf("%w: %q", ErrUnsupportedRandomAlgorithm, algorithm)
	}
	return RandomSource{algorithm: algorithm, state: seed}, nil
}

// Next 返回 [0, bound) 内的无偏随机值、下一随机源状态和对应轨迹项。
func (source RandomSource) Next(bound int32, reason string) (int32, RandomSource, RandomTraceEntry, error) {
	if bound <= 0 || strings.TrimSpace(reason) == "" || source.sequence == math.MaxInt32 {
		return 0, source, RandomTraceEntry{}, fmt.Errorf(
			"%w: bound=%d reason=%q", ErrInvalidRandomRequest, bound, reason,
		)
	}
	if source.replaying {
		value, nextReplay, trace, err := source.replay.Next(bound, reason)
		if err != nil {
			return 0, source, RandomTraceEntry{}, err
		}
		next := source
		next.replay = nextReplay
		return value, next, trace, nil
	}

	// 丢弃会造成取模偏差的低位区间。uint64 溢出在 Go 中定义为模 2^64，
	// 因而 -bound % bound 等价于 2^64 % bound。
	unsignedBound := uint64(bound)
	threshold := -unsignedBound % unsignedBound
	next := source
	var raw uint64
	for {
		next.state += 0x9e3779b97f4a7c15
		raw = mixSplitMix64(next.state)
		if raw >= threshold {
			break
		}
	}

	next.sequence++
	value := int32(raw % unsignedBound)
	trace := RandomTraceEntry{
		Sequence: next.sequence,
		Bound:    bound,
		Reason:   reason,
		Value:    value,
	}
	return value, next, trace, nil
}

// mixSplitMix64 执行 splitmix64-v1 固定的雪崩混合步骤。
func mixSplitMix64(value uint64) uint64 {
	value = (value ^ (value >> 30)) * 0xbf58476d1ce4e5b9
	value = (value ^ (value >> 27)) * 0x94d049bb133111eb
	return value ^ (value >> 31)
}

// TracedRandom 是从既有轨迹读取随机值的不可变回放源。
type TracedRandom struct {
	// entries 是构造时复制并完成完整性校验的只读随机轨迹。
	entries []RandomTraceEntry
	// position 指向下一次重放必须消费的轨迹项下标。
	position int
}

// battleRandomInput 将 TracedRandom 标记为引擎支持的随机输入实现。
func (TracedRandom) battleRandomInput() {}

// NewTracedRandom 校验并复制完整随机轨迹，避免调用方后续修改影响重放结果。
func NewTracedRandom(entries []RandomTraceEntry) (TracedRandom, error) {
	owned := append([]RandomTraceEntry(nil), entries...)
	for index, entry := range owned {
		expectedSequence := int32(index + 1)
		if entry.Sequence != expectedSequence || entry.Bound <= 0 ||
			strings.TrimSpace(entry.Reason) == "" || entry.Value < 0 || entry.Value >= entry.Bound {
			return TracedRandom{}, fmt.Errorf(
				"%w: position=%d entry=%+v", ErrInvalidRandomTrace, index, entry,
			)
		}
	}
	return TracedRandom{entries: owned}, nil
}

// Next 按顺序消费一个轨迹项，并在序号、上界或用途首次不一致时拒绝重放。
func (replay TracedRandom) Next(bound int32, reason string) (int32, TracedRandom, RandomTraceEntry, error) {
	if bound <= 0 || strings.TrimSpace(reason) == "" {
		return 0, replay, RandomTraceEntry{}, fmt.Errorf(
			"%w: bound=%d reason=%q", ErrInvalidRandomRequest, bound, reason,
		)
	}
	if replay.position >= len(replay.entries) {
		return 0, replay, RandomTraceEntry{}, fmt.Errorf(
			"%w: 已消费全部随机轨迹", ErrRandomTraceDiverged,
		)
	}

	entry := replay.entries[replay.position]
	expectedSequence := int32(replay.position + 1)
	if entry.Sequence != expectedSequence || entry.Bound != bound || entry.Reason != reason {
		return 0, replay, RandomTraceEntry{}, fmt.Errorf(
			"%w: 请求 sequence=%d bound=%d reason=%q，轨迹为 %+v",
			ErrRandomTraceDiverged, expectedSequence, bound, reason, entry,
		)
	}

	next := replay
	next.position++
	return entry.Value, next, entry, nil
}

// FullyConsumed 报告轨迹是否已被完整消费。
func (replay TracedRandom) FullyConsumed() bool {
	return replay.position == len(replay.entries)
}
