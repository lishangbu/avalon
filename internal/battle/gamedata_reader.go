package battle

import "context"
import "encoding/json"
import "fmt"
import "reflect"
import "strings"
import "time"
import "github.com/lishangbu/avalon/internal/platform/snowflake"
import "github.com/lishangbu/avalon/internal/battleengine"
import "github.com/lishangbu/avalon/internal/gamedata/ability"
import "github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
import "github.com/lishangbu/avalon/internal/gamedata/battleformat"
import "github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
import "github.com/lishangbu/avalon/internal/gamedata/element"
import "github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
import "github.com/lishangbu/avalon/internal/gamedata/itemrules"
import "github.com/lishangbu/avalon/internal/gamedata/nature"
import "github.com/lishangbu/avalon/internal/gamedata/skill"
import "github.com/lishangbu/avalon/internal/gamedata/skillailment"
import "github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
import "github.com/lishangbu/avalon/internal/gamedata/skilldetail"
import "github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
import "github.com/lishangbu/avalon/internal/gamedata/skilltarget"
import "github.com/lishangbu/avalon/internal/gamedata/stat"

const initialStateDataPageSize int32 = 100

// InitialStateDataFormatQuery 读取当前启用的 BattleFormat。
type InitialStateDataFormatQuery interface {
	GetFormat(context.Context, snowflake.ID) (battleformat.Format, error)
}

// InitialStateDataElementQuery 分页读取启用属性，以解析引擎的属性稳定编码映射。
type InitialStateDataElementQuery interface {
	List(context.Context, element.ListQuery) (element.Page, error)
}

// InitialStateDataElementEffectivenessQuery 读取全部启用的非中性属性克制倍率。
type InitialStateDataElementEffectivenessQuery interface {
	ListEnabled(context.Context) ([]elementeffectiveness.Effectiveness, error)
}

// InitialStateDataAbilityQuery 读取冻结 Team 成员引用的特性主资料。
//
// Battle 必须确认特性仍启用，不能只冻结 Team 中的 Identifier 后根据名称或说明文本猜测运行时规则。
type InitialStateDataAbilityQuery interface {
	Get(context.Context, snowflake.ID) (ability.Ability, error)
}

// InitialStateDataSkillQuery 读取冻结 Team 技能槽位引用的完整实时资料。
type InitialStateDataSkillQuery interface {
	Get(context.Context, snowflake.ID) (skill.Skill, error)
}

// InitialStateDataDamageClassQuery 读取技能伤害分类，以映射到战斗引擎的物理、特殊或变化类别。
type InitialStateDataDamageClassQuery interface {
	Get(context.Context, snowflake.ID) (skilldamageclass.DamageClass, error)
}

// InitialStateDataAilmentQuery 读取技能详情引用的异常稳定编码。
type InitialStateDataAilmentQuery interface {
	Get(context.Context, snowflake.ID) (skillailment.Ailment, error)
}

// InitialStateDataTargetQuery 读取技能详情引用的技能目标范围稳定编码。
type InitialStateDataTargetQuery interface {
	Get(context.Context, snowflake.ID) (skilltarget.Target, error)
}

// InitialStateDataStatChangeQuery 读取技能对能力阶段的全部实时变化记录。
type InitialStateDataStatChangeQuery interface {
	List(context.Context, skillstatchange.ListQuery) (skillstatchange.Page, error)
}

// InitialStateDataStatQuery 读取 Team 培养数值与精灵基础数值引用的稳定编码。
type InitialStateDataStatQuery interface {
	Get(context.Context, snowflake.ID) (stat.Stat, error)
}

// InitialStateDataNatureQuery 读取 Team 成员引用的 Nature 修正资料。
type InitialStateDataNatureQuery interface {
	Get(context.Context, snowflake.ID) (nature.Nature, error)
}

// InitialStateDataCreatureQuery 读取含精灵、形态、基础能力和可学习关系的完整实时资料聚合。
type InitialStateDataCreatureQuery interface {
	Get(context.Context) (creaturemetadata.Snapshot, error)
}

// InitialStateDataItemRulesQuery 读取包含道具属性伤害强化身份的完整实时道具资料聚合。
//
// 读取器只在 Battle 启动时访问此聚合，并把命中的属性 Identifier 冻结到成员快照；战斗运行期不会持有此查询或
// 读取其效果文本。资料尚未建立时可以被视为空聚合，因为普通持有道具并不要求具有复杂详情。
type InitialStateDataItemRulesQuery interface {
	GetItemRules(context.Context) (itemrules.Projection, error)
}

