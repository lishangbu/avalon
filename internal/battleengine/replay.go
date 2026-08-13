package battleengine

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"strings"
)

const (
	// GoldenReplaySchemaVersion 是黄金样本文件当前支持的结构版本。
	GoldenReplaySchemaVersion uint32 = 1
)

var (
	// ErrInvalidGoldenReplay 表示黄金样本本身不是当前引擎可安全执行的完整回放输入。
	ErrInvalidGoldenReplay = errors.New("无效的战斗黄金样本")
	// ErrGoldenReplayDiverged 表示引擎输出与黄金样本记录的事件、随机轨迹或状态摘要不同。
	ErrGoldenReplayDiverged = errors.New("战斗黄金样本发生偏离")
)

// GoldenReplay 是完整的确定性战斗回放样本。
//
// 样本不使用运行时随机源或系统时钟，而是为每一回合显式记录完整随机轨迹、预期事件和
// 状态摘要，使引擎语义回退能够被结构化发现。
type GoldenReplay struct {
	// SchemaVersion 是黄金样本 JSON 结构版本，当前固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// Provenance 标识产生本样本预期事实的批准用例和稳定场景。
	Provenance *GoldenReplayProvenance `json:"provenance,omitempty"`
	// InitialState 是开始第一回合前的完整冻结战斗输入。
	InitialState InitialState `json:"initialState"`
	// Turns 按严格回合顺序保存需要重放的命令及其独立预期输出。
	Turns []GoldenReplayTurn `json:"turns"`
}

// GoldenReplayProvenance 是黄金样本对应的批准事实来源。
//
// 引擎只能消费已经人工批准的期望事实，不能用一次运行结果自动批准或覆盖黄金样本。
type GoldenReplayProvenance struct {
	// CaseID 是批准用例的稳定机器标识，用于定位维护责任和评审记录。
	CaseID string `json:"caseId"`
	// Description 是批准用例的简体中文可读说明。
	Description string `json:"description"`
	// Scenario 是黄金样本声明的稳定场景标识。
	Scenario string `json:"scenario"`
}

// GoldenReplayTurn 是黄金样本中的一个完整回合断言。
type GoldenReplayTurn struct {
	// Command 是双方全部决策已经锁定后交给纯引擎的完整命令。
	Command TurnCommand `json:"command"`
	// RandomTrace 是本回合必须按序消费的全部显式随机值。
	RandomTrace []RandomTraceEntry `json:"randomTrace"`
	// ExpectedEvents 是按发生顺序记录的版本化事件 JSON；保留原始对象可避免回放器弱化事件 discriminator。
	ExpectedEvents []json.RawMessage `json:"expectedEvents"`
	// ExpectedState 是完成本回合后的可审计状态摘要。
	ExpectedState StateSummary `json:"expectedState"`
}

// GoldenReplayResult 是成功重放后提供给调用方的最小确定性结果。
type GoldenReplayResult struct {
	// ReplayedTurns 是已通过全部事件、随机与状态断言的完整回合数量。
	ReplayedTurns uint32 `json:"replayedTurns"`
	// FinalState 是最后一个已确认回合结束后的状态摘要。
	FinalState StateSummary `json:"finalState"`
}

// GoldenReplaySuiteResult 是一个黄金样本目录完成严格重放后的聚合报告。
//
// 报告只公开样本数与回合数，不暴露完整命令、事件或成员状态，适合由测试和 CI 输出。
type GoldenReplaySuiteResult struct {
	// ReplayedSamples 是目录中通过加载和完整重放的 JSON 样本数量。
	ReplayedSamples uint32 `json:"replayedSamples"`
	// ReplayedTurns 是全部样本通过事件、随机轨迹与状态摘要比较的回合总数。
	ReplayedTurns uint32 `json:"replayedTurns"`
}

