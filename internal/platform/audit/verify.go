// Package audit 提供应用事务显式追加、由受控后台任务独立验证的审计哈希链能力。
package audit

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
	"github.com/lishangbu/avalon/internal/platform/database"
)

var (
	// ErrHashChainInvalid 表示审计记录顺序、前置哈希、记录摘要或链尾状态至少一项不一致。
	ErrHashChainInvalid = errors.New("审计哈希链校验失败")
)

// LedgerReport 是一条独立审计账本的可安全输出验证摘要。
type LedgerReport struct {
	// Ledger 是数据库中存放该类审计事实的稳定表名。
	Ledger string `json:"ledger"`
	// Entries 是已参与哈希验证的审计记录总数。
	Entries int64 `json:"entries"`
	// LastSequence 是该账本中最后一条记录的自增顺序号；空账本为零。
	LastSequence int64 `json:"lastSequence"`
	// LatestHash 是数据库链尾的十六进制 SHA-256 摘要；它不包含审计业务载荷。
	LatestHash string `json:"latestHash"`
}

// VerificationReport 是一次完整审计哈希链验证的最小结果。
type VerificationReport struct {
	// Ledgers 是每个独立审计表的验证摘要，按稳定账本名排序。
	Ledgers []LedgerReport `json:"ledgers"`
}

// Verifier 把 PostgreSQL 连接池适配为后台校验应用服务需要的只读接口。
type Verifier struct {
	// pool 提供审计账本与链尾状态的一致数据库视图。
	pool *database.Pool
}

// NewVerifier 使用显式数据库连接池创建审计哈希链验证器。
func NewVerifier(pool *database.Pool) *Verifier {
	return &Verifier{pool: pool}
}

// Verify 独立重算全部支持的审计哈希链，不修改任何业务或审计记录。
func (verifier *Verifier) Verify(ctx context.Context) (VerificationReport, error) {
	if verifier == nil {
		return VerificationReport{}, ErrHashChainInvalid
	}
	return Verify(ctx, verifier.pool)
}

// Verify 从 PostgreSQL 读取两类审计账本，并按应用写入时的规范载荷重算每个链式摘要。
//
// 本函数不修改数据库，也不会把审计的 changes 字段打印或返回给调用者。正常业务进程只依赖数据库
// 追加器写入哈希；离线验证器从持久字段独立重建载荷，防止校验依赖缓存结果。
func Verify(ctx context.Context, pool *database.Pool) (VerificationReport, error) {
	if pool == nil {
		return VerificationReport{}, ErrHashChainInvalid
	}
	rows, err := pool.Query(ctx, `
SELECT ledger, id, sequence, actor_account_id, actor_kind, actor_identifier,
       action_code, object_type, object_id, request_id, reason, changes, created_at,
       previous_hash, entry_hash
FROM (
    SELECT
        'admin_audit_log'::text AS ledger,
        id, sequence, actor_account_id, actor_kind, actor_identifier, action_code,
        object_type, object_id, request_id, reason, changes, created_at, previous_hash, entry_hash
    FROM public.admin_audit_log
    UNION ALL
    SELECT
        'administration_audit_log'::text AS ledger,
        id, sequence, actor_account_id, actor_kind, actor_identifier, action_code,
        object_type, object_id, request_id, reason, changes, created_at, previous_hash, entry_hash
    FROM public.administration_audit_log
) AS entries
ORDER BY ledger, sequence`)
	if err != nil {
		return VerificationReport{}, fmt.Errorf("读取审计哈希记录: %w", err)
	}
	defer rows.Close()
	records := make([]hashRecord, 0)
	for rows.Next() {
		var record hashRecord
		var entry Entry
		var actorAccountID pgtype.Int8
		var actorIdentifier pgtype.Text
		var objectID pgtype.Text
		var reason pgtype.Text
		var changes []byte
		if err := rows.Scan(
			&record.ledger, &entry.ID, &record.sequence, &actorAccountID, &entry.ActorKind,
			&actorIdentifier, &entry.ActionCode, &entry.ObjectType, &objectID, &entry.RequestID,
			&reason, &changes, &entry.CreatedAt, &record.previousHash, &record.entryHash,
		); err != nil {
			return VerificationReport{}, fmt.Errorf("解码审计哈希记录: %w", err)
		}
		entry.ActorAccountID = pgIDPointer(actorAccountID)
		entry.ActorIdentifier = pgTextPointer(actorIdentifier)
		entry.ObjectID = pgTextPointer(objectID)
		entry.Reason = pgTextPointer(reason)
		entry.CreatedAt = entry.CreatedAt.UTC().Truncate(time.Microsecond)
		normalizedChanges, err := normalizeJSON(changes)
		if err != nil {
			return VerificationReport{}, fmt.Errorf("规范化审计哈希记录: %w", err)
		}
		entry.Changes = normalizedChanges
		record.payload, err = canonicalPayload(record.ledger, record.sequence, entry)
		if err != nil {
			return VerificationReport{}, fmt.Errorf("重建审计哈希载荷: %w", err)
		}
		records = append(records, record)
	}
	if err := rows.Err(); err != nil {
		return VerificationReport{}, fmt.Errorf("遍历审计哈希记录: %w", err)
	}
	chainState, err := loadChainState(ctx, pool)
	if err != nil {
		return VerificationReport{}, err
	}
	return verifyRecords(records, chainState)
}

