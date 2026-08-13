package audit

import (
	"crypto/sha256"
	"errors"
	"testing"
)

// TestVerifyRecordsAcceptsIndependentLedgers 验证两个审计账本各自从空摘要开始、但都必须与持久化链尾一致。
func TestVerifyRecordsAcceptsIndependentLedgers(t *testing.T) {
	t.Parallel()
	adminFirst := chainedRecord("admin_audit_log", 1, nil, []byte("admin-first"))
	adminSecond := chainedRecord("admin_audit_log", 2, adminFirst.entryHash, []byte("admin-second"))
	playerFirst := chainedRecord("administration_audit_log", 3, nil, []byte("player-first"))
	report, err := verifyRecords([]hashRecord{adminFirst, adminSecond, playerFirst}, chainState{
		"admin_audit_log":          adminSecond.entryHash,
		"administration_audit_log": playerFirst.entryHash,
	})
	if err != nil {
		t.Fatalf("verifyRecords() error = %v", err)
	}
	if len(report.Ledgers) != 2 || report.Ledgers[0].Entries != 2 || report.Ledgers[1].Entries != 1 {
		t.Fatalf("verifyRecords() report = %+v", report)
	}
}

// TestVerifyRecordsRejectsChangedPayload 验证单条审计业务事实被直接篡改后，重算摘要会稳定失败。
func TestVerifyRecordsRejectsChangedPayload(t *testing.T) {
	t.Parallel()
	record := chainedRecord("admin_audit_log", 1, nil, []byte("original"))
	record.payload = []byte("tampered")
	_, err := verifyRecords([]hashRecord{record}, chainState{
		"admin_audit_log":          record.entryHash,
		"administration_audit_log": nil,
	})
	if !errors.Is(err, ErrHashChainInvalid) {
		t.Fatalf("verifyRecords() error = %v，期望 ErrHashChainInvalid", err)
	}
}

// chainedRecord 以与生产验证器相同的 SHA-256 拼接规则构造测试记录。
func chainedRecord(ledger string, sequence int64, previousHash []byte, payload []byte) hashRecord {
	digest := sha256.Sum256(append(append([]byte(nil), previousHash...), payload...))
	return hashRecord{
		ledger: ledger, sequence: sequence, previousHash: append([]byte(nil), previousHash...),
		entryHash: digest[:], payload: append([]byte(nil), payload...),
	}
}
