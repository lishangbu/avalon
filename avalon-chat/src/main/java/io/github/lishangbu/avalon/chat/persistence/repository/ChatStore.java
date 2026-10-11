package io.github.lishangbu.avalon.chat.persistence.repository;

import io.github.lishangbu.avalon.chat.application.model.ChatViews.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 聊天集合存储：每个 SQL 都显式约束接收者、频道或角色，不接受客户端提供的账号身份。 JdbcClient
 * 使用应用共享事务连接，写入加入最外层业务事务；不会单独提交通知或已读。 逐条已读避免把雪花编号当作提交水位：较小编号的事务可能更晚提交，届时仍应保持未读。
 */
@Repository
public class ChatStore {

	/** 注入共享连接适配，所有写入加入调用方事务，不能在本存储层新开独立事务。 */
	private final JdbcClient jdbc;

	/** 与普通实体共用CosId，不自行创建第二套雪花生成器或把自增号当作玩家编号。 */
	private final IdGenerator ids;

	public ChatStore(JdbcClient jdbc, @Qualifier(IdGeneratorProvider.SHARE + "SnowflakeId") IdGenerator ids) {
		this.jdbc = jdbc;
		this.ids = ids;
	}

	/** 仅有限的固定 SQL 片段可拼接；所有玩家值始终通过绑定参数传入。 */
	private static final String VISIBLE = """
			((:channel='WORLD' AND m.channel='WORLD') OR
			 (:channel='SYSTEM' AND m.channel='SYSTEM' AND (m.recipient IS NULL OR m.recipient=:self)) OR
			 (:channel='PRIVATE' AND m.channel='PRIVATE' AND
			  ((m.sender=:self AND m.recipient=:peer) OR (m.sender=:peer AND m.recipient=:self))))
			AND NOT EXISTS(SELECT 1 FROM chat_block b WHERE b.character_id=:self AND b.peer=m.sender)
			AND (m.channel='PRIVATE' OR m.recipient IS NOT NULL OR m.created_at>=:cutoff)
			""";

	/** 系统公告用公开过期规则，个人通知与私聊没有时间裁剪。read 对发件人表示收件人已读。 */
	private static final String PROJECTION = """
			SELECT m.*, EXISTS(SELECT 1 FROM chat_seen s WHERE s.message_id=m.id
			  AND s.character_id=CASE WHEN m.channel='PRIVATE' THEN m.recipient ELSE :self END) AS was_read
			FROM chat_message m
			""";

	/** 跨请求幂等先查询持锁发送者的原消息；调用者还须比较频道、正文与收件人。 */
	public Message prior(long self, String request) {
		return jdbc.sql(PROJECTION + " WHERE m.sender=:self AND m.request_id=:request")
			.param("self", self)
			.param("request", request)
			.query(this::message)
			.optional()
			.orElse(null);
	}

	/** 同角色发送在角色锁内串行，数据库唯一索引进一步保护请求号；系统来源键也有独立唯一约束。 */
	public Message insert(String channel, Long sender, String name, Long recipient, String recipientName, String text,
			String request) {
		long id = ids.generate();
		jdbc.sql(
				"""
						INSERT INTO chat_message(id,channel,sender,sender_name,recipient,recipient_name,text,created_at,request_id)
						VALUES(:id,:channel,:sender,:name,:recipient,:recipientName,:text,clock_timestamp(),:request)
						ON CONFLICT DO NOTHING
						""")
			.param("id", id)
			.param("channel", channel)
			.param("sender", sender)
			.param("name", name)
			.param("recipient", recipient)
			.param("recipientName", recipientName)
			.param("text", text)
			.param("request", request)
			.update();
		return jdbc
			.sql(PROJECTION + " WHERE " + (sender == null ? "m.sender IS NULL" : "m.sender=:self")
					+ " AND m.request_id=:request")
			.param("self", sender == null ? 0L : sender)
			.param("request", request)
			.query(this::message)
			.single();
	}

