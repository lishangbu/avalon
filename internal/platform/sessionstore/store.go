// Package sessionstore 提供管理员与玩家认证会话使用的独立 Valkey 存储。
package sessionstore

import (
	"context"
	"encoding/hex"
	"errors"
	"fmt"
	"strconv"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/redis/go-redis/v9"
)

// Config 描述 Session Store 专用的 Valkey 连接和命名空间。
type Config struct {
	Address, Username, Password, Prefix, Domain string
	Database                                    int
}

// Store 是不复用 Asynq 连接池的认证会话客户端。
type Store struct {
	client         *redis.Client
	prefix, domain string
}

// New 创建独立连接池；Domain 隔离管理员和玩家会话键空间。
func New(config Config) *Store {
	prefix := config.Prefix
	if prefix == "" {
		prefix = "avalon:session"
	}
	domain := config.Domain
	if domain == "" {
		domain = "default"
	}
	return &Store{client: redis.NewClient(&redis.Options{Addr: config.Address, Username: config.Username, Password: config.Password, DB: config.Database}), prefix: prefix, domain: domain}
}
func (s *Store) tokenKey(d []byte) string {
	return s.prefix + ":" + s.domain + ":token:" + hex.EncodeToString(d)
}
func (s *Store) sessionKey(id snowflake.ID) string {
	return s.prefix + ":" + s.domain + ":session:" + id.String()
}
func (s *Store) familyKey(id snowflake.ID) string {
	return s.prefix + ":" + s.domain + ":family:" + id.String()
}
func (s *Store) accountKey(id snowflake.ID) string {
	return s.prefix + ":" + s.domain + ":account:" + id.String()
}

// Ready 探测 Valkey；失败时调用方应阻止服务启动或拒绝认证请求。
func (s *Store) Ready(ctx context.Context) error {
	if s == nil || s.client == nil {
		return errors.New("session store 未初始化")
	}
	probe, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	return s.client.Ping(probe).Err()
}

// Close 关闭 Session Store 的独立连接池。
func (s *Store) Close() error {
	if s == nil || s.client == nil {
		return nil
	}
	return s.client.Close()
}

func valuesToPrincipal(v map[string]string) (authentication.Principal, time.Time, time.Time, time.Time, time.Time, string, error) {
	id, e1 := snowflake.Parse(v["id"])
	fam, e2 := snowflake.Parse(v["family"])
	account, e3 := snowflake.Parse(v["account"])
	security, e4 := strconv.ParseInt(v["security"], 10, 64)
	exp, e5 := strconv.ParseInt(v["expires"], 10, 64)
	idle, e6 := strconv.ParseInt(v["idle"], 10, 64)
	last, e7 := strconv.ParseInt(v["last"], 10, 64)
	created, e8 := strconv.ParseInt(v["created"], 10, 64)
	if e1 != nil || e2 != nil || e3 != nil || e4 != nil || e5 != nil || e6 != nil || e7 != nil || e8 != nil {
		return authentication.Principal{}, time.Time{}, time.Time{}, time.Time{}, time.Time{}, "", errors.New("Valkey 会话记录损坏")
	}
	return authentication.Principal{AccountID: account, SessionID: id, SessionFamilyID: fam, SecurityVersion: security}, time.UnixMilli(exp), time.UnixMilli(idle), time.UnixMilli(last), time.UnixMilli(created), v["device"], nil
}

