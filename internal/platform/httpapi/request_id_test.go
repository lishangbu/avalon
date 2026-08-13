package httpapi_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-kratos/kratos/v3/transport"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
)

func TestRequestIDMiddlewareRejectsUnsafeClientValues(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name     string
		provided string
		want     string
	}{
		{name: "keeps valid value", provided: "admin-ui_01.request", want: "admin-ui_01.request"},
		{name: "generates missing value", want: "019c-generated"},
		{name: "rejects log injection", provided: "request\r\nforged:true", want: "019c-generated"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			var contextValue string
			var requestHeaderValue string
			handler := httpapi.RequestIDMiddleware(func() string { return "019c-generated" })(http.HandlerFunc(
				func(w http.ResponseWriter, r *http.Request) {
					contextValue = httpapi.RequestIDFromContext(r.Context())
					requestHeaderValue = r.Header.Get("X-Request-ID")
					w.WriteHeader(http.StatusNoContent)
				},
			))
			recorder := httptest.NewRecorder()
			request := httptest.NewRequest(http.MethodGet, "/", nil)
			if tt.provided != "" {
				request.Header.Set("X-Request-ID", tt.provided)
			}

			handler.ServeHTTP(recorder, request)

			if contextValue != tt.want {
				t.Errorf("context request ID = %q, want %q", contextValue, tt.want)
			}
			if header := recorder.Header().Get("X-Request-ID"); header != tt.want {
				t.Errorf("response request ID = %q, want %q", header, tt.want)
			}
			if requestHeaderValue != tt.want {
				t.Errorf("forwarded request ID = %q, want %q", requestHeaderValue, tt.want)
			}
		})
	}
}

// TestRequestIDFromKratosTransport 验证 RPC Handler 可以从 Kratos 传输上下文恢复入口请求标识。
func TestRequestIDFromKratosTransport(t *testing.T) {
	t.Parallel()
	header := testHeader{"X-Request-ID": {"019c-kratos-request"}}
	ctx := transport.NewServerContext(context.Background(), testTransport{request: header})
	if requestID := httpapi.RequestIDFromContext(ctx); requestID != "019c-kratos-request" {
		t.Fatalf("Kratos transport request ID = %q", requestID)
	}
}

// testHeader 是 Request ID Kratos 传输回归测试使用的最小 Header 实现。
type testHeader map[string][]string

func (header testHeader) Get(key string) string {
	values := header[key]
	if len(values) == 0 {
		return ""
	}
	return values[0]
}
func (header testHeader) Set(key, value string) { header[key] = []string{value} }
func (header testHeader) Add(key, value string) { header[key] = append(header[key], value) }
func (header testHeader) Keys() []string {
	keys := make([]string, 0, len(header))
	for key := range header {
		keys = append(keys, key)
	}
	return keys
}
func (header testHeader) Values(key string) []string { return header[key] }

// testTransport 是 Request ID 测试使用的最小 Kratos 服务端传输实现。
type testTransport struct {
	// request 保存模拟 RPC 请求头。
	request transport.Header
}

func (testTransport) Kind() transport.Kind                  { return transport.KindHTTP }
func (testTransport) Endpoint() string                      { return "http://127.0.0.1" }
func (testTransport) Operation() string                     { return "/test.Request/Call" }
func (value testTransport) RequestHeader() transport.Header { return value.request }
func (testTransport) ReplyHeader() transport.Header         { return testHeader{} }