// GameDataInitialStateFactsReader 将实时游戏资料和已冻结 Team 编译为 Battle 启动输入。
//
// 读取器不共享管理端的 CRUD 结构：它只做一次方向明确的运行时投影。资料变更由部署方在停机期间完成，
// 在线启动路径只负责核对本场已经冻结的赛制快照和当前可执行资料。
type GameDataInitialStateFactsReader struct {
	// formats 提供与 Battle 冻结赛制核对后的实时赛制。
	formats InitialStateDataFormatQuery
	// elements 提供状态免疫规则使用的属性 code 到 Identifier 映射。
	elements InitialStateDataElementQuery
	// elementEffectiveness 提供冻结到规则快照的非中性属性倍率。
	elementEffectiveness InitialStateDataElementEffectivenessQuery
	// abilities 提供成员所选特性的启用状态。
	abilities InitialStateDataAbilityQuery
	// skills 提供成员技能槽位的基础战斗参数。
	skills InitialStateDataSkillQuery
	// damageClasses 将实时技能伤害分类映射到引擎有限枚举。
	damageClasses InitialStateDataDamageClassQuery
	// ailments 解析技能详情引用的异常 code。
	ailments InitialStateDataAilmentQuery
	// targets 解析技能详情引用的范围目标 code。
	targets InitialStateDataTargetQuery
	// statChanges 读取技能定义的所有能力阶段变化。
	statChanges InitialStateDataStatChangeQuery
	// stats 把实时数值项目映射到战斗引擎识别的基础能力 code。
	stats InitialStateDataStatQuery
	// natures 提供成员最终能力计算所需的 Nature 修正。
	natures InitialStateDataNatureQuery
	// creatures 提供精灵形态、基础能力和属性关系。
	creatures InitialStateDataCreatureQuery
	// itemRules 提供道具可选的属性伤害强化身份。
	itemRules InitialStateDataItemRulesQuery
	// rules 将 BattleFormat 引用的 Clause、Restriction 与 Mechanic 编译为冻结引擎规则。
	rules *BattleFormatRuleCompiler
}

// NewGameDataInitialStateFactsReader 使用显式实时资料读取边界创建 Battle 初始状态事实读取器。
func NewGameDataInitialStateFactsReader(
	formats InitialStateDataFormatQuery,
	elements InitialStateDataElementQuery,
	elementEffectiveness InitialStateDataElementEffectivenessQuery,
	abilities InitialStateDataAbilityQuery,
	skills InitialStateDataSkillQuery,
	damageClasses InitialStateDataDamageClassQuery,
	ailments InitialStateDataAilmentQuery,
	targets InitialStateDataTargetQuery,
	statChanges InitialStateDataStatChangeQuery,
	stats InitialStateDataStatQuery,
	natures InitialStateDataNatureQuery,
	creatures InitialStateDataCreatureQuery,
	itemRules InitialStateDataItemRulesQuery,
) *GameDataInitialStateFactsReader {
	return NewGameDataInitialStateFactsReaderWithRules(
		formats, elements, elementEffectiveness, abilities, skills, damageClasses, ailments, targets, statChanges, stats, natures, creatures, itemRules, nil,
	)
}

// NewGameDataInitialStateFactsReaderWithRules 创建会把赛制规则组件编译进 Battle Engine 快照的资料读取器。
func NewGameDataInitialStateFactsReaderWithRules(
	formats InitialStateDataFormatQuery,
	elements InitialStateDataElementQuery,
	elementEffectiveness InitialStateDataElementEffectivenessQuery,
	abilities InitialStateDataAbilityQuery,
	skills InitialStateDataSkillQuery,
	damageClasses InitialStateDataDamageClassQuery,
	ailments InitialStateDataAilmentQuery,
	targets InitialStateDataTargetQuery,
	statChanges InitialStateDataStatChangeQuery,
	stats InitialStateDataStatQuery,
	natures InitialStateDataNatureQuery,
	creatures InitialStateDataCreatureQuery,
	itemRules InitialStateDataItemRulesQuery,
	rules *BattleFormatRuleCompiler,
) *GameDataInitialStateFactsReader {
	return &GameDataInitialStateFactsReader{
		formats: formats, elements: elements, elementEffectiveness: elementEffectiveness, abilities: abilities,
		skills: skills, damageClasses: damageClasses, ailments: ailments, targets: targets,
		statChanges: statChanges, stats: stats, natures: natures, creatures: creatures, itemRules: itemRules, rules: rules,
	}
}

