package authentication

import "context"

type principalContextKey struct{}

// WithPrincipal 把已经认证的权威账号身份写入请求 Context，供受保护业务 Handler 使用。
func WithPrincipal(ctx context.Context, principal Principal) context.Context {
	return context.WithValue(ctx, principalContextKey{}, principal)
}

// PrincipalFromContext 读取认证中间件写入的权威账号身份。
func PrincipalFromContext(ctx context.Context) (Principal, bool) {
	principal, ok := ctx.Value(principalContextKey{}).(Principal)
	return principal, ok
}