	/** 最多取51条判断是否还有下一页，避免COUNT全部历史；before=0表示最新一页。 */
	public List<Message> history(long self, String channel, long peer, long before, Instant cutoff) {
		return jdbc
			.sql(PROJECTION + " WHERE " + VISIBLE + " AND (:before=0 OR m.id<:before) ORDER BY m.id DESC LIMIT 51")
			.param("self", self)
			.param("channel", channel)
			.param("peer", peer)
			.param("before", before)
			.param("cutoff", Timestamp.from(cutoff))
			.query(this::message)
			.list();
	}

	/** 先确认through当前确实可见；用其他人的消息编号或已经过期的公告不能推进本人的已读。 */
	public boolean visible(long self, String channel, long peer, long through, Instant cutoff) {
		return jdbc.sql("SELECT EXISTS(SELECT 1 FROM chat_message m WHERE " + VISIBLE + " AND m.id=:through)")
			.param("self", self)
			.param("channel", channel)
			.param("peer", peer)
			.param("through", through)
			.param("cutoff", Timestamp.from(cutoff))
			.query(Boolean.class)
			.single();
	}

	/** 只为当前已提交且实际加载区间内的消息建凭证；重复确认不改变首次阅读时刻。 */
	public int read(long self, String channel, long peer, long through, Instant cutoff) {
		return jdbc
			.sql("INSERT INTO chat_seen(character_id,message_id,seen_at) SELECT :self,m.id,clock_timestamp() "
					+ "FROM chat_message m WHERE " + VISIBLE + " AND m.id<=:through ON CONFLICT DO NOTHING")
			.param("self", self)
			.param("channel", channel)
			.param("peer", peer)
			.param("through", through)
			.param("cutoff", Timestamp.from(cutoff))
			.update();
	}

	/** 会话从实际消息形成，无需允许任意客户端创建陌生会话。公开名快照避免暴露跨模块账号资料。 */
	public List<Conversation> conversations(long self) {
		return jdbc
			.sql("""
					SELECT peer, max(name) AS name, count(*) FILTER(WHERE incoming AND NOT seen) AS unread, max(id) AS latest
					FROM (SELECT m.id, CASE WHEN m.sender=:self THEN m.recipient ELSE m.sender END AS peer,
					  CASE WHEN m.sender=:self THEN m.recipient_name ELSE m.sender_name END AS name,
					  m.recipient=:self AS incoming,
					  EXISTS(SELECT 1 FROM chat_seen s WHERE s.character_id=:self AND s.message_id=m.id) AS seen
					  FROM chat_message m WHERE m.channel='PRIVATE' AND (m.sender=:self OR m.recipient=:self)) c
					WHERE NOT EXISTS(SELECT 1 FROM chat_block b WHERE b.character_id=:self AND b.peer=c.peer)
					GROUP BY peer ORDER BY latest DESC
					""")
			.param("self", self)
			.query((rs, row) -> new Conversation(rs.getString("peer"), rs.getString("name"), rs.getLong("unread"),
					rs.getString("latest")))
			.list();
	}

	/** 计算本人尚未阅读的个人通知及有效全服公告；其他角色的通知不参与计数。 */
	public long systemUnread(long self, Instant cutoff) {
		return jdbc.sql("""
				SELECT count(*) FROM chat_message m WHERE m.channel='SYSTEM'
				  AND (m.recipient=:self OR (m.recipient IS NULL AND m.created_at>=:cutoff))
				  AND NOT EXISTS(SELECT 1 FROM chat_seen s WHERE s.character_id=:self AND s.message_id=m.id)
				""").param("self", self).param("cutoff", Timestamp.from(cutoff)).query(Long.class).single();
	}

	/** 未设置的角色默认允许陌生人私聊；读取绝不为了默认值插入一行。 */
	public boolean rejectStrangers(long self) {
		return jdbc.sql("SELECT reject_strangers FROM chat_settings WHERE character_id=:self")
			.param("self", self)
			.query(Boolean.class)
			.optional()
			.orElse(false);
	}

