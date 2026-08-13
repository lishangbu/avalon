package battleengine

import (
	"errors"
	"fmt"
)

var (
	// ErrInvalidTurnCommand 表示完整回合命令缺少行动、引用了无效槽位或违反当前状态要求。
	ErrInvalidTurnCommand = errors.New("无效的回合命令")
)

// TurnCommandErrorCode 是非法回合命令可写入黄金样本的稳定原因代码。
type TurnCommandErrorCode string

const (
	// TurnCommandErrorInvalidSchemaVersion 表示命令结构版本不受支持。
	TurnCommandErrorInvalidSchemaVersion TurnCommandErrorCode = "invalidSchemaVersion"
	// TurnCommandErrorUnexpectedTurnNumber 表示命令回合号不是当前状态的下一回合。
	TurnCommandErrorUnexpectedTurnNumber TurnCommandErrorCode = "unexpectedTurnNumber"
	// TurnCommandErrorInvalidTimeInput 表示显式逻辑时间输入为负数。
	TurnCommandErrorInvalidTimeInput TurnCommandErrorCode = "invalidTimeInput"
	// TurnCommandErrorBattleEnded 表示调用方尝试继续结算已终局战斗。
	TurnCommandErrorBattleEnded TurnCommandErrorCode = "battleEnded"
	// TurnCommandErrorIncompleteActions 表示命令没有覆盖全部当前场上槽位。
	TurnCommandErrorIncompleteActions TurnCommandErrorCode = "incompleteActions"
	// TurnCommandErrorDuplicateActor 表示同一个场上槽位提交了多个行动。
	TurnCommandErrorDuplicateActor TurnCommandErrorCode = "duplicateActor"
	// TurnCommandErrorInvalidActor 表示行动者不是当前状态中的有效场上成员。
	TurnCommandErrorInvalidActor TurnCommandErrorCode = "invalidActor"
	// TurnCommandErrorInvalidActionShape 表示行动种类和专属载荷不匹配。
	TurnCommandErrorInvalidActionShape TurnCommandErrorCode = "invalidActionShape"
	// TurnCommandErrorFaintedActor 表示倒下成员尝试执行非补位行动。
	TurnCommandErrorFaintedActor TurnCommandErrorCode = "faintedActor"
	// TurnCommandErrorInvalidSkillPosition 表示技能位置不属于行动者当前技能列表。
	TurnCommandErrorInvalidSkillPosition TurnCommandErrorCode = "invalidSkillPosition"
	// TurnCommandErrorSkillUnavailable 表示技能没有 PP 或尚无已实现的可执行效果。
	TurnCommandErrorSkillUnavailable TurnCommandErrorCode = "skillUnavailable"
	// TurnCommandErrorInvalidTarget 表示技能或替换目标不满足当前状态要求。
	TurnCommandErrorInvalidTarget TurnCommandErrorCode = "invalidTarget"
	// TurnCommandErrorDuplicateSwitchTarget 表示多个双打槽位选择了同一后备成员。
	TurnCommandErrorDuplicateSwitchTarget TurnCommandErrorCode = "duplicateSwitchTarget"
	// TurnCommandErrorSwitchPrevented 表示束缚、蓄力或锁招使当前成员不能选择主动换人。
	TurnCommandErrorSwitchPrevented TurnCommandErrorCode = "switchPrevented"
	// TurnCommandErrorForcedSkill 表示蓄力或锁招要求成员使用指定技能，但客户端提交了其它行动。
	TurnCommandErrorForcedSkill TurnCommandErrorCode = "forcedSkill"
	// TurnCommandErrorUnsupportedActionKind 表示行动种类尚未由当前引擎版本实现。
	TurnCommandErrorUnsupportedActionKind TurnCommandErrorCode = "unsupportedActionKind"
	// TurnCommandErrorTerastallizationDisabled 表示当前赛制没有冻结允许太晶化的特殊机制。
	TurnCommandErrorTerastallizationDisabled TurnCommandErrorCode = "terastallizationDisabled"
	// TurnCommandErrorTerastallizationAlreadyUsed 表示行动方已经消耗本局唯一的太晶化机会。
	TurnCommandErrorTerastallizationAlreadyUsed TurnCommandErrorCode = "terastallizationAlreadyUsed"
	// TurnCommandErrorActorAlreadyTerastallized 表示同一成员重复请求太晶化。
	TurnCommandErrorActorAlreadyTerastallized TurnCommandErrorCode = "actorAlreadyTerastallized"
	// TurnCommandErrorTeraElementUnavailable 表示成员没有可冻结为单属性的太晶属性。
	TurnCommandErrorTeraElementUnavailable TurnCommandErrorCode = "teraElementUnavailable"
)