// StateSummary 是用于黄金样本和 Turn Record 的稳定状态摘要。
//
// 摘要覆盖当前引擎所有可变成员状态、场上位置和终局事实，但不把内部索引或 Go
// 实现细节写入持久化契约。新增可变状态时必须同步扩展本结构和黄金样本版本。
type StateSummary struct {
	// TurnNumber 是已经完成结算的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Result 是已确认的终局事实；仍可继续战斗时为 null。
	Result *BattleResult `json:"result,omitempty"`
	// Members 按阵营和成员位置稳定排序，保存每名成员的完整可变摘要。
	Members []MemberStateSummary `json:"members"`
	// Sides 按稳定阵营位置保存不属于任何单个成员的可变机会状态。
	// 太晶化机会在成员换下后仍然消耗，因此不能从任意成员的 Terastallized 状态反向推断。
	Sides []SideStateSummary `json:"sides"`
}

// SideStateSummary 是一方队伍在回合结束时可用于重放核验的可变阵营状态。
type SideStateSummary struct {
	// Side 是该摘要所属的稳定阵营位置。
	Side Side `json:"side"`
	// TerastallizationUsed 表示该方已经消耗本局唯一太晶化机会。
	TerastallizationUsed bool `json:"terastallizationUsed"`
}

// MemberStateSummary 是一名冻结成员在特定回合结束后的可审计可变状态。
type MemberStateSummary struct {
	// Side 是成员所属的稳定阵营位置。
	Side Side `json:"side"`
	// MemberPosition 是成员在本场队伍快照中的稳定位置。
	MemberPosition MemberPosition `json:"memberPosition"`
	// SlotPosition 是成员当前占用的场上槽位；0 表示当前处于后备位置。
	SlotPosition SlotPosition `json:"slotPosition"`
	// CreatureID 是本回合结束时真实用于战斗计算的精灵稳定 Identifier。
	// 它会随形态切换和变身改变，不能由初始队伍资料或对外伪装种类替代。
	CreatureID Identifier `json:"creatureId"`
	// ApparentCreatureID 是本回合结束时对外披露的伪装种类；空字符串表示使用真实种类。
	ApparentCreatureID Identifier `json:"apparentCreatureId,omitempty"`
	// MaxHP 是本回合结束时当前形态的最大生命值。
	// 形态切换可能改变该值，超时裁定和重放均不能继续假定初始最大生命值。
	MaxHP uint32 `json:"maxHp"`
	// CurrentHP 是本回合结束时的实际生命值。
	CurrentHP uint32 `json:"currentHp"`
	// Stats 是本回合结束时当前形态用于伤害和行动排序的五项基础战斗能力。
	Stats StatBlock `json:"stats"`
	// Weight 是本回合结束时当前形态的体重整数刻度，用于依赖双方体重的动态威力规则。
	Weight uint32 `json:"weight"`
	// ElementIDs 是本回合结束时当前形态的一至两个属性稳定 Identifier 文本。
	ElementIDs []Identifier `json:"elementIds"`
	// NaturalElementIDs 是本回合结束时当前形态的自然属性基线。
	// 太晶化或道具属性身份覆盖当前属性时，本字段仍可用于审计形态变化，而不能由当前 ElementIDs 反推。
	NaturalElementIDs []Identifier `json:"naturalElementIds"`
	// TeraElementID 是成员在 Battle 启动时冻结的太晶属性稳定 Identifier。
	TeraElementID Identifier `json:"teraElementId,omitempty"`
	// Terastallized 表示成员是否已经完成太晶化。
	Terastallized bool `json:"terastallized"`
	// AbilityID 是本回合结束时实际生效的特性稳定 Identifier。
	// 入场复制特性会修改该值，因此它不能只由初始成员资料或显示文本推断。
	AbilityID Identifier `json:"abilityId,omitempty"`
	// ItemID 是本回合结束时仍携带的道具稳定 Identifier；空字符串表示未携带或已经被消耗。
	ItemID Identifier `json:"itemId,omitempty"`
	// BoosterEnergyStat 是已消耗最高原始能力强化道具后持续强化的能力项；空字符串表示尚未激活。
	BoosterEnergyStat Stat `json:"boosterEnergyStat,omitempty"`
	// MajorStatus 是本回合结束时持有的主要异常；空字符串表示没有主要异常。
	MajorStatus MajorStatus `json:"majorStatus,omitempty"`
	// BadPoisonCounter 是剧毒当前连续在场阶段的伤害倍率；其它状态时为 0。
	BadPoisonCounter int32 `json:"badPoisonCounter"`
	// SleepTurnsRemaining 是睡眠剩余阻止行动次数；其它状态时为 0。
	SleepTurnsRemaining int32 `json:"sleepTurnsRemaining"`
	// ConfusionTurnsRemaining 是混乱仍会参与行动前判定的次数；0 表示未混乱。
	ConfusionTurnsRemaining uint8 `json:"confusionTurnsRemaining"`
	// BindingTurnsRemaining 是束缚仍会在回合末造成伤害并禁止换人的次数；0 表示未束缚。
	BindingTurnsRemaining uint8 `json:"bindingTurnsRemaining"`
	// ProtectionTurnsRemaining 是保护在回合结束前仍会阻止对方技能的剩余阶段数。
	ProtectionTurnsRemaining uint8 `json:"protectionTurnsRemaining"`
	// ProtectionChain 是成员连续成功保护次数，用于重放下一次保护的确定性概率。
	ProtectionChain uint8 `json:"protectionChain"`
	// SubstituteHP 是本回合结束时替身剩余的独立生命值；0 表示成员当前没有替身。
	SubstituteHP uint32 `json:"substituteHp,omitempty"`
	// TauntTurnsRemaining 是挑衅仍会阻止变化技能的行动次数；0 表示未挑衅。
	TauntTurnsRemaining uint8 `json:"tauntTurnsRemaining"`
	// ChargingSkillPosition 是下一回合必须完成的蓄力技能槽；0 表示没有蓄力。
	ChargingSkillPosition SkillPosition `json:"chargingSkillPosition"`
	// ChargingTurnsRemaining 是完成蓄力前仍需等待的行动次数。
	ChargingTurnsRemaining uint8 `json:"chargingTurnsRemaining"`
	// RechargeTurnsRemaining 是该成员在下一次技能行动前还必须放弃的行动次数。
	RechargeTurnsRemaining uint8 `json:"rechargeTurnsRemaining"`
	// AccuracyLockTarget 是该成员当前锁定命中的具体目标成员；nil 表示没有命中锁定。
	AccuracyLockTarget *MemberRef `json:"accuracyLockTarget,omitempty"`
	// AccuracyLockTurnsRemaining 是命中锁定在回合末推进前的剩余持续阶段数。
	AccuracyLockTurnsRemaining uint8 `json:"accuracyLockTurnsRemaining"`
	// LockedSkillPosition 是锁招期间必须重复的技能槽；0 表示没有锁招。
	LockedSkillPosition SkillPosition `json:"lockedSkillPosition"`
	// LockedTurnsRemaining 是还必须重复使用锁定技能的行动次数。
	LockedTurnsRemaining uint8 `json:"lockedTurnsRemaining"`
	// DisabledSkillPosition 是当前被定身的技能槽；0 表示没有定身。
	DisabledSkillPosition SkillPosition `json:"disabledSkillPosition"`
	// DisabledTurnsRemaining 是定身剩余有效行动次数。
	DisabledTurnsRemaining uint8 `json:"disabledTurnsRemaining"`
	// LastUsedSkillPosition 是成员最近一次实际宣告的技能槽。
	LastUsedSkillPosition SkillPosition `json:"lastUsedSkillPosition"`
	// StatStages 是本回合结束时的全部非零或已显式记录的能力阶级。
	StatStages map[Stat]int8 `json:"statStages"`
	// RemainingPP 按技能稳定位置保存本回合结束时的剩余 PP。
	RemainingPP []uint8 `json:"remainingPp"`
}

