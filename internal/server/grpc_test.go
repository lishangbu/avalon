package server

import (
	"context"
	"errors"
	"testing"
)

type testTransport struct {
	start func(context.Context) error
}

func (transport *testTransport) Start(ctx context.Context) error { return transport.start(ctx) }
func (transport *testTransport) Stop(context.Context) error      { return nil }

func TestStartTransportsStartsConnectAlongsideGRPC(t *testing.T) {
	t.Parallel()

	grpcStarted := make(chan struct{})
	connectFailure := errors.New("connect listen failed")
	grpc := &testTransport{start: func(ctx context.Context) error {
		close(grpcStarted)
		<-ctx.Done()
		return nil
	}}
	connect := &testTransport{start: func(context.Context) error {
		<-grpcStarted
		return connectFailure
	}}

	err := startTransports(context.Background(), grpc, connect)
	if !errors.Is(err, connectFailure) {
		t.Fatalf("startTransports() error = %v, want %v", err, connectFailure)
	}
}
