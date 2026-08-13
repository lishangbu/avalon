package rpg

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
)

// ConditionContext 是出口条件求值允许读取的最小玩家快照。
type ConditionContext struct {
	Level            int32
	Items            map[string]int32
	QuestObjectives  map[string]int32
	Professions      map[string]bool
	WorldStateSwitch map[string]bool
}

// Condition 是已经通过闭集编译器校验的出口条件。
type Condition interface{ Evaluate(ConditionContext) bool }

type conditionNode struct {
	op        string
	value     int32
	key       string
	boolValue bool
	children  []Condition
}

// Evaluate 在 fail-closed 语义下求值已编译条件。
func (node conditionNode) Evaluate(ctx ConditionContext) bool {
	switch node.op {
	case "all":
		for _, child := range node.children {
			if !child.Evaluate(ctx) {
				return false
			}
		}
		return true
	case "any":
		for _, child := range node.children {
			if child.Evaluate(ctx) {
				return true
			}
		}
		return false
	case "not":
		return len(node.children) == 1 && !node.children[0].Evaluate(ctx)
	case "level_gte":
		return ctx.Level >= node.value
	case "item_count_gte":
		return ctx.Items[node.key] >= node.value
	case "quest_objective_gte":
		return ctx.QuestObjectives[node.key] >= node.value
	case "profession":
		return ctx.Professions[node.key]
	case "world_state":
		return ctx.WorldStateSwitch[node.key] == node.boolValue
	default:
		return false
	}
}

// CompileCondition 将规范化 JSON 编译为后端闭集条件；未知字段和操作直接拒绝。
func CompileCondition(raw json.RawMessage) (Condition, error) {
	return compileCondition(raw, 0)
}

func compileCondition(raw json.RawMessage, depth int) (Condition, error) {
	if depth > 32 {
		return nil, errors.New("Exit Condition 嵌套深度超过 32")
	}
	var object map[string]json.RawMessage
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&object); err != nil || object == nil {
		return nil, errors.New("Exit Condition 必须是 JSON 对象")
	}
	var op string
	if err := decodeRequired(object, "op", &op); err != nil {
		return nil, err
	}
	allowed := map[string]bool{"op": true}
	switch op {
	case "all", "any":
		allowed["children"] = true
	case "not":
		allowed["child"] = true
	case "level_gte":
		allowed["value"] = true
	case "item_count_gte", "quest_objective_gte":
		allowed["key"], allowed["value"] = true, true
	case "profession":
		allowed["key"] = true
	case "world_state":
		allowed["key"], allowed["value"] = true, true
	}
	for key := range object {
		if !allowed[key] {
			return nil, fmt.Errorf("Exit Condition 操作 %s 不允许字段 %q", op, key)
		}
	}
	node := conditionNode{op: op}
	switch op {
	case "all", "any":
		var children []json.RawMessage
		if err := decodeRequired(object, "children", &children); err != nil {
			return nil, fmt.Errorf("%s.children 必须为数组", op)
		}
		for _, child := range children {
			compiled, err := compileCondition(child, depth+1)
			if err != nil {
				return nil, err
			}
			node.children = append(node.children, compiled)
		}
	case "not":
		var child json.RawMessage
		if err := decodeRequired(object, "child", &child); err != nil {
			return nil, err
		}
		compiled, err := compileCondition(child, depth+1)
		if err != nil {
			return nil, err
		}
		node.children = []Condition{compiled}
	case "level_gte":
		if err := decodeRequired(object, "value", &node.value); err != nil || node.value < 1 {
			return nil, errors.New("level_gte.value 必须大于零")
		}
	case "item_count_gte", "quest_objective_gte":
		if err := decodeRequired(object, "key", &node.key); err != nil || node.key == "" {
			return nil, errors.New("条件 key 不能为空")
		}
		if err := decodeRequired(object, "value", &node.value); err != nil || node.value < 1 {
			return nil, errors.New("条件 value 必须大于零")
		}
	case "profession":
		if err := decodeRequired(object, "key", &node.key); err != nil || node.key == "" {
			return nil, errors.New("profession.key 不能为空")
		}
	case "world_state":
		if err := decodeRequired(object, "key", &node.key); err != nil || node.key == "" {
			return nil, errors.New("world_state.key 不能为空")
		}
		if err := decodeRequired(object, "value", &node.boolValue); err != nil {
			return nil, errors.New("world_state.value 必须为布尔值")
		}
	default:
		return nil, fmt.Errorf("不支持的 Exit Condition 操作: %s", op)
	}
	return node, nil
}

func decodeRequired[T any](object map[string]json.RawMessage, key string, target *T) error {
	raw, ok := object[key]
	if !ok {
		return fmt.Errorf("缺少条件字段 %q", key)
	}
	if err := json.Unmarshal(raw, target); err != nil {
		return fmt.Errorf("条件字段 %q 格式无效: %w", key, err)
	}
	return nil
}

// TraversalEffect 是已经通过白名单编译的移动副作用。
type TraversalEffect interface{ Apply(*ConditionContext) }

type effect struct {
	op, key   string
	value     int32
	boolValue bool
}

func (value effect) Apply(ctx *ConditionContext) {
	if ctx == nil {
		return
	}
	switch value.op {
	case "set_world_state":
		if ctx.WorldStateSwitch == nil {
			ctx.WorldStateSwitch = map[string]bool{}
		}
		ctx.WorldStateSwitch[value.key] = value.boolValue
	case "increment_quest_objective":
		if ctx.QuestObjectives == nil {
			ctx.QuestObjectives = map[string]int32{}
		}
		ctx.QuestObjectives[value.key] += value.value
	}
}

// CompileEffect 编译只允许修改 World State 开关或 Quest Objective 进度的副作用。
func CompileEffect(raw json.RawMessage) (TraversalEffect, error) {
	if len(bytes.TrimSpace(raw)) == 0 || bytes.Equal(bytes.TrimSpace(raw), []byte("null")) {
		return nil, nil
	}
	var object map[string]json.RawMessage
	if err := json.Unmarshal(raw, &object); err != nil || object == nil {
		return nil, errors.New("Traversal Effect 必须是 JSON 对象")
	}
	var op, key string
	if err := decodeRequired(object, "op", &op); err != nil {
		return nil, err
	}
	if err := decodeRequired(object, "key", &key); err != nil || key == "" {
		return nil, errors.New("Traversal Effect key 不能为空")
	}
	for field := range object {
		if field != "op" && field != "key" && field != "value" {
			return nil, fmt.Errorf("Traversal Effect 不允许字段 %q", field)
		}
	}
	switch op {
	case "set_world_state":
		var value bool
		if err := decodeRequired(object, "value", &value); err != nil {
			return nil, err
		}
		return effect{op: op, key: key, boolValue: value}, nil
	case "increment_quest_objective":
		var value int32
		if err := decodeRequired(object, "value", &value); err != nil || value < 1 {
			return nil, errors.New("increment_quest_objective.value 必须大于零")
		}
		return effect{op: op, key: key, value: value}, nil
	default:
		return nil, fmt.Errorf("不支持的 Traversal Effect 操作: %s", op)
	}
}
