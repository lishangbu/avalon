package battleengine

// FormChangedEvent 记录成员已经从一种冻结形态画像切换到另一种。
//
// 事件只携带稳定 Identifier 与封闭原因，不暴露或依赖实时资料中的名称、形态代码或任意效果文本；因此重放能够在
// 不连接资料库的条件下完整还原身份与能力变化。
type FormChangedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场阶段使用第 0 回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是发生形态变化的成员稳定引用。
	Member MemberRef `json:"member"`
	// FromCreatureID 是变化前的形态稳定 Identifier。
	FromCreatureID Identifier `json:"fromCreatureId"`
	// ToCreatureID 是变化后的形态稳定 Identifier。
	ToCreatureID Identifier `json:"toCreatureId"`
	// Reason 是此次形态变化的封闭触发原因。
	Reason FormChangeReason `json:"reason"`
}

// Kind 返回 formChanged。
func (event FormChangedEvent) Kind() EventKind { return event.Type }

// HeldItemElementIdentityAppliedEvent 记录成员已按所持道具冻结的属性伤害强化身份替换为单属性。
//
// 事件只编码本场快照已有的成员、道具和属性稳定 Identifier；没有道具、道具没有属性身份或原本已经是该单属性时
// 不产生空事件，重放无需访问 Item Metadata 即可确认真实属性变化原因。
type HeldItemElementIdentityAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场阶段使用第 0 回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是当前属性被替换的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是提供属性伤害强化身份的冻结持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// ElementID 是该道具指定并已经写入成员当前属性的稳定 Identifier。
	ElementID Identifier `json:"elementId"`
}

// Kind 返回 heldItemElementIdentityApplied。
func (event HeldItemElementIdentityAppliedEvent) Kind() EventKind { return event.Type }

// HeldItemHighestStatBoostActivatedEvent 记录成员已消耗一件道具，并按当前最高原始能力建立持续强化。
//
// ItemID 是消耗前的稳定道具 Identifier，AbilityID 是当时实际生效的特性，Stat 是固定优先级计算出的能力项。
// 事件发生后 State 中的 ItemID 必须为空而 BoosterEnergyStat 必须等于 Stat，使重放、审计和客户端状态可共同
// 验证道具既已消耗又仍保留强化。
type HeldItemHighestStatBoostActivatedEvent struct {
	// Type 是固定事件种类 heldItemHighestStatBoostActivated。
	Type EventKind `json:"type"`
	// SchemaVersion 是事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始入场阶段固定为 0。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是消耗道具并获得强化的稳定成员引用。
	Member MemberRef `json:"member"`
	// ItemID 是道具在被清空前的稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// AbilityID 是本次允许消耗该道具的当前实际特性 Identifier 文本。
	AbilityID Identifier `json:"abilityId"`
	// Stat 是本次按冻结五项原始能力和固定优先级选出的持续强化能力项。
	Stat Stat `json:"stat"`
}

// Kind 返回 heldItemHighestStatBoostActivated。
func (event HeldItemHighestStatBoostActivatedEvent) Kind() EventKind { return event.Type }
