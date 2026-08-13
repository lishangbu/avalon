// Package runtime 组合 Kratos V3 应用生命周期和进程级资源边界。
package runtime

import (
	"log/slog"
	"time"

	"github.com/go-kratos/kratos/v3"
	"github.com/go-kratos/kratos/v3/transport"
)

// ApplicationInfo 描述写入 Kratos App、日志与遥测资源的稳定进程身份。
type ApplicationInfo struct {
	// ID 是当前进程实例标识；默认由组合根使用主机名提供。
	ID string
	// Name 是稳定的可执行程序名称，例如 avalon-server。
	Name string
	// Version 是发布构建通过 ldflags 注入的版本，开发构建固定为 dev。
	Version string
	// Metadata 保存不含秘密、可安全用于注册信息和诊断的静态键值。
	Metadata map[string]string
	// StopTimeout 限制所有 Server 与 Worker 完成优雅关闭的最长时间。
	StopTimeout time.Duration
}

// NewApplication 创建统一启动、等待和优雅停止全部进程组件的 Kratos V3 应用。
//
// 调用方继续显式构造依赖图；这里不使用 Wire 或运行时容器。HTTP Server 与 Asynq Worker
// 都通过 Kratos transport.Server 的 Start/Stop 契约参与同一关闭顺序。
func NewApplication(
	info ApplicationInfo,
	logger *slog.Logger,
	servers ...transport.Server,
) *kratos.App {
	return kratos.New(
		kratos.ID(info.ID),
		kratos.Name(info.Name),
		kratos.Version(info.Version),
		kratos.Metadata(info.Metadata),
		kratos.Logger(logger),
		kratos.StopTimeout(info.StopTimeout),
		kratos.Server(servers...),
	)
}
