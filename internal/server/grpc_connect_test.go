package server

import (
	"context"
	"net/http"
	"testing"

	"connectrpc.com/connect"
	"github.com/go-kratos/kratos/v3/transport"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
)

func TestNewConnectRequestTransportCarriesProcedureAndHeaders(t *testing.T) {
	requestHeaders := http.Header{"Authorization": {"Bearer test-token"}}
	value := newConnectRequestTransport("/avalon.admin.v1.AdminSecurityService/Login", requestHeaders)
	ctx := transport.NewServerContext(t.Context(), value)

	got, ok := transport.FromServerContext(ctx)
	if !ok {
		t.Fatal("transport context missing")
	}
	if got.Operation() != "/avalon.admin.v1.AdminSecurityService/Login" {
		t.Fatalf("operation = %q", got.Operation())
	}
	if got.RequestHeader().Get("Authorization") != "Bearer test-token" {
		t.Fatalf("authorization header = %q", got.RequestHeader().Get("Authorization"))
	}
}

func TestConnectInterceptorGeneratesRequestIDForGameDataWrites(t *testing.T) {
	interceptor := &kratosConnectInterceptor{}
	request := connect.NewRequest(&struct{}{})
	request.Header().Set("Authorization", "Bearer test-token")

	var handlerRequestID string
	response, err := interceptor.WrapUnary(func(ctx context.Context, _ connect.AnyRequest) (connect.AnyResponse, error) {
		handlerRequestID = httpapi.RequestIDFromContext(ctx)
		return connect.NewResponse(&struct{}{}), nil
	})(t.Context(), request)
	if err != nil {
		t.Fatalf("WrapUnary() error = %v", err)
	}
	if handlerRequestID == "" {
		t.Fatal("handler request ID is empty")
	}
	if got := response.Header().Get("X-Request-ID"); got != handlerRequestID {
		t.Fatalf("response request ID = %q, want %q", got, handlerRequestID)
	}
}
