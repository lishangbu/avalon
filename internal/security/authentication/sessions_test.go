package authentication_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/authentication"
)

func TestSessionManagerListsOnlyTheAuthenticatedAccountsFamilies(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576204")
	currentFamilyID := snowflake.MustParse("1048576205")
	otherFamilyID := snowflake.MustParse("1048576206")
	query := &sessionQueryStub{families: []authentication.SessionFamily{
		{FamilyID: currentFamilyID},
		{FamilyID: otherFamilyID},
	}}
	manager := authentication.NewSessionManager(query, &sessionTransactionsStub{}, snowflake.NewTestID, time.Now)

	families, err := manager.List(context.Background(), authentication.Principal{
		AccountID:       accountID,
		SessionFamilyID: currentFamilyID,
	})

	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if query.accountID != accountID {
		t.Fatalf("query account ID = %s", query.accountID)
	}
	if len(families) != 2 || !families[0].Current || families[1].Current {
		t.Fatalf("families = %+v", families)
	}
}

func TestSessionManagerRevokesAnOwnedFamilyAndWritesAuditInOneTransaction(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576204")
	familyID := snowflake.MustParse("1048576205")
	auditID := snowflake.MustParse("1048576206")
	now := time.Unix(1_800_000_000, 0).UTC()
	writer := &sessionWriterStub{revoked: true}
	transactions := &sessionTransactionsStub{writer: writer}
	manager := authentication.NewSessionManager(
		&sessionQueryStub{},
		transactions,
		snowflake.TestSource(func() snowflake.ID { return auditID }),
		func() time.Time { return now },
	)

	err := manager.Revoke(context.Background(), authentication.Principal{AccountID: accountID}, familyID, "revoke-session")

	if err != nil {
		t.Fatalf("Revoke() error = %v", err)
	}
	if transactions.calls != 1 || writer.accountID != accountID || writer.familyID != familyID {
		t.Fatalf("revocation = account %s family %s transactions %d", writer.accountID, writer.familyID, transactions.calls)
	}
	if writer.audit.ID != auditID || writer.audit.RequestID != "revoke-session" || writer.audit.OccurredAt != now {
		t.Fatalf("audit = %+v", writer.audit)
	}
}

type sessionQueryStub struct {
	accountID snowflake.ID
	families  []authentication.SessionFamily
}

func (s *sessionQueryStub) ListActiveSessionFamilies(
	_ context.Context,
	accountID snowflake.ID,
	_ time.Time,
) ([]authentication.SessionFamily, error) {
	s.accountID = accountID
	return s.families, nil
}

type sessionTransactionsStub struct {
	writer *sessionWriterStub
	calls  int
}

func (s *sessionTransactionsStub) WithinSessionRevocation(
	ctx context.Context,
	work func(authentication.SessionRevocationWriter) error,
) error {
	s.calls++
	if s.writer == nil {
		s.writer = &sessionWriterStub{}
	}
	return work(s.writer)
}

type sessionWriterStub struct {
	accountID snowflake.ID
	familyID  snowflake.ID
	revoked   bool
	audit     authentication.SessionRevocationAudit
}

func (s *sessionWriterStub) RevokeOwnedSessionFamily(
	_ context.Context,
	accountID snowflake.ID,
	familyID snowflake.ID,
	_ time.Time,
) (bool, error) {
	s.accountID = accountID
	s.familyID = familyID
	return s.revoked, nil
}

func (s *sessionWriterStub) RecordSessionRevocation(
	_ context.Context,
	audit authentication.SessionRevocationAudit,
) error {
	s.audit = audit
	return nil
}