// ReadInitialStateFacts 使用当前可执行资料编译双方冻结 Team。
func (reader *GameDataInitialStateFactsReader) ReadInitialStateFacts(ctx context.Context, session Battle) (InitialStateFacts, error) {
	if reader == nil || reader.formats == nil || reader.elements == nil || reader.elementEffectiveness == nil || reader.abilities == nil ||
		reader.skills == nil || reader.damageClasses == nil || reader.ailments == nil || reader.targets == nil ||
		reader.statChanges == nil || reader.stats == nil || reader.natures == nil || reader.creatures == nil || reader.itemRules == nil || session.Status != StatusRunning || !session.StartedAt.IsZero() ||
		session.BattleFormatID == snowflake.ID(0) {
		return InitialStateFacts{}, ErrInitialStateCompilation
	}
	format, err := reader.formats.GetFormat(ctx, session.BattleFormatID)
	if err != nil {
		return InitialStateFacts{}, fmt.Errorf("读取 Battle BattleFormat: %w", err)
	}
	if !sameBattleFormat(session.Format, format) || !sameBattleFormatSnapshot(session.BattleFormatSnapshot, format) {
		return InitialStateFacts{}, fmt.Errorf("%w: 冻结赛制与实时资料不一致", ErrInitialStateCompilation)
	}
	elements, err := reader.readEnabledElements(ctx)
	if err != nil {
		return InitialStateFacts{}, err
	}
	rules := battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: elements,
	}
	if reader.rules != nil {
		rules, err = reader.rules.CompileRules(ctx, format)
		if err != nil {
			return InitialStateFacts{}, err
		}
		rules.ElementIDs = elements
	}
	effectiveness, err := reader.elementEffectiveness.ListEnabled(ctx)
	if err != nil {
		return InitialStateFacts{}, fmt.Errorf("读取属性克制实时资料: %w", err)
	}
	rules.ElementEffectiveness = make([]battleengine.ElementEffectiveness, len(effectiveness))
	for index, value := range effectiveness {
		rules.ElementEffectiveness[index] = battleengine.ElementEffectiveness{AttackElementID: value.AttackElementID, DefenseElementID: value.DefenseElementID, Numerator: value.Numerator, Denominator: value.Denominator}
	}
	metadata, err := reader.creatures.Get(ctx)
	if err != nil {
		return InitialStateFacts{}, fmt.Errorf("读取精灵实时资料: %w", err)
	}
	itemRulesSnapshot, err := reader.itemRules.GetItemRules(ctx)
	if err != nil {
		return InitialStateFacts{}, fmt.Errorf("读取道具实时资料: %w", err)
	}
	dataSnapshot, err := loadInitialStateDataSnapshot(ctx, reader, session, metadata.Data, itemRulesSnapshot)
	if err != nil {
		return InitialStateFacts{}, err
	}
	compiler := initialMemberCompiler{
		ctx: ctx, snapshot: dataSnapshot, format: format, normalizedLevel: rules.NormalizedLevel, elements: elements,
		metadata: dataSnapshot.metadata, itemRules: dataSnapshot.itemRules,
		skills: make(map[snowflake.ID]battleengine.SkillSnapshot), stats: dataSnapshot.stats,
		abilities: dataSnapshot.abilities, abilityDetails: dataSnapshot.abilityDetail,
		damageClasses: make(map[snowflake.ID]battleengine.DamageClass), details: dataSnapshot.details,
		ailments: make(map[snowflake.ID]battleengine.MajorStatus), targets: make(map[snowflake.ID]battleengine.SkillTargetScope),
		statChanges: dataSnapshot.statChanges, natures: dataSnapshot.natures,
	}
	facts := InitialStateFacts{
		Format: battleengine.FormatSnapshot{
			Code: format.Code, TeamSize: uint8(session.Format.SelectCount),
			ActiveSlotsPerSide: battleengine.SlotPosition(session.Format.ActiveParticipantsPerSide),
		},
		Rules: rules,
		Sides: make([]BattleSideFacts, 0, len(session.Participants)),
	}
	for _, participant := range session.Participants {
		members, compileErr := compiler.compileTeam(participant.Team)
		if compileErr != nil {
			return InitialStateFacts{}, compileErr
		}
		facts.Sides = append(facts.Sides, BattleSideFacts{Side: participant.Side, Members: members})
	}
	return facts, nil
}

