package battleengine

// extendTerrainDurationByHeldItem 在成员当前持有全场地延长道具时抬高即将建立的普通场地持续回合。
// 四种普通场地属于同一固定道具规则；天气、强天气、无道具成员和更长来源值均保持不变。
func extendTerrainDurationByHeldItem(state State, source MemberRef, effect TerrainEffect) TerrainEffect {
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemTerrainTurnsRemaining <= effect.TurnsRemaining {
		return effect
	}
	effect.TurnsRemaining = member.HeldItemTerrainTurnsRemaining
	return effect
}
