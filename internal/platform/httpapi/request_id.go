package httpapi

import (
	"context"
	"net/http"
	"regexp"

	"github.com/go-kratos/kratos/v3/transport"
)

const requestIDHeader = "X-Request-ID"

var validRequestID = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$`)

type requestIDContextKey struct{}

// RequestIDMiddleware 校验客户端请求标识，并为缺失或非法值生成替代标识。
func RequestIDMiddleware(generate func() string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			requestID := EnsureRequestID(r.Header, generate)
			// Kratos 会把 HTTP Header 复制到自己的传输上下文；同步回请求头，确保生成的
			// 标识在 RPC Handler 中仍可读取，而不仅存在于原始 net/http Context。
			w.Header().Set(requestIDHeader, requestID)
			ctx := context.WithValue(r.Context(), requestIDContextKey{}, requestID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

// EnsureRequestID 校验请求头中的请求标识，并在缺失或非法时生成并写回替代值。
func EnsureRequestID(header http.Header, generate func() string) string {
	requestID := header.Get(requestIDHeader)
	if !validRequestID.MatchString(requestID) {
		requestID = generate()
	}
	header.Set(requestIDHeader, requestID)
	return requestID
}

// RequestIDFromContext 返回入口中间件确认后的请求标识。
func RequestIDFromContext(ctx context.Context) string {
	requestID, _ := ctx.Value(requestIDContextKey{}).(string)
	if validRequestID.MatchString(requestID) {
		return requestID
	}
	// Kratos RPC Handler 使用传输上下文；原始 net/http 自定义值在协议适配时不保证保留。
	if transporter, ok := transport.FromServerContext(ctx); ok {
		requestID = transporter.RequestHeader().Get(requestIDHeader)
		if validRequestID.MatchString(requestID) {
			return requestID
		}
	}
	return ""
}
