// Package stablecode 集中定义游戏资料 Stable Code 的边界规范。
package stablecode

import "regexp"

var pattern = regexp.MustCompile(`^[a-z][a-z0-9-]{1,63}$`)

// Valid 判断字符串是否为可持久化的英文机器稳定编码。
func Valid(value string) bool {
	return pattern.MatchString(value)
}