func (reader *GameDataInitialStateFactsReader) readEnabledElements(ctx context.Context) (map[string]battleengine.Identifier, error) {
	result := make(map[string]battleengine.Identifier)
	enabled := true
	for page := int32(1); ; page++ {
		current, err := reader.elements.List(ctx, element.ListQuery{Page: page, PageSize: initialStateDataPageSize, Enabled: &enabled})
		if err != nil {
			return nil, fmt.Errorf("读取启用属性资料: %w", err)
		}
		for _, value := range current.Items {
			if !value.Enabled || strings.TrimSpace(value.Code) == "" || value.ID == snowflake.ID(0) {
				return nil, ErrInitialStateCompilation
			}
			if _, exists := result[value.Code]; exists {
				return nil, ErrInitialStateCompilation
			}
			result[value.Code] = value.ID
		}
		if int64(len(result)) >= current.Total || len(current.Items) == 0 {
			return result, nil
		}
	}
}

func sameBattleFormat(battleFormat Format, format battleformat.Format) bool {
	return format.Enabled && int32(battleFormat.RosterCount) == format.RosterCount &&
		int32(battleFormat.SelectCount) == format.SelectCount &&
		int32(battleFormat.ActiveParticipantsPerSide) == format.ActiveParticipantsPerSide &&
		battleFormat.PreviewDuration == durationSeconds(format.Deadlines.PreviewSeconds) &&
		battleFormat.BattleDuration == durationSeconds(format.Deadlines.BattleSeconds)
}

// sameBattleFormatSnapshot 确认当前实时赛制仍等于 Battle 创建时冻结的完整资料。
//
// 比较完整快照可以防止手工数据库操作或未来新增字段悄悄改变已创建 Battle 的规则语义。
func sameBattleFormatSnapshot(raw json.RawMessage, current battleformat.Format) bool {
	if !json.Valid(raw) {
		return false
	}
	var frozen battleformat.Format
	if err := json.Unmarshal(raw, &frozen); err != nil {
		return false
	}
	return reflect.DeepEqual(frozen, current)
}

func durationSeconds(seconds int32) time.Duration { return time.Duration(seconds) * time.Second }

// initialMemberCompiler 只在单次读取中缓存已解析的资料，不跨 Battle 保存可变目录。
type initialMemberCompiler struct {
	// ctx 继承启动请求的取消和截止时间，禁止在资料读取中脱离调用链。
	ctx      context.Context
	snapshot initialStateDataSnapshot
	format   battleformat.Format
	// normalizedLevel 是由 Level Normalization Mechanic 编译出的本场固定等级；0 表示使用赛制基础等级规则。
	normalizedLevel uint8
	elements        map[string]battleengine.Identifier
	metadata        creaturemetadata.Data
	itemRules       itemrules.Projection
	skills          map[snowflake.ID]battleengine.SkillSnapshot
	stats           map[snowflake.ID]stat.Stat
	// natures 缓存同一次初始状态读取内已经验证的 Nature。
	natures map[snowflake.ID]nature.Nature
	// abilities 缓存同一次初始状态读取内已验证的启用特性。
	abilities map[snowflake.ID]ability.Ability
	// abilityDetails 缓存同一次初始状态读取内的特性详情；nil 表示该启用特性没有详情记录。
	abilityDetails map[snowflake.ID]*abilitydetail.RuleSet
	damageClasses  map[snowflake.ID]battleengine.DamageClass
	details        map[snowflake.ID]skilldetail.RuleSet
	ailments       map[snowflake.ID]battleengine.MajorStatus
	targets        map[snowflake.ID]battleengine.SkillTargetScope
	statChanges    map[snowflake.ID][]skillstatchange.Change
}

