package io.github.lishangbu.avalon.chat.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.character.application.query.FriendshipQueryService;
import io.github.lishangbu.avalon.chat.application.model.ChatChanged;
import io.github.lishangbu.avalon.chat.application.model.ChatMessage;
import io.github.lishangbu.avalon.chat.application.model.ChatRead;
import io.github.lishangbu.avalon.chat.application.model.ChatSend;
import io.github.lishangbu.avalon.chat.application.query.ChatQueryService;
import io.github.lishangbu.avalon.chat.domain.ChatRules;
import io.github.lishangbu.avalon.chat.persistence.repository.ChatStore;
import io.github.lishangbu.avalon.content.text.ForbiddenWordsCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 所有玩家写入口在角色锁内校验战斗门禁，和开战使用同一锁，不能通过手工HTTP或另一个标签页绕过。
 * 成功发送与频率状态、已读凭证同事务提交；提交后才通知实时连接，回滚不会产生幽灵消息。
 */
@Component
@RequiredArgsConstructor
public class ChatCommandService {

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final FriendshipQueryService friends;

	private final ForbiddenWordsCatalog forbidden;

	private final ChatStore store;

	private final ApplicationEventPublisher events;

	/** 验证格式后锁本人角色，先处理已成功回执，再核验内容/收件权限/频率，最后原子插入消息与发送时间。 */
	@Transactional
	public ChatMessage send(long owner, ChatSend request) {
		if (request == null)
			throw new BusinessException(400, "缺少消息");
		String channel = ChatRules.channel(request.channel());
		if ("SYSTEM".equals(channel))
			throw new BusinessException(403, "系统频道仅接收游戏事件通知");
		ChatRules.request(request.requestId());
		String text = ChatRules.text(request.text());
		long self = characters.requireCommunication(owner).id();
		var peer = "PRIVATE".equals(channel) ? queries.publicPlayer(request.peer()) : null;
		Long recipient = peer == null ? null : peer.id();
		if (Objects.equals(recipient, self))
			throw new BusinessException(400, "不能与自己私聊");
		var prior = store.prior(self, request.requestId());
		// 成功请求可安全重放；同请求号改正文、频道或收件人视为冲突，不把旧回执当成新发送。
		if (prior != null) {
			if (!prior.channel().equals(channel) || !prior.text().equals(text)
					|| !Objects.equals(prior.recipient(), recipient == null ? null : recipient.toString()))
				throw new BusinessException(409, "该聊天请求已发送，不能修改消息");
			return prior;
		}
		if (forbidden.contains(text))
			throw new BusinessException(400, "消息包含不允许使用的内容，请修改后重试");
		if (recipient != null) {
			if (store.blockedEither(self, recipient))
				throw new BusinessException(403, "无法向该玩家发送私聊");
			if (store.rejectStrangers(recipient) && friends.friendOwner(owner, recipient) == null)
				throw new BusinessException(403, "对方拒收陌生人私聊，请先成为好友");
		}
		if (store.rateLimited(self))
			throw new BusinessException(429, "发送过于频繁，请稍后再试");
		var result = store.insert(channel, self, queries.publicName(self), recipient, peer == null ? null : peer.name(),
				text, request.requestId());
		store.sent(self);
		events.publishEvent(new ChatChanged(self, recipient));
		return result;
	}

	/** 战斗期间不标记已读，后台收到更新不等于玩家阅读；私聊回执通知双方刷新已读状态。 */
	@Transactional
	public void read(long owner, ChatRead request) {
		if (request == null)
			throw new BusinessException(400, "缺少已读确认");
		String channel = ChatRules.channel(request.channel());
		long self = characters.requireCommunication(owner).id();
		long peer = "PRIVATE".equals(channel) ? queries.publicPlayer(request.peer()).id() : 0;
		long through;
		try {
			through = Long.parseLong(request.through());
		}
		catch (RuntimeException invalid) {
			throw new BusinessException(400, "已读消息编号无效");
		}
		if (through <= 0 || !store.visible(self, channel, peer, through, ChatQueryService.cutoff()))
			throw new BusinessException(404, "消息不存在或不可查看");
		int changed = store.read(self, channel, peer, through, ChatQueryService.cutoff());
		// 世界频道不显示已读，系统已读只刷新本人的其他标签页，避免每次阅读全服广播。
		if (changed > 0 && !"WORLD".equals(channel))
			events.publishEvent(new ChatChanged(self, peer == 0 ? self : peer));
	}

	/** 拒收开关按角色持久化，修改不影响已有历史；与开战共用角色锁，战斗期间不可操作。 */
	@Transactional
	public void settings(long owner, boolean reject) {
		long self = characters.requireCommunication(owner).id();
		store.settings(self, reject);
		events.publishEvent(new ChatChanged(self, self));
	}

	/** 按精确角色名解析公开身份，黑名单只改变本人的接收与显示权限，不授予修改对方资料的能力。 */
	@Transactional
	public void block(long owner, String name) {
		long self = characters.requireCommunication(owner).id();
		var peer = queries.publicPlayer(name);
		if (self == peer.id())
			throw new BusinessException(400, "不能拉黑自己");
		store.block(self, peer.id(), peer.name());
		events.publishEvent(new ChatChanged(self, self));
	}

	/** 解除本人建立的黑名单，不删除既有消息；操作幂等，不能解除其他角色的黑名单。 */
	@Transactional
	public void unblock(long owner, long peer) {
		long self = characters.requireCommunication(owner).id();
		store.unblock(self, peer);
		events.publishEvent(new ChatChanged(self, self));
	}

	/** 无玩家HTTP入口的可信系统消息写入；与任务/奖励事务共用提交，业务来源键防止重试重复通知。 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void notify(Long owner, String source, String text) {
		if (source == null || source.length() > 100)
			throw new IllegalArgumentException("通知缺少有效来源");
		Long recipient = owner == null ? null : queries.requireId(owner);
		// 系统文字来自服务端内容目录，不能用玩家禁词过滤误拦合法任务标题；仍遵守存储长度上限。
		store.insert("SYSTEM", null, "系统", recipient, null, ChatRules.text(text), source);
		events.publishEvent(new ChatChanged(null, recipient));
	}

	/** 周期任务只清理公共历史，永不触碰个人私聊、个人通知或拒收设置。 */
	@Transactional
	public int clean() {
		return store.clean(ChatQueryService.cutoff());
	}

}
