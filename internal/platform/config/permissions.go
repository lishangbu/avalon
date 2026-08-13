package config

import (
	"fmt"
	"os"
	"runtime"
)

// PermissionWarning 在支持 POSIX 权限位的平台提醒部署者收紧含敏感值的配置文件。
func PermissionWarning(path string) string {
	if runtime.GOOS == "windows" {
		return ""
	}
	info, err := os.Stat(path)
	if err != nil || info.Mode().Perm()&0o077 == 0 {
		return ""
	}
	return fmt.Sprintf("配置文件 %q 可被属主之外的用户读取，请检查部署文件权限", path)
}