// LoadGoldenReplay 从严格 JSON 文件加载黄金样本。
//
// 文件中出现未知字段、多个 JSON 文档或不完整基础结构都会被拒绝，避免错误的样本
// 在 CI 中被静默降级为较弱断言。
func LoadGoldenReplay(path string) (GoldenReplay, error) {
	encoded, err := os.ReadFile(path)
	if err != nil {
		return GoldenReplay{}, fmt.Errorf("读取黄金样本 %q: %w", path, err)
	}
	decoder := json.NewDecoder(bytes.NewReader(encoded))
	decoder.DisallowUnknownFields()
	var replay GoldenReplay
	if err := decoder.Decode(&replay); err != nil {
		return GoldenReplay{}, fmt.Errorf("解析黄金样本 %q: %w", path, err)
	}
	if decoder.More() {
		return GoldenReplay{}, fmt.Errorf("%w: 黄金样本 %q 包含多个 JSON 值", ErrInvalidGoldenReplay, path)
	}
	if replay.Provenance == nil || strings.TrimSpace(replay.Provenance.CaseID) == "" ||
		strings.TrimSpace(replay.Provenance.Description) == "" || strings.TrimSpace(replay.Provenance.Scenario) == "" {
		return GoldenReplay{}, fmt.Errorf("%w: 黄金样本 %q 的来源元数据不完整", ErrInvalidGoldenReplay, path)
	}
	if err := validateGoldenReplay(replay); err != nil {
		return GoldenReplay{}, err
	}
	return replay, nil
}

