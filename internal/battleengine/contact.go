package battleengine

// bypassesPersonalProtectionByContactAbility 报告本次技能是否因使用者特性而绕过目标个人保护。
//
// 该判定只处理使用者和目标位于敌对阵营、使用者当前具备规则且技能本次仍为接触类的组合。它仅跳过本次
// 命中前的个人保护 gate：不会删除目标的 ProtectionTurnsRemaining，不会重置 ProtectionChain，也不改变
// 其它技能在同一回合继续受到保护阻止的事实。未来若有道具或其它规则动态改写接触事实，应只扩展
// skillMakesEffectiveContact，保持所有消费者使用同一语义入口。
func bypassesPersonalProtectionByContactAbility(
	actorSlot SlotRef,
	targetSlot SlotRef,
	actor MemberSnapshot,
	skill SkillSnapshot,
) bool {
	return actorSlot.Side != targetSlot.Side && actor.ContactSkillProtectionBypass && skillMakesEffectiveContact(actor, skill)
}

// skillMakesEffectiveContact 返回技能在当前使用者下是否实际构成接触。
//
// 技能资料标签为 false 时不能被特性或道具反向变为接触；接触抑制特性和拳击道具只消除本次结算的有效接触
// 事实，不会写回冻结资料。独立函数避免保护穿透、接触反制等入口各自复制并逐渐偏离判定条件。
func skillMakesEffectiveContact(actor MemberSnapshot, skill SkillSnapshot) bool {
	return skill.MakesContact && !actor.ContactSuppression &&
		(actor.ItemID == 0 || !actor.HeldItemPunchBasedContactSuppression || !skill.PunchBased)
}
