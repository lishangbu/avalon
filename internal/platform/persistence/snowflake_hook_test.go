package persistence

import (
	"context"
	"errors"
	"testing"

	"entgo.io/ent"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

func TestSnowflakeHookRejectsCreateWithoutExplicitIdentifier(t *testing.T) {
	t.Parallel()
	client := avalonent.NewClient()
	mutation := client.Account.Create().Mutation()
	called := false
	mutator := requireExplicitSnowflakeIdentifiers()(ent.MutateFunc(func(context.Context, ent.Mutation) (ent.Value, error) {
		called = true
		return nil, nil
	}))
	if _, err := mutator.Mutate(context.Background(), mutation); !errors.Is(err, errExplicitSnowflakeIdentifierRequired) {
		t.Fatalf("Mutate() error = %v", err)
	}
	if called {
		t.Fatal("缺少 Identifier 的创建仍进入了底层 Mutator")
	}
	mutation.SetID(snowflake.MustParse("1048577"))
	if _, err := mutator.Mutate(context.Background(), mutation); err != nil {
		t.Fatal(err)
	}
	if !called {
		t.Fatal("显式 Identifier 创建没有进入底层 Mutator")
	}
}