// ReplayGolden 严格执行黄金样本，并逐回合比较随机轨迹、事件和状态摘要。
func ReplayGolden(replay GoldenReplay) (GoldenReplayResult, error) {
	if err := validateGoldenReplay(replay); err != nil {
		return GoldenReplayResult{}, err
	}
	state, err := NewState(replay.InitialState)
	if err != nil {
		return GoldenReplayResult{}, fmt.Errorf("%w: 初始状态: %w", ErrInvalidGoldenReplay, err)
	}

	for index, turn := range replay.Turns {
		random, randomErr := NewTracedRandom(turn.RandomTrace)
		if randomErr != nil {
			return GoldenReplayResult{}, fmt.Errorf("%w: 第 %d 回合随机轨迹: %w", ErrInvalidGoldenReplay, index+1, randomErr)
		}
		result, resolveErr := ResolveTurn(state, turn.Command, random)
		if resolveErr != nil {
			return GoldenReplayResult{}, fmt.Errorf("%w: 第 %d 回合无法结算: %w", ErrGoldenReplayDiverged, index+1, resolveErr)
		}
		if !reflect.DeepEqual(result.RandomTrace, turn.RandomTrace) {
			return GoldenReplayResult{}, fmt.Errorf("%w: 第 %d 回合随机轨迹不同", ErrGoldenReplayDiverged, index+1)
		}
		if err := compareEvents(result.Events, turn.ExpectedEvents); err != nil {
			return GoldenReplayResult{}, fmt.Errorf("%w: 第 %d 回合事件: %w", ErrGoldenReplayDiverged, index+1, err)
		}
		summary := result.State.Summary()
		if !reflect.DeepEqual(summary, turn.ExpectedState) {
			return GoldenReplayResult{}, fmt.Errorf("%w: 第 %d 回合状态摘要不同", ErrGoldenReplayDiverged, index+1)
		}
		state = result.State
	}
	return GoldenReplayResult{ReplayedTurns: uint32(len(replay.Turns)), FinalState: state.Summary()}, nil
}

