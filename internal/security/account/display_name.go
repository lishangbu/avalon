package account

import (
	"errors"
	"strings"
	"unicode"
	"unicode/utf8"
)

// ErrInvalidDisplayName 表示账号展示名称为空、过长或包含控制字符。
var ErrInvalidDisplayName = errors.New("账号展示名称格式无效")

// DisplayName 是与登录用户名分离的账号展示名称。
type DisplayName struct {
	value string
}

// ParseDisplayName 清理首尾空白并校验面向用户的账号展示名称。
func ParseDisplayName(input string) (DisplayName, error) {
	value := strings.TrimSpace(input)
	if value == "" || utf8.RuneCountInString(value) > 64 {
		return DisplayName{}, ErrInvalidDisplayName
	}
	for _, character := range value {
		if unicode.IsControl(character) {
			return DisplayName{}, ErrInvalidDisplayName
		}
	}
	return DisplayName{value: value}, nil
}

// String 返回已校验的账号展示名称。
func (d DisplayName) String() string {
	return d.value
}