// CreateSession 写入会话事实、token 索引、会话族索引和账号索引。
func (s *Store) CreateSession(ctx context.Context, record authentication.SessionRecord) error {
	ttl := time.Until(record.ExpiresAt)
	if ttl <= 0 {
		return errors.New("会话已过期")
	}
	token := s.tokenKey(record.SessionTokenDigest)
	pipe := s.client.TxPipeline()
	pipe.HSet(ctx, token, map[string]any{"id": record.ID.String(), "family": record.FamilyID.String(), "account": record.AccountID.String(), "security": record.SecurityVersion, "expires": record.ExpiresAt.UnixMilli(), "idle": record.IdleExpiresAt.UnixMilli(), "last": record.LastActivityAt.UnixMilli(), "created": record.CreatedAt.UnixMilli(), "device": record.DeviceSummary, "revoked": ""})
	pipe.Expire(ctx, token, ttl)
	pipe.Set(ctx, s.sessionKey(record.ID), token, ttl)
	pipe.SAdd(ctx, s.familyKey(record.FamilyID), token)
	pipe.Expire(ctx, s.familyKey(record.FamilyID), ttl)
	pipe.SAdd(ctx, s.accountKey(record.AccountID), record.FamilyID.String())
	pipe.Expire(ctx, s.accountKey(record.AccountID), ttl)
	_, err := pipe.Exec(ctx)
	return err
}

// AuthenticateSession 根据摘要读取仍然有效的会话。
func (s *Store) AuthenticateSession(ctx context.Context, digest []byte, now time.Time) (authentication.Principal, error) {
	v, err := s.client.HGetAll(ctx, s.tokenKey(digest)).Result()
	if err != nil {
		return authentication.Principal{}, err
	}
	if len(v) == 0 || v["revoked"] != "" {
		return authentication.Principal{}, authentication.ErrSessionNotFound
	}
	p, exp, idle, _, _, _, err := valuesToPrincipal(v)
	if err != nil {
		return p, err
	}
	if !exp.After(now) || !idle.After(now) {
		return authentication.Principal{}, authentication.ErrSessionNotFound
	}
	return p, nil
}

// RotateRefreshSession 使用 Lua 原子消费旧 token、写入下一 token，并在重放时撤销会话族。
func (s *Store) RotateRefreshSession(ctx context.Context, digest, nextDigest []byte, nextID snowflake.ID, now time.Time, idleTTL time.Duration) (authentication.Principal, time.Time, error) {
	old := s.tokenKey(digest)
	next := s.tokenKey(nextDigest)
	script := redis.NewScript(`local old=KEYS[1]; local next=KEYS[2]; local now=tonumber(ARGV[1]); local idlettl=tonumber(ARGV[2]); local revoked=redis.call('HGET',old,'revoked') or ''; local exp=tonumber(redis.call('HGET',old,'expires') or '0'); local idle=tonumber(redis.call('HGET',old,'idle') or '0'); local family=redis.call('HGET',old,'family') or ''; local familykey=ARGV[3]..family; if revoked~='' then local members=redis.call('SMEMBERS',familykey); for _,m in ipairs(members) do redis.call('HSET',m,'revoked','refresh_replay') end; return {'replay'} end; if exp<=now or idle<=now then return {'missing'} end; local account=redis.call('HGET',old,'account'); local security=redis.call('HGET',old,'security'); local created=redis.call('HGET',old,'created'); local device=redis.call('HGET',old,'device') or ''; local nextidle=math.min(exp,now+idlettl); redis.call('HSET',old,'revoked','rotated'); redis.call('HSET',next,'id',ARGV[4],'family',family,'account',account,'security',security,'expires',exp,'idle',nextidle,'last',now,'created',created,'device',device,'revoked',''); redis.call('PEXPIRE',next,math.floor((exp-now))); redis.call('SET',ARGV[5],next,'PX',math.floor((exp-now))); redis.call('SADD',familykey,next); return {'ok',account,family,security,exp,nextidle,created,device}`)
	raw, err := script.Run(ctx, s.client, []string{old, next}, now.UnixMilli(), idleTTL.Milliseconds(), s.prefix+":"+s.domain+":family:", nextID.String(), s.sessionKey(nextID)).Result()
	if err != nil {
		return authentication.Principal{}, time.Time{}, err
	}
	rawValues, ok := raw.([]interface{})
	if !ok {
		return authentication.Principal{}, time.Time{}, errors.New("Valkey refresh 返回格式无效")
	}
	r := make([]string, len(rawValues))
	for i, value := range rawValues {
		r[i] = fmt.Sprint(value)
	}
	if len(r) == 0 || r[0] != "ok" {
		if len(r) > 0 && r[0] == "replay" {
			return authentication.Principal{}, time.Time{}, authentication.ErrRefreshReplay
		}
		return authentication.Principal{}, time.Time{}, authentication.ErrSessionNotFound
	}
	account, _ := snowflake.Parse(r[1])
	family, _ := snowflake.Parse(r[2])
	security, _ := strconv.ParseInt(r[3], 10, 64)
	expN, _ := strconv.ParseInt(r[4], 10, 64)
	return authentication.Principal{AccountID: account, SessionID: nextID, SessionFamilyID: family, SecurityVersion: security}, time.UnixMilli(expN), nil
}