// RestoreState 严格重放零个或多个持久化回合并返回可继续执行的完整引擎状态。
func RestoreState(initial InitialState, turns []GoldenReplayTurn) (State, error) {
	state, err := NewState(initial)
	if err != nil {
		return State{}, fmt.Errorf("%w: 初始状态: %w", ErrInvalidGoldenReplay, err)
	}
	for index, turn := range turns {
		random, randomErr := NewTracedRandom(turn.RandomTrace)
		if randomErr != nil {
			return State{}, fmt.Errorf("%w: 第 %d 回合随机轨迹: %w", ErrInvalidGoldenReplay, index+1, randomErr)
		}
		result, resolveErr := ResolveTurn(state, turn.Command, random)
		if resolveErr != nil || !reflect.DeepEqual(result.RandomTrace, turn.RandomTrace) || !reflect.DeepEqual(result.State.Summary(), turn.ExpectedState) {
			return State{}, fmt.Errorf("%w: 第 %d 回合", ErrGoldenReplayDiverged, index+1)
		}
		if err := compareEvents(result.Events, turn.ExpectedEvents); err != nil {
			return State{}, fmt.Errorf("%w: 第 %d 回合事件: %w", ErrGoldenReplayDiverged, index+1, err)
		}
		state = result.State
	}
	return state, nil
}

// ReplayGoldenSuite 按文件名稳定顺序严格重放目录中的全部 JSON 黄金样本。
//
// 子目录、非 JSON 文件和空目录都会被拒绝，防止 CI 因路径错误或样本扩展名漂移而报告虚假的成功。
func ReplayGoldenSuite(directory string) (GoldenReplaySuiteResult, error) {
	entries, err := os.ReadDir(directory)
	if err != nil {
		return GoldenReplaySuiteResult{}, fmt.Errorf("读取战斗黄金样本目录 %q: %w", directory, err)
	}
	result := GoldenReplaySuiteResult{}
	for _, entry := range entries {
		if entry.IsDir() || !strings.EqualFold(filepath.Ext(entry.Name()), ".json") {
			continue
		}
		replay, loadErr := LoadGoldenReplay(filepath.Join(directory, entry.Name()))
		if loadErr != nil {
			return GoldenReplaySuiteResult{}, fmt.Errorf("加载黄金样本 %q: %w", entry.Name(), loadErr)
		}
		replayed, replayErr := ReplayGolden(replay)
		if replayErr != nil {
			return GoldenReplaySuiteResult{}, fmt.Errorf("重放黄金样本 %q: %w", entry.Name(), replayErr)
		}
		result.ReplayedSamples++
		result.ReplayedTurns += replayed.ReplayedTurns
	}
	if result.ReplayedSamples == 0 {
		return GoldenReplaySuiteResult{}, fmt.Errorf("%w: 黄金样本目录 %q 不包含 JSON 样本", ErrInvalidGoldenReplay, directory)
	}
	return result, nil
}