// abilityAccuracyRules 是单条特性详情编译后写入成员快照的全部命中规则。
//
// 该私有结构只用于 Battle 编译期间汇集同一份详情的独立字段，既不作为持久化模型也不作为通用效果容器；进入
// Battle Engine 后仍由 MemberSnapshot 中的明确字段保存每条规则的独立语义。
type abilityAccuracyRules struct {
	accuracyMultiplier                          *battleengine.AccuracyMultiplier
	physicalSkillAccuracyMultiplier             *battleengine.AccuracyMultiplier
	opponentAccuracySandstormMultiplier         *battleengine.AccuracyMultiplier
	opponentAccuracySnowMultiplier              *battleengine.AccuracyMultiplier
	opponentAccuracyConfusionMultiplier         *battleengine.AccuracyMultiplier
	accuracyAlwaysHits                          bool
	statusSkillAccuracyCap                      uint8
	ignoreOpponentAccuracyStatStages            bool
	criticalHitImmunity                         bool
	skillRecoilDamageImmunity                   bool
	indirectDamageImmunity                      bool
	contactDamageToAttackerDenominator          uint16
	ignoreOpponentDamageStatStages              bool
	ignoreTargetAbilityEffects                  bool
	surviveFatalDamageAtFullHP                  bool
	opponentStatusSkillImmunity                 bool
	nonSuperEffectiveDamageImmunity             bool
	criticalHitStageBoost                       uint8
	multiHitMaximum                             bool
	damagingSkillSecondaryEffectImmunity        bool
	priorityMoveImmunityForSideEnabled          bool
	priorityMoveImmunityForSideProtectsAllies   bool
	statusSkillMovesLastAndIgnoresTargetAbility bool
	contactSkillProtectionBypass                bool
	contactSuppression                          bool
	receivedContactDamageHalved                 bool
	receivedFireDamageDoubled                   bool
}

// abilityDamageMultiplierRules 汇集一条特性详情编译后的十类攻击方最终伤害倍率。
// 它只存在于 Battle 编译期间；每条规则仍以独立强类型字段冻结到成员事实，不能折叠成通用效果集合。
type abilityDamageMultiplierRules struct {
	basePowerAtMostDamageBoost   *battleengine.BasePowerAtMostDamageBoost
	recoilSkillDamageBoost       *battleengine.RecoilSkillDamageBoost
	lowHPElementDamageBoost      *battleengine.LowHPElementDamageBoost
	weatherElementDamageBoost    *battleengine.WeatherElementDamageBoost
	elementSkillDamageBoost      *battleengine.ElementSkillDamageBoost
	sameElementBonusOverride     *battleengine.SameElementBonusOverride
	contactBasedSkillDamageBoost *battleengine.ContactBasedSkillDamageBoost
	criticalHitDamageBoost       *battleengine.CriticalHitDamageBoost
	superEffectiveDamageBoost    *battleengine.SuperEffectiveDamageBoost
	notVeryEffectiveDamageBoost  *battleengine.NotVeryEffectiveDamageBoost
}

// compileChargeSkippedWeathers 将资料层跳过蓄力天气集合冻结到纯战斗引擎快照。
//
// 非空集合必须与一项 charging 易变状态共同存在；这个交叉约束在 Battle 启动时再次校验，阻止直接修改数据库的
// 资料把永远不会触发、或会错误跳过普通技能生命周期的规则带入权威对局。
func compileChargeSkippedWeathers(
	compiled *battleengine.SkillSnapshot,
	values []skilldetail.WeatherKind,
	volatileEffects []skilldetail.VolatileEffect,
) error {
	if len(values) > 4 {
		return ErrInitialStateCompilation
	}
	if len(values) != 0 {
		hasCharging := false
		for _, effect := range volatileEffects {
			if effect.Status == skilldetail.VolatileStatusCharging {
				hasCharging = true
				break
			}
		}
		if !hasCharging {
			return ErrInitialStateCompilation
		}
	}
	weathers := make([]battleengine.WeatherKind, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Valid() {
			return ErrInitialStateCompilation
		}
		if _, duplicated := seen[value]; duplicated {
			return ErrInitialStateCompilation
		}
		seen[value] = struct{}{}
		switch value {
		case skilldetail.WeatherKindSun:
			weathers = append(weathers, battleengine.WeatherKindSun)
		case skilldetail.WeatherKindRain:
			weathers = append(weathers, battleengine.WeatherKindRain)
		case skilldetail.WeatherKindSandstorm:
			weathers = append(weathers, battleengine.WeatherKindSandstorm)
		case skilldetail.WeatherKindSnow:
			weathers = append(weathers, battleengine.WeatherKindSnow)
		default:
			return ErrInitialStateCompilation
		}
	}
	compiled.ChargeSkippedWeathers = weathers
	return nil
}

