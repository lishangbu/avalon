// Package snowflake 提供 Avalon 全局稳定雪花标识及其唯一运行时发号实现。
package snowflake

import (
	"bytes"
	"context"
	"database/sql/driver"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"sync/atomic"
)

var (
	// ErrInvalidID 表示输入不是正数十进制雪花标识。
	ErrInvalidID = errors.New("雪花 ID 格式无效")
)

// ID 是数据库 BIGINT、Go 领域对象和字符串契约之间共享的正数雪花标识。
//
// ID 只承担稳定身份，不允许调用方对它执行领域算术或推断业务时间。
type ID int64

var testCounter atomic.Int64

// Source 是应用生成 Snowflake Identifier 所依赖的最小接口。
//
// 实现必须传播 Context 取消和租约失效，不得以本地随机值或数据库默认值降级。
type Source interface {
	Next(context.Context) (ID, error)
}

// TestSource 是无需 PostgreSQL 租约的测试发号函数类型，仅供测试夹具装配。
type TestSource func() ID

// Next 让 TestSource 实现 Source，并在生成前尊重 Context 取消。
func (source TestSource) Next(ctx context.Context) (ID, error) {
	if err := ctx.Err(); err != nil {
		return 0, err
	}
	if source == nil {
		return 0, ErrLeaseInvalid
	}
	return source(), nil
}

// Parse 将不带符号和前导零的十进制文本解析为正数 ID。
func Parse(raw string) (ID, error) {
	if raw == "" || raw[0] == '0' || raw[0] == '+' || raw[0] == '-' {
		return 0, ErrInvalidID
	}
	for _, character := range raw {
		if character < '0' || character > '9' {
			return 0, ErrInvalidID
		}
	}
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil || value <= 0 {
		return 0, ErrInvalidID
	}
	return ID(value), nil
}

// MustParse 解析固定资料或测试中的十进制 ID，失败时 panic。
func MustParse(raw string) ID {
	id, err := Parse(raw)
	if err != nil {
		panic(err)
	}
	return id
}

// NewTestID 仅为不装配 PostgreSQL 租约的测试生成稳定形状 ID；生产装配不得调用。
var NewTestID TestSource = func() ID {
	value := testCounter.Add(1)
	millis := value >> sequenceBits
	sequence := value & int64(maximumSequence)
	return ID(((millis + 1) << timestampShift) | (254 << nodeShift) | sequence)
}

// String 返回供 Protobuf 和日志边界使用的十进制形式。
func (id ID) String() string { return strconv.FormatInt(int64(id), 10) }

// MarshalText 支持结构化配置和 map key 使用十进制字符串。
func (id ID) MarshalText() ([]byte, error) {
	if !id.IsValid() {
		return nil, ErrInvalidID
	}
	return []byte(id.String()), nil
}

// UnmarshalText 从十进制字符串读取 ID。
func (id *ID) UnmarshalText(raw []byte) error {
	value, err := Parse(string(raw))
	if err != nil {
		return err
	}
	*id = value
	return nil
}

// MarshalJSON 始终把 ID 编码为字符串，避免 JavaScript 数值精度损失。
func (id ID) MarshalJSON() ([]byte, error) {
	if !id.IsValid() {
		return nil, ErrInvalidID
	}
	return json.Marshal(id.String())
}

// UnmarshalJSON 只接受规范十进制 JSON 字符串，不接受数值或前导零。
func (id *ID) UnmarshalJSON(raw []byte) error {
	if id == nil || len(raw) < 3 || raw[0] != '"' || raw[len(raw)-1] != '"' || bytes.Contains(raw[1:len(raw)-1], []byte{'"'}) {
		return ErrInvalidID
	}
	var text string
	if err := json.Unmarshal(raw, &text); err != nil {
		return ErrInvalidID
	}
	return id.UnmarshalText([]byte(text))
}

// IsValid 报告 ID 是否处于允许的正数范围。
func (id ID) IsValid() bool { return id > 0 }

// Value 把 ID 编码为 PostgreSQL BIGINT。
func (id ID) Value() (driver.Value, error) {
	if !id.IsValid() {
		return nil, ErrInvalidID
	}
	return int64(id), nil
}

// Scan 从 PostgreSQL BIGINT 扫描 ID。
func (id *ID) Scan(source any) error {
	if id == nil {
		return errors.New("雪花 ID 扫描目标不能为空")
	}
	var value int64
	switch source := source.(type) {
	case int64:
		value = source
	case []byte:
		parsed, err := strconv.ParseInt(string(source), 10, 64)
		if err != nil {
			return fmt.Errorf("扫描雪花 ID: %w", err)
		}
		value = parsed
	case string:
		parsed, err := strconv.ParseInt(source, 10, 64)
		if err != nil {
			return fmt.Errorf("扫描雪花 ID: %w", err)
		}
		value = parsed
	default:
		return fmt.Errorf("雪花 ID 不支持扫描 %T", source)
	}
	if value <= 0 {
		return ErrInvalidID
	}
	*id = ID(value)
	return nil
}