// Summary 生成当前 State 的语言无关、稳定排序状态摘要。
func (state State) Summary() StateSummary {
	summary := StateSummary{
		TurnNumber: state.turnNumber,
		Result:     cloneBattleResult(state.result),
		Members:    make([]MemberStateSummary, 0, MaximumMembersPerSide*2),
		Sides:      make([]SideStateSummary, 0, 2),
	}
	for _, side := range state.sides {
		summary.Sides = append(summary.Sides, SideStateSummary{
			Side: side.Side, TerastallizationUsed: side.TerastallizationUsed,
		})
		slotByMember := make(map[MemberPosition]SlotPosition, len(side.ActiveMembers))
		for index, position := range side.ActiveMembers {
			slotByMember[position] = SlotPosition(index + 1)
		}
		for _, member := range side.Members {
			remainingPP := make([]uint8, len(member.Skills))
			for skillIndex, skill := range member.Skills {
				remainingPP[skillIndex] = skill.RemainingPP
			}
			summary.Members = append(summary.Members, MemberStateSummary{
				Side:                       side.Side,
				MemberPosition:             member.Position,
				SlotPosition:               slotByMember[member.Position],
				CreatureID:                 member.CreatureID,
				ApparentCreatureID:         member.ApparentCreatureID,
				MaxHP:                      member.MaxHP,
				CurrentHP:                  member.CurrentHP,
				Stats:                      member.Stats,
				Weight:                     member.Weight,
				ElementIDs:                 append([]Identifier(nil), member.ElementIDs...),
				NaturalElementIDs:          append([]Identifier(nil), member.NaturalElementIDs...),
				TeraElementID:              member.TeraElementID,
				Terastallized:              member.Terastallized,
				AbilityID:                  member.AbilityID,
				ItemID:                     member.ItemID,
				BoosterEnergyStat:          member.BoosterEnergyStat,
				MajorStatus:                member.MajorStatus,
				BadPoisonCounter:           member.BadPoisonCounter,
				SleepTurnsRemaining:        member.SleepTurnsRemaining,
				ConfusionTurnsRemaining:    member.ConfusionTurnsRemaining,
				BindingTurnsRemaining:      member.BindingTurnsRemaining,
				ProtectionTurnsRemaining:   member.ProtectionTurnsRemaining,
				ProtectionChain:            member.ProtectionChain,
				SubstituteHP:               member.SubstituteHP,
				TauntTurnsRemaining:        member.TauntTurnsRemaining,
				ChargingSkillPosition:      member.ChargingSkillPosition,
				ChargingTurnsRemaining:     member.ChargingTurnsRemaining,
				RechargeTurnsRemaining:     member.RechargeTurnsRemaining,
				AccuracyLockTarget:         cloneMemberRef(member.AccuracyLockTarget),
				AccuracyLockTurnsRemaining: member.AccuracyLockTurnsRemaining,
				LockedSkillPosition:        member.LockedSkillPosition,
				LockedTurnsRemaining:       member.LockedTurnsRemaining,
				DisabledSkillPosition:      member.DisabledSkillPosition,
				DisabledTurnsRemaining:     member.DisabledTurnsRemaining,
				LastUsedSkillPosition:      member.LastUsedSkillPosition,
				StatStages:                 cloneStatStageSummary(member.StatStages),
				RemainingPP:                remainingPP,
			})
		}
	}
	sort.Slice(summary.Members, func(left, right int) bool {
		if summary.Members[left].Side != summary.Members[right].Side {
			return summary.Members[left].Side < summary.Members[right].Side
		}
		return summary.Members[left].MemberPosition < summary.Members[right].MemberPosition
	})
	return summary
}

// validateGoldenReplay 在创建 State 前拒绝当前版本无法解释的黄金样本结构。
func validateGoldenReplay(replay GoldenReplay) error {
	if replay.SchemaVersion != GoldenReplaySchemaVersion {
		return fmt.Errorf("%w: schemaVersion=%d", ErrInvalidGoldenReplay, replay.SchemaVersion)
	}
	if len(replay.Turns) == 0 {
		return fmt.Errorf("%w: 至少需要一个回合", ErrInvalidGoldenReplay)
	}
	return nil
}

// compareEvents 把当前强类型事件与 fixture 原始 JSON 做结构化比较，不依赖对象字段文本顺序或空白。
func compareEvents(actual []Event, expected []json.RawMessage) error {
	if len(actual) != len(expected) {
		return fmt.Errorf("事件数量为 %d，预期 %d", len(actual), len(expected))
	}
	for index, event := range actual {
		actualJSON, err := json.Marshal(event)
		if err != nil {
			return fmt.Errorf("编码第 %d 个实际事件: %w", index+1, err)
		}
		if !sameJSON(actualJSON, expected[index]) {
			return fmt.Errorf("第 %d 个事件不同", index+1)
		}
	}
	return nil
}

// sameJSON 比较两个 JSON 值的结构语义，并把不合法的 fixture 值视为不匹配。
func sameJSON(left, right []byte) bool {
	var leftValue any
	if err := json.Unmarshal(left, &leftValue); err != nil {
		return false
	}
	var rightValue any
	if err := json.Unmarshal(right, &rightValue); err != nil {
		return false
	}
	return reflect.DeepEqual(leftValue, rightValue)
}

// cloneStatStageSummary 复制成员能力阶级映射，避免调用方通过摘要修改权威状态内存。
func cloneStatStageSummary(source map[Stat]int8) map[Stat]int8 {
	if source == nil {
		return nil
	}
	cloned := make(map[Stat]int8, len(source))
	for stat, stage := range source {
		cloned[stat] = stage
	}
	return cloned
}