// heldItemElementID 从启动时读取的完整道具资料中解析一件持有道具的属性伤害强化身份。
//
// 没有道具、没有详情或详情没有该身份都返回空字符串。聚合内出现重复详情、空 Identifier 或指向非启用属性则说明
// 资料已绕过维护期校验，必须拒绝新 Battle，不能依赖数组顺序选取任意一项。
func (compiler *initialMemberCompiler) heldItemElementID(itemID *snowflake.ID) (snowflake.ID, error) {
	if itemID == nil {
		return 0, nil
	}
	var result *snowflake.ID
	foundDetail := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.ElementDamageBoostElementID
	}
	if result == nil {
		return 0, nil
	}
	if *result == snowflake.ID(0) {
		return 0, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*result); !enabled {
		return 0, ErrInitialStateCompilation
	}
	return *result, nil
}

// highestStatBoosterAbilityIDs 解析一件持有道具允许消耗它的特性集合，并转换为引擎只读 Identifier 文本。
//
// Item Metadata 的详情必须一一对应道具；缺失详情或空集合都合法地表示没有道具效果。存在集合时必须保持原有
// 稳定顺序、没有重复、每个 Identifier 指向启用特性，不能让运行时通过名称、效果文本或数据库数组顺序猜测触发器。
func (compiler *initialMemberCompiler) highestStatBoosterAbilityIDs(itemID *snowflake.ID) ([]battleengine.Identifier, error) {
	if itemID == nil {
		return []battleengine.Identifier{}, nil
	}
	var result []snowflake.ID
	foundDetail := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return nil, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.HighestStatBoosterAbilityIDs
	}
	if len(result) > 16 {
		return nil, ErrInitialStateCompilation
	}
	compiled := make([]battleengine.Identifier, 0, len(result))
	seen := make(map[snowflake.ID]struct{}, len(result))
	for _, abilityID := range result {
		if abilityID == snowflake.ID(0) {
			return nil, ErrInitialStateCompilation
		}
		if _, duplicate := seen[abilityID]; duplicate {
			return nil, ErrInitialStateCompilation
		}
		seen[abilityID] = struct{}{}
		if _, err := compiler.ability(abilityID); err != nil {
			return nil, err
		}
		compiled = append(compiled, abilityID)
	}
	return compiled, nil
}

// itemUtilityEffectFacts 汇集一组彼此独立、无需额外参数的持有道具运行时开关。
type itemUtilityEffectFacts struct {
	damageDealtHeal, drainHealingBoost, accuracyBoost, opponentAccuracyReduction bool
	criticalHitStageBoost, airborneUntilDamaged, forceGrounded, speedHalf        bool
	specialDefenseBoost, statusSkillRestriction                                  bool
	physicalDamagePowerBoost50, specialDamagePowerBoost50, choiceSkillLock       bool
	speedBoost50, accuracyAfterTargetActedBoost, typeImmunitySuppression         bool
	opponentStatStageReductionImmunity, negativeStatStageReset                   bool
	abilityStatReductionSpeedBoost, opponentPositiveStatStageCopy                bool
	damagingSkillSecondaryEffectImmunity                                         bool
	bindingTurns                                                                 uint8
	bindingDamageDenominator                                                     uint16
	accuracyMissStatStageBoostStat                                               battleengine.Stat
	accuracyMissStatStageBoostDelta                                              int8
	weaknessPolicy                                                               bool
	waterDamageSpecialAttackBoostElementID, electricDamageAttackBoostElementID   snowflake.ID
	waterDamageSpecialDefenseBoostElementID, iceDamageAttackBoostElementID       snowflake.ID
	additionalFlinchChancePercent, randomActionOrderBoostChancePercent           uint8
	forcedLastActionOrder, lowHPActionOrderBoost                                 bool
	fieldSpeedOrderSpeedStageDrop, consecutiveSkillDamageBoost                   bool
}

func (compiler *initialMemberCompiler) elementIDEnabled(id snowflake.ID) (string, bool) {
	for code, value := range compiler.elements {
		if value == id {
			return code, true
		}
	}
	return "", false
}
