package io.github.lishangbu.avalon.chat.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.chat.application.model.ChatPage;
import io.github.lishangbu.avalon.chat.application.model.ChatSummary;
import io.github.lishangbu.avalon.chat.domain.ChatRules;
import io.github.lishangbu.avalon.chat.persistence.repository.ChatStore;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 只读入口统一拒绝战斗期间查看正文、会话与未读摘要；实时连接不经过此门禁，因为它不传正文。 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService {

	private final CharacterQueryService characters;

	private final ChatStore store;

	/** 读取当前已提交战斗状态，不获取写锁；发送与已读命令另用角色锁消除开战并发。 */
	public long requireReadable(long owner) {
		long self = characters.requireId(owner);
		if (characters.adventure(owner).battle() != null)
			throw new BusinessException(409, "战斗期间不能查看聊天消息");
		return self;
	}

	/** 摘要无消息正文，但同样受战斗限制，避免通过会话预览绕开不能查看的规则。 */
	public ChatSummary summary(long owner) {
		long self = requireReadable(owner);
		return new ChatSummary(Long.toString(self), store.rejectStrangers(self), store.conversations(self),
				store.systemUnread(self, cutoff()), store.blocks(self));
	}

	/** 用户只选择对端角色，私聊查询始终固定本人为另一端，不能传入任意双方查看他人对话。 */
	public ChatPage history(long owner, String channel, String peer, long before) {
		ChatRules.channel(channel);
		if (before < 0)
			throw new BusinessException(400, "聊天分页位置无效");
		long self = requireReadable(owner);
		long target = "PRIVATE".equals(channel) ? characters.publicPlayer(peer).id() : 0;
		if (target == self)
			throw new BusinessException(400, "不能与自己私聊");
		var rows = store.history(self, channel, target, before, cutoff());
		var entries = rows.stream().limit(50).toList();
		return new ChatPage(entries, rows.size() > 50 ? entries.getLast().id() : null);
	}

	/** 过期过滤即时生效，不依赖后台清理刚好执行；维护任务稍晚运行不会展示过期公开消息。 */
	public static Instant cutoff() {
		return Instant.now().minus(7, ChronoUnit.DAYS);
	}

}
