package skilldetail

// TerrainKind 是技能详情能够建立的普通全场场地的封闭资料代码。
//
// 场地与天气、全场速度顺序、成员易变状态和侧状态分别持久化。资料只声明场地种类、持续回合与触发概率；接地
// 判定、伤害修正、异常阻止及回合末回复由纯战斗引擎按已冻结快照解释，不能从技能名称或说明文本推断。
type TerrainKind string

const (
	// TerrainKindElectric 表示电气场地。
	TerrainKindElectric TerrainKind = "electric"
	// TerrainKindGrassy 表示青草场地。
	TerrainKindGrassy TerrainKind = "grassy"
	// TerrainKindMisty 表示薄雾场地。
	TerrainKindMisty TerrainKind = "misty"
	// TerrainKindPsychic 表示精神场地。
	TerrainKindPsychic TerrainKind = "psychic"
)

// Valid 报告场地种类是否能够由当前资料服务和纯战斗引擎共同解释。
func (kind TerrainKind) Valid() bool {
	return kind == TerrainKindElectric || kind == TerrainKindGrassy || kind == TerrainKindMisty || kind == TerrainKindPsychic
}

// Terrain 描述技能成功后尝试建立普通全场场地的完整资料。
//
// 再次成功使用同一种普通场地不会刷新持续回合，而是产生明确的技能失败事件；不同场地会覆盖当前场地。它不能与
// 天气、戏法空间或未来的侧状态共用泛型环境 JSON。
type Terrain struct {
	// Kind 是技能尝试建立的封闭普通场地种类。
	Kind TerrainKind `json:"kind"`
	// TurnsRemaining 是场地建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是场地建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validTerrain 校验可选场地资料的封闭种类和数值边界。
func validTerrain(value *Terrain) bool {
	return value == nil || value.Kind.Valid() && value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneTerrain 复制可选场地资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneTerrain(value *Terrain) *Terrain {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
