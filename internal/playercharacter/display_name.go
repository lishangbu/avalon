// Package playercharacter 定义账号拥有的持久游戏角色及其生命周期边界。
package playercharacter

import (
	"errors"
	"strings"
	"unicode"
	"unicode/utf8"

	"golang.org/x/text/unicode/norm"
)

const (
	displayNameMinLength = 2
	displayNameMaxLength = 16
)

// ErrInvalidDisplayName 表示 PlayerCharacter 展示名称未满足长度或字符边界。
var ErrInvalidDisplayName = errors.New("PlayerCharacter 展示名称格式无效")

// DisplayName 保存公开展示值、全局唯一键和敏感词匹配键。
type DisplayName struct {
	value         string
	key           string
	moderationKey string
}

// ParseDisplayName 使用 NFKC 统一宽度并建立大小写不敏感的稳定名称键。
func ParseDisplayName(input string) (DisplayName, error) {
	value := norm.NFKC.String(strings.TrimSpace(input))
	key := strings.ToLower(value)
	length := utf8.RuneCountInString(key)
	if length < displayNameMinLength || length > displayNameMaxLength {
		return DisplayName{}, ErrInvalidDisplayName
	}
	for _, character := range key {
		if !unicode.IsLetter(character) && !unicode.IsNumber(character) &&
			character != ' ' && character != '_' && character != '-' {
			return DisplayName{}, ErrInvalidDisplayName
		}
	}
	return DisplayName{
		value:         value,
		key:           key,
		moderationKey: strings.Map(removeNameSeparator, key),
	}, nil
}

// String 返回规范化后仍保留用户选择大小写的公开展示名称。
func (d DisplayName) String() string {
	return d.value
}

// Key 返回用于当前名称与历史名称全局判重的大小写不敏感键。
func (d DisplayName) Key() string {
	return d.key
}

// ModerationKey 返回移除允许分隔符后的敏感词匹配键。
func (d DisplayName) ModerationKey() string {
	return d.moderationKey
}

func removeNameSeparator(character rune) rune {
	if character == ' ' || character == '_' || character == '-' {
		return -1
	}
	return character
}
