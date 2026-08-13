// Package account 定义认证账号的领域值与安全规则。
package account

import (
	"errors"
	"regexp"
	"strings"
)

// ErrInvalidUsername 表示登录用户名不满足规范化 ASCII 规则。
var ErrInvalidUsername = errors.New("登录用户名格式无效")

var validUsername = regexp.MustCompile(`^[a-z0-9]+(?:[._-][a-z0-9]+)*$`)

// Username 是规范化后按不区分大小写唯一的登录标识。
type Username struct {
	value string
}

// ParseUsername 校验用户名并建立唯一的小写 ASCII 表达。
func ParseUsername(input string) (Username, error) {
	canonical := strings.ToLower(input)
	if len(canonical) < 3 || len(canonical) > 32 || !validUsername.MatchString(canonical) {
		return Username{}, ErrInvalidUsername
	}
	return Username{value: canonical}, nil
}

// String 返回可持久化和比较的规范用户名。
func (u Username) String() string {
	return u.value
}