func pgTextPointer(value pgtype.Text) *string {
	if !value.Valid {
		return nil
	}
	text := value.String
	return &text
}

// hashRecord 是验证阶段使用的数据库规范化审计摘要行，不向外暴露业务 changes 载荷。
type hashRecord struct {
	// ledger 是对应审计表的稳定名称。
	ledger string
	// sequence 是该表的单调递增审计顺序。
	sequence int64
	// previousHash 是写入时冻结的前一条链式摘要。
	previousHash []byte
	// entryHash 是应用事务追加器写入的当前记录摘要。
	entryHash []byte
	// payload 是验证器从持久字段独立重建的规范化字节序列。
	payload []byte
}

// chainState 是每个账本数据库持久化的链尾摘要。
type chainState map[string][]byte

func loadChainState(ctx context.Context, pool *database.Pool) (chainState, error) {
	rows, err := pool.Query(ctx, `SELECT ledger, latest_hash FROM public.audit_hash_chain_state ORDER BY ledger`)
	if err != nil {
		return nil, fmt.Errorf("读取审计哈希链尾状态: %w", err)
	}
	defer rows.Close()
	result := chainState{}
	for rows.Next() {
		var ledger string
		var latestHash []byte
		if err := rows.Scan(&ledger, &latestHash); err != nil {
			return nil, fmt.Errorf("解码审计哈希链尾状态: %w", err)
		}
		result[ledger] = latestHash
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("遍历审计哈希链尾状态: %w", err)
	}
	return result, nil
}

func verifyRecords(records []hashRecord, state chainState) (VerificationReport, error) {
	ledgers := []string{"admin_audit_log", "administration_audit_log"}
	lastSequence := make(map[string]int64, len(ledgers))
	lastHash := make(map[string][]byte, len(ledgers))
	counts := make(map[string]int64, len(ledgers))
	for _, ledger := range ledgers {
		if _, found := state[ledger]; !found {
			return VerificationReport{}, fmt.Errorf("%w: 缺少账本 %s 的链尾状态", ErrHashChainInvalid, ledger)
		}
		lastHash[ledger] = []byte{}
	}
	for _, record := range records {
		if _, expectedLedger := lastHash[record.ledger]; !expectedLedger {
			return VerificationReport{}, fmt.Errorf("%w: 未知账本 %s", ErrHashChainInvalid, record.ledger)
		}
		if record.sequence <= lastSequence[record.ledger] {
			return VerificationReport{}, fmt.Errorf("%w: %s 顺序号 %d 非递增", ErrHashChainInvalid, record.ledger, record.sequence)
		}
		if !bytes.Equal(record.previousHash, lastHash[record.ledger]) {
			return VerificationReport{}, fmt.Errorf("%w: %s 顺序号 %d 的前置摘要不匹配", ErrHashChainInvalid, record.ledger, record.sequence)
		}
		digest := sha256.Sum256(append(append([]byte(nil), record.previousHash...), record.payload...))
		if !bytes.Equal(record.entryHash, digest[:]) {
			return VerificationReport{}, fmt.Errorf("%w: %s 顺序号 %d 的记录摘要不匹配", ErrHashChainInvalid, record.ledger, record.sequence)
		}
		lastSequence[record.ledger] = record.sequence
		lastHash[record.ledger] = append([]byte(nil), record.entryHash...)
		counts[record.ledger]++
	}
	report := VerificationReport{Ledgers: make([]LedgerReport, 0, len(ledgers))}
	for _, ledger := range ledgers {
		if !bytes.Equal(lastHash[ledger], state[ledger]) {
			return VerificationReport{}, fmt.Errorf("%w: %s 的链尾状态不匹配", ErrHashChainInvalid, ledger)
		}
		report.Ledgers = append(report.Ledgers, LedgerReport{
			Ledger: ledger, Entries: counts[ledger], LastSequence: lastSequence[ledger], LatestHash: hex.EncodeToString(lastHash[ledger]),
		})
	}
	return report, nil
}
