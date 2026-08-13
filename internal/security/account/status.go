package account

// Status 是认证、会话失效和系统授权共同使用的稳定账号状态。
type Status string

const (
	// StatusActive 表示账号可以登录并承担系统授权职责。
	StatusActive Status = "active"
	// StatusLocked 表示账号因连续认证失败处于有时限锁定中。
	StatusLocked Status = "locked"
	// StatusDisabled 表示账号已被管理员禁用。
	StatusDisabled Status = "disabled"
)