// TouchSessionActivity 按会话 ID 更新最近活动和空闲期限。
func (s *Store) TouchSessionActivity(ctx context.Context, id snowflake.ID, last, idle, writeBefore time.Time) error {
	token, err := s.client.Get(ctx, s.sessionKey(id)).Result()
	if err != nil {
		return authentication.ErrSessionNotFound
	}
	_, err = s.client.Eval(ctx, `local t=KEYS[1]; if (redis.call('HGET',t,'revoked') or '')~='' then return 0 end; local previous=tonumber(redis.call('HGET',t,'last') or '0'); if previous>=tonumber(ARGV[2]) then return 0 end; redis.call('HSET',t,'last',ARGV[1],'idle',ARGV[3]); return 1`, []string{token}, last.UnixMilli(), writeBefore.UnixMilli(), idle.UnixMilli()).Result()
	return err
}

// ListActiveSessionFamilies 返回账号下仍然有效的会话族。
func (s *Store) ListActiveSessionFamilies(ctx context.Context, account snowflake.ID, now time.Time) ([]authentication.SessionFamily, error) {
	families, err := s.client.SMembers(ctx, s.accountKey(account)).Result()
	if err != nil {
		return nil, err
	}
	out := make([]authentication.SessionFamily, 0, len(families))
	for _, f := range families {
		fid, e := snowflake.Parse(f)
		if e != nil {
			continue
		}
		members, e := s.client.SMembers(ctx, s.familyKey(fid)).Result()
		if e != nil {
			return nil, e
		}
		for _, token := range members {
			v, e := s.client.HGetAll(ctx, token).Result()
			if e != nil {
				return nil, e
			}
			if len(v) == 0 || v["revoked"] != "" {
				continue
			}
			p, exp, idle, last, created, device, e := valuesToPrincipal(v)
			if e != nil || p.AccountID != account || !exp.After(now) || !idle.After(now) {
				continue
			}
			out = append(out, authentication.SessionFamily{FamilyID: fid, DeviceSummary: device, CreatedAt: created, LastActivityAt: last, ExpiresAt: exp, IdleExpiresAt: idle})
			break
		}
	}
	return out, nil
}

// RevokeSessionFamily 撤销会话族所有 token。
func (s *Store) RevokeSessionFamily(ctx context.Context, family snowflake.ID, now time.Time) error {
	members, err := s.client.SMembers(ctx, s.familyKey(family)).Result()
	if err != nil {
		return err
	}
	pipe := s.client.Pipeline()
	for _, token := range members {
		pipe.HSet(ctx, token, "revoked", "logout")
		pipe.Expire(ctx, token, time.Until(now.Add(365*24*time.Hour)))
	}
	_, err = pipe.Exec(ctx)
	return err
}

// Key 返回经过命名空间隔离的会话键，供健康检查和诊断使用。
func (s *Store) Key(domain, kind, id string) string {
	return fmt.Sprintf("%s:%s:%s:%s", s.prefix, domain, kind, id)
}
