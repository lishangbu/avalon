package asset

import (
	"bytes"
	"context"
	"errors"
	"image"
	"io"
	"testing"
	"time"
)

func TestFullImageDecodeReturnsAtDeadlineAndKeepsRunawayWorkBounded(t *testing.T) {
	t.Parallel()
	started := make(chan struct{})
	release := make(chan struct{})
	service := &Service{
		decodeSlots: make(chan struct{}, 1),
		decode: func(io.Reader) (image.Image, string, error) {
			close(started)
			<-release
			return image.NewNRGBA(image.Rect(0, 0, 1, 1)), "png", nil
		},
	}
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Millisecond)
	defer cancel()
	before := time.Now()
	_, _, err := service.decodeFullImage(ctx, []byte("buffered image payload"))
	if !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("decodeFullImage() error = %v, want DeadlineExceeded", err)
	}
	if elapsed := time.Since(before); elapsed > 500*time.Millisecond {
		t.Fatalf("decodeFullImage() returned after %s", elapsed)
	}
	<-started
	if len(service.decodeSlots) != 1 {
		t.Fatalf("decode slots in use = %d, want 1", len(service.decodeSlots))
	}
	secondCtx, secondCancel := context.WithTimeout(context.Background(), 20*time.Millisecond)
	defer secondCancel()
	_, _, secondErr := service.decodeFullImage(secondCtx, bytes.Repeat([]byte{0x01}, 16))
	if !errors.Is(secondErr, context.DeadlineExceeded) {
		t.Fatalf("second decodeFullImage() error = %v, want DeadlineExceeded", secondErr)
	}
	close(release)
	deadline := time.Now().Add(time.Second)
	for len(service.decodeSlots) != 0 && time.Now().Before(deadline) {
		time.Sleep(time.Millisecond)
	}
	if len(service.decodeSlots) != 0 {
		t.Fatal("decode slot was not released after decoder exited")
	}
}
