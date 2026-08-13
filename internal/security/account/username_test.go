package account_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/security/account"
)

func TestParseUsernameBuildsOneCanonicalLoginIdentity(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		input   string
		want    string
		wantErr bool
	}{
		{name: "normalizes ASCII case", input: "Avalon.Admin", want: "avalon.admin"},
		{name: "accepts explicit separators", input: "avalon-admin_01", want: "avalon-admin_01"},
		{name: "rejects Unicode lookalike", input: "avalon-管理员", wantErr: true},
		{name: "rejects surrounding whitespace", input: " avalon", wantErr: true},
		{name: "rejects consecutive separators", input: "avalon..admin", wantErr: true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			username, err := account.ParseUsername(tt.input)
			if tt.wantErr {
				if err == nil {
					t.Fatalf("ParseUsername(%q) error = nil", tt.input)
				}
				return
			}
			if err != nil {
				t.Fatalf("ParseUsername(%q) error = %v", tt.input, err)
			}
			if username.String() != tt.want {
				t.Errorf("username = %q, want %q", username.String(), tt.want)
			}
		})
	}
}