	/** 仅更新拒收开关，保留last_sent_at，不能通过切换设置重置发送间隔。 */
	public void settings(long self, boolean reject) {
		jdbc.sql("""
				INSERT INTO chat_settings(character_id,reject_strangers) VALUES(:self,:reject)
				ON CONFLICT(character_id) DO UPDATE SET reject_strangers=EXCLUDED.reject_strangers
				""").param("self", self).param("reject", reject).update();
	}

	/** 双向检查：自己拉黑对方或对方拉黑自己，都拒绝后续私聊；不暴露具体是哪一方设置。 */
	public boolean blockedEither(long self, long peer) {
		return jdbc.sql("""
				SELECT EXISTS(SELECT 1 FROM chat_block WHERE
				  (character_id=:self AND peer=:peer) OR (character_id=:peer AND peer=:self))
				""").param("self", self).param("peer", peer).query(Boolean.class).single();
	}

	/** 仅列出本人主动建立的黑名单，不允许查询被谁拉黑，也不泄漏对方的设置。 */
	public List<Blocked> blocks(long self) {
		return jdbc.sql("SELECT peer,name FROM chat_block WHERE character_id=:self ORDER BY peer")
			.param("self", self)
			.query((rs, row) -> new Blocked(rs.getString("peer"), rs.getString("name")))
			.list();
	}

	/** 幂等建立有方向的黑名单；不删除消息或好友关系，解除后历史仍可读取。 */
	public void block(long self, long peer, String name) {
		jdbc.sql("INSERT INTO chat_block(character_id,peer,name) VALUES(:self,:peer,:name) ON CONFLICT DO NOTHING")
			.param("self", self)
			.param("peer", peer)
			.param("name", name)
			.update();
	}

	/** 删除限定本人归属的黑名单；其他角色的记录即使peer相同也不能受影响。 */
	public void unblock(long self, long peer) {
		jdbc.sql("DELETE FROM chat_block WHERE character_id=:self AND peer=:peer")
			.param("self", self)
			.param("peer", peer)
			.update();
	}

	/** 成功发送的间隔存数据库，不因重启、多标签页或切换频道绕过；只统计最近一分钟的玩家消息。 */
	public boolean rateLimited(long self) {
		return jdbc
			.sql("""
					SELECT EXISTS(SELECT 1 FROM chat_settings WHERE character_id=:self
					  AND last_sent_at>clock_timestamp()-interval '2 seconds') OR
					  (SELECT count(*) FROM chat_message WHERE sender=:self AND created_at>clock_timestamp()-interval '1 minute')>=20
					""")
			.param("self", self)
			.query(Boolean.class)
			.single();
	}

	/** 成功消息与最近发送时刻同事务持久化；冲突更新只改时间，保留已有拒收选择。 */
	public void sent(long self) {
		jdbc.sql(
				"""
						INSERT INTO chat_settings(character_id,reject_strangers,last_sent_at) VALUES(:self,false,clock_timestamp())
						ON CONFLICT(character_id) DO UPDATE SET last_sent_at=EXCLUDED.last_sent_at
						""")
			.param("self", self)
			.update();
	}

	/** 分批删除公开消息，级联删除对应已读凭证；永久私聊和个人通知不进入清理集合。 */
	public int clean(Instant cutoff) {
		return jdbc.sql("""
				DELETE FROM chat_message WHERE id IN (SELECT id FROM chat_message
				  WHERE (channel='WORLD' OR (channel='SYSTEM' AND recipient IS NULL)) AND created_at<:cutoff
				  ORDER BY created_at LIMIT 1000)
				""").param("cutoff", Timestamp.from(cutoff)).update();
	}

	/** ResultSet.getString保持空ID为null；禁止把空收件人转成0，避免误当公开消息的合法角色。 */
	private Message message(ResultSet rs, int row) throws SQLException {
		return new Message(rs.getString("id"), rs.getString("channel"), rs.getString("sender"),
				rs.getString("sender_name"), rs.getString("recipient"), rs.getString("text"),
				rs.getTimestamp("created_at").toInstant(), rs.getBoolean("was_read"));
	}

}