// TurnCommandError 是包含稳定原因和字段路径的结构化命令校验错误。
type TurnCommandError struct {
	// Code 是可用于黄金样本和 API 映射的稳定机器原因。
	Code TurnCommandErrorCode `json:"code"`
	// Field 是相对于 TurnCommand JSON 根对象的字段路径。
	Field string `json:"field"`
	// Message 是供日志和简体中文错误响应使用的具体说明。
	Message string `json:"message"`
}

// Error 返回包含稳定 code、字段路径和说明的错误文本。
func (commandError *TurnCommandError) Error() string {
	return fmt.Sprintf("%s %s: %s", commandError.Code, commandError.Field, commandError.Message)
}

// Unwrap 使调用方仍可使用 errors.Is 识别 ErrInvalidTurnCommand 大类。
func (commandError *TurnCommandError) Unwrap() error {
	return ErrInvalidTurnCommand
}

// TimeInput 是一次回合结算显式接收的逻辑时间输入。
//
// 纯引擎不会读取系统时钟；需要时间语义的规则只能读取 Battle Runtime 持久化到命令中的该值。
type TimeInput struct {
	// ElapsedMilliseconds 是从 Battle 战斗阶段开始到本命令截止时的非负毫秒数。
	ElapsedMilliseconds int32 `json:"elapsedMilliseconds"`
}

// ActionKind 是回合内单个场上槽位选择的行动种类。
type ActionKind string

const (
	// ActionKindUseSkill 表示当前成员使用一个技能槽。
	ActionKindUseSkill ActionKind = "useSkill"
	// ActionKindSwitch 表示当前槽位主动替换为同侧后备成员。
	ActionKindSwitch ActionKind = "switch"
)

// UseSkillAction 保存一次技能行动特有的命令字段。
type UseSkillAction struct {
	// SkillPosition 是行动者一至四号技能槽中的稳定位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// Target 是单体技能由玩家选择的目标场上槽位；目标成员先换出时仍指向同一个槽位。
	// 自身、范围和随机范围技能不以本字段决定实际目标，但保留它以维持所有技能行动的同一命令形状。
	Target SlotRef `json:"target"`
	// Terastallize 表示成员请求在本次技能真正开始结算前消耗己方整局唯一的太晶化机会。
	// 它只能随 useSkill 行动提交，不能单独构成一次行动；赛制许可、单方次数、成员太晶属性和重复使用均由
	// 引擎在完整回合命令校验时以冻结状态判定。
	Terastallize bool `json:"terastallize"`
}

// SwitchAction 保存一次主动替换行动特有的命令字段。
type SwitchAction struct {
	// MemberPosition 是准备换入当前场上槽位的同侧后备成员位置。
	MemberPosition MemberPosition `json:"memberPosition"`
}

// Action 是一个场上槽位在当前回合提交的完整选择。
//
// Kind 决定且只允许对应的可选结构存在，避免弱类型参数在不同命令种类间复用。
type Action struct {
	// Kind 是 useSkill 或 switch 等稳定行动种类。
	Kind ActionKind `json:"kind"`
	// Actor 是提交本次行动的当前场上槽位。
	Actor SlotRef `json:"actor"`
	// UseSkill 仅在 Kind 为 useSkill 时存在。
	UseSkill *UseSkillAction `json:"useSkill,omitempty"`
	// Switch 仅在 Kind 为 switch 时存在。
	Switch *SwitchAction `json:"switch,omitempty"`
}

// TurnCommand 是双方对当前回合全部人工选择要求的一次完整秘密提交结果。
//
// Battle Runtime 只有在双方命令都锁定后才组装该对象并调用纯引擎；引擎不接受半回合增量。
type TurnCommand struct {
	// SchemaVersion 是回合命令 JSON 结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是调用方期望结算的回合编号，必须等于当前状态编号加一。
	TurnNumber uint32 `json:"turnNumber"`
	// Time 是 Battle Runtime 提供且可随 Turn Record 重放的显式逻辑时间输入。
	Time TimeInput `json:"time"`
	// Actions 必须为当前回合每一个需要玩家决策的场上槽位提供且只提供一个行动。
	Actions []Action `json:"actions"`
}
