// Package systemapi 实现与具体业务领域无关的版本信息查询契约。
package systemapi

import (
	"context"

	systemv1 "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1"
)

// BuildInfo 保存由构建系统注入并允许通过公开 API 返回的非敏感版本信息。
type BuildInfo struct {
	// Version 是应用语义版本；本地未发布构建使用 dev。
	Version string
	// Commit 是生成当前二进制的 Git 提交标识。
	Commit string
	// APIMajorVersion 是客户端用于判断 Protobuf 契约族的主版本。
	APIMajorVersion string
}

// Service 实现 SystemService 的无状态查询能力。
type Service struct {
	// build 是进程启动后保持不可变的构建元数据快照。
	build BuildInfo
}

// NewService 使用不可变构建元数据创建系统查询服务。
func NewService(build BuildInfo) *Service {
	return &Service{build: build}
}

// GetVersion 返回当前二进制及其 Protobuf 契约版本，不读取运行期敏感配置。
func (s *Service) GetVersion(
	_ context.Context,
	_ *systemv1.GetVersionRequest,
) (*systemv1.GetVersionResponse, error) {
	return &systemv1.GetVersionResponse{
		Version:         s.build.Version,
		Commit:          s.build.Commit,
		ApiMajorVersion: s.build.APIMajorVersion,
	}, nil
}
