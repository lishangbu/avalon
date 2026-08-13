// Package access 编译 RPC 的公开性策略，不包含角色或权限判断。
package access

import (
	"fmt"

	securityv1 "github.com/lishangbu/avalon/api/gen/go/avalon/security/v1"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/reflect/protoreflect"
	"google.golang.org/protobuf/types/descriptorpb"
)

// OperationPolicy 是从 Protobuf 方法注解编译得到的运行期安全策略。
type OperationPolicy struct {
	// Public 表示方法允许匿名调用。
	Public bool
}

// OperationCatalog 保存以 Kratos 完整 operation 名称索引的不可变方法策略。
type OperationCatalog map[string]OperationPolicy

// NewOperationCatalog 从生成的 Protobuf 描述符建立唯一安全目录。
//
// 每个 RPC 都必须显式声明 access 注解；缺失注解会使 Server 启动失败，避免新接口
// 在未评估安全边界时被默认放行。进程注册集合决定玩家或管理员能力。
func NewOperationCatalog(files ...protoreflect.FileDescriptor) (OperationCatalog, error) {
	result := make(OperationCatalog)
	for _, file := range files {
		services := file.Services()
		for serviceIndex := 0; serviceIndex < services.Len(); serviceIndex++ {
			service := services.Get(serviceIndex)
			methods := service.Methods()
			for methodIndex := 0; methodIndex < methods.Len(); methodIndex++ {
				method := methods.Get(methodIndex)
				options, ok := method.Options().(*descriptorpb.MethodOptions)
				if !ok || !proto.HasExtension(options, securityv1.E_Access) {
					return nil, fmt.Errorf("RPC %s.%s 缺少 access 安全注解", service.FullName(), method.Name())
				}
				extension := proto.GetExtension(options, securityv1.E_Access)
				policy, ok := extension.(*securityv1.OperationAccess)
				if !ok || policy == nil {
					return nil, fmt.Errorf("RPC %s.%s 的 access 安全注解无效", service.FullName(), method.Name())
				}
				operation := "/" + string(service.FullName()) + "/" + string(method.Name())
				result[operation] = OperationPolicy{Public: policy.GetPublic()}
			}
		}
	}
	return result, nil
}
