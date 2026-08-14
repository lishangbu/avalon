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

const pendingSessionTTL = time.Minute

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

// StageSession 写入短期 pending 会话事实，但不建立任何可认证索引。
func (s *Store) StageSession(ctx context.Context, record authentication.SessionRecord) error {
	ttl := min(time.Until(record.ExpiresAt), pendingSessionTTL)
	if ttl <= 0 {
		return errors.New("会话已过期")
	}
	token := s.tokenKey(record.SessionTokenDigest)
	script := redis.NewScript(`if redis.call('EXISTS',KEYS[1])~=0 then return 0 end; redis.call('HSET',KEYS[1],'id',ARGV[1],'family',ARGV[2],'account',ARGV[3],'security',ARGV[4],'expires',ARGV[5],'idle',ARGV[6],'last',ARGV[7],'created',ARGV[8],'device',ARGV[9],'state','pending','revoked',''); redis.call('PEXPIRE',KEYS[1],ARGV[10]); return 1`)
	created, err := script.Run(ctx, s.client, []string{token}, record.ID.String(), record.FamilyID.String(), record.AccountID.String(), record.SecurityVersion, record.ExpiresAt.UnixMilli(), record.IdleExpiresAt.UnixMilli(), record.LastActivityAt.UnixMilli(), record.CreatedAt.UnixMilli(), record.DeviceSummary, ttl.Milliseconds()).Int()
	if err != nil {
		return err
	}
	if created != 1 {
		return errors.New("会话摘要已存在")
	}
	return nil
}

// ActivateSession 在 PostgreSQL 登录事实提交后原子激活 pending 会话并建立全部查询索引。
func (s *Store) ActivateSession(ctx context.Context, digest []byte, securityVersion int64) error {
	now := time.Now().UTC().UnixMilli()
	script := redis.NewScript(`local token=KEYS[1]; local state=redis.call('HGET',token,'state') or ''; if state=='active' then return 1 end; if state~='pending' then return 0 end; local expires=tonumber(redis.call('HGET',token,'expires') or '0'); if expires<=tonumber(ARGV[1]) then redis.call('DEL',token); return 0 end; local id=redis.call('HGET',token,'id'); local family=redis.call('HGET',token,'family'); local account=redis.call('HGET',token,'account'); if not id or not family or not account then redis.call('DEL',token); return 0 end; local ttl=expires-tonumber(ARGV[1]); local sessionkey=ARGV[3]..id; local familykey=ARGV[4]..family; local accountkey=ARGV[5]..account; redis.call('HSET',token,'state','active','security',ARGV[2]); redis.call('PEXPIRE',token,ttl); redis.call('SET',sessionkey,token,'PX',ttl); redis.call('SADD',familykey,token); redis.call('PEXPIRE',familykey,ttl); redis.call('SADD',accountkey,family); redis.call('PEXPIRE',accountkey,ttl); return 1`)
	activated, err := script.Run(ctx, s.client, []string{s.tokenKey(digest)}, now, securityVersion, s.prefix+":"+s.domain+":session:", s.prefix+":"+s.domain+":family:", s.prefix+":"+s.domain+":account:").Int()
	if err != nil {
		return err
	}
	if activated != 1 {
		return authentication.ErrSessionNotFound
	}
	return nil
}

// AbortSession 删除 pending 或已激活的单次登录会话及其索引；重复调用保持幂等。
func (s *Store) AbortSession(ctx context.Context, digest []byte) error {
	script := redis.NewScript(`local token=KEYS[1]; if redis.call('EXISTS',token)==0 then return 1 end; local id=redis.call('HGET',token,'id') or ''; local family=redis.call('HGET',token,'family') or ''; local account=redis.call('HGET',token,'account') or ''; if id~='' then redis.call('DEL',ARGV[1]..id) end; if family~='' then local familykey=ARGV[2]..family; redis.call('SREM',familykey,token); if redis.call('SCARD',familykey)==0 then redis.call('DEL',familykey); if account~='' then redis.call('SREM',ARGV[3]..account,family) end end end; redis.call('DEL',token); return 1`)
	return script.Run(ctx, s.client, []string{s.tokenKey(digest)}, s.prefix+":"+s.domain+":session:", s.prefix+":"+s.domain+":family:", s.prefix+":"+s.domain+":account:").Err()
}

// AuthenticateSession 根据摘要读取仍然有效的会话。
func (s *Store) AuthenticateSession(ctx context.Context, digest []byte, now time.Time) (authentication.Principal, error) {
	v, err := s.client.HGetAll(ctx, s.tokenKey(digest)).Result()
	if err != nil {
		return authentication.Principal{}, err
	}
	if len(v) == 0 || v["state"] != "active" || v["revoked"] != "" {
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
	script := redis.NewScript(`local old=KEYS[1]; local next=KEYS[2]; local now=tonumber(ARGV[1]); local idlettl=tonumber(ARGV[2]); local state=redis.call('HGET',old,'state') or ''; if state~='active' then return {'missing'} end; local revoked=redis.call('HGET',old,'revoked') or ''; local exp=tonumber(redis.call('HGET',old,'expires') or '0'); local idle=tonumber(redis.call('HGET',old,'idle') or '0'); local family=redis.call('HGET',old,'family') or ''; local familykey=ARGV[3]..family; if revoked~='' then local members=redis.call('SMEMBERS',familykey); for _,m in ipairs(members) do redis.call('HSET',m,'revoked','refresh_replay') end; return {'replay'} end; if exp<=now or idle<=now then return {'missing'} end; local account=redis.call('HGET',old,'account'); local security=redis.call('HGET',old,'security'); local created=redis.call('HGET',old,'created'); local device=redis.call('HGET',old,'device') or ''; local nextidle=math.min(exp,now+idlettl); redis.call('HSET',old,'revoked','rotated'); redis.call('HSET',next,'id',ARGV[4],'family',family,'account',account,'security',security,'expires',exp,'idle',nextidle,'last',now,'created',created,'device',device,'state','active','revoked',''); redis.call('PEXPIRE',next,math.floor((exp-now))); redis.call('SET',ARGV[5],next,'PX',math.floor((exp-now))); redis.call('SADD',familykey,next); return {'ok',account,family,security,exp,nextidle,created,device}`)
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
	_, err = s.client.Eval(ctx, `local t=KEYS[1]; if (redis.call('HGET',t,'state') or '')~='active' or (redis.call('HGET',t,'revoked') or '')~='' then return 0 end; local previous=tonumber(redis.call('HGET',t,'last') or '0'); if previous>=tonumber(ARGV[2]) then return 0 end; redis.call('HSET',t,'last',ARGV[1],'idle',ARGV[3]); return 1`, []string{token}, last.UnixMilli(), writeBefore.UnixMilli(), idle.UnixMilli()).Result()
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
			if len(v) == 0 || v["state"] != "active" || v["revoked"] != "" {
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
