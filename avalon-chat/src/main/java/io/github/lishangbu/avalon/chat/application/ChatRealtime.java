package io.github.lishangbu.avalon.chat.application;

import io.github.lishangbu.avalon.chat.application.model.ChatViews.Changed;
import java.io.IOException;
import java.util.List;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 单实例实时唤醒通道，仅发送常量更新提示，绝不传消息正文、对端角色或未读预览。 战斗期间连接照常存活，正文和已读受另一个鉴权接口约束；数据库才是断线后的消息来源。
 * 45秒自动结束连接，客户端重新以Bearer认证接入，旧连接不会无限延长失效令牌的权限。 本组件仅管理活跃连接，不保存消息、不判断角色战斗状态、不自行开启数据库事务。
 */
@Component
public class ChatRealtime {

	/** 每个活跃Emitter只对应一个服务端解析的角色；断开后移除，不为历史角色永久保留空集合。 */
	private final ConcurrentHashMap<SseEmitter, Long> clients = new ConcurrentHashMap<>();

	/** 应用关闭后不再接受新流；与注册共用短临界区，防止回收快照遗漏并发新连接。 */
	private boolean closing;

	/**
	 * 每角色最多四个实时连接，覆盖常用多标签页；超过时关闭一个旧连接，客户端仍可重新鉴权接入。
	 * 注册与限额检查使用同一短临界区，避免并发开标签页突破限额；不在此临界区发送网络数据。
	 * 完成、超时和异常都清理同一Emitter键，清理幂等，不能误移除同角色后来建立的新连接。
	 */
	public SseEmitter connect(long character) {
		var emitter = new SseEmitter(45_000L);
		SseEmitter evicted = null;
		synchronized (clients) {
			if (closing)
				throw new BusinessException(503, "聊天服务正在重启，请稍后重试");
			var existing = clients.entrySet().stream().filter(e -> e.getValue() == character).toList();
			if (existing.size() >= 4) {
				evicted = existing.getFirst().getKey();
				clients.remove(evicted);
			}
			clients.put(emitter, character);
		}
		if (evicted != null)
			finish(evicted);
		Runnable remove = () -> clients.remove(emitter);
		emitter.onCompletion(remove);
		// 正常45秒轮换是连接生命周期，不是业务异常；主动complete避免MVC把它记为异步超时错误。
		emitter.onTimeout(() -> {
			remove.run();
			finish(emitter);
		});
		emitter.onError(ignored -> remove.run());
		// ready也触发一次数据库补齐，覆盖连接建立前后的提交空隙以及服务器重启。
		send(emitter, "ready");
		return emitter;
	}

	/**
	 * 默认AFTER_COMMIT，失败奖励事务不会发更新；私聊更新只送双方，个人通知只送本人。
	 * 公开消息recipient为null；其余事件按角色编号过滤，不向陌生角色暴露私聊发生时间。
	 */
	@TransactionalEventListener
	public void changed(Changed event) {
		clients.forEach((emitter, character) -> {
			if (event.recipient() == null || event.recipient().equals(character)
					|| (event.sender() != null && event.sender().equals(character)))
				send(emitter, "changed");
		});
	}

	/**
	 * ContextClosedEvent早于Web服务器的优雅关闭阶段：先结束长期SSE响应，避免退出等待整个45秒期限。
	 * 回收快照后在锁外complete，完成回调可安全从注册表删除，不在锁内调用网络响应生命周期。
	 * 只关闭传输连接，不删除数据库消息、已读或偏好；客户端重连后由ready继续补齐。
	 */
	@EventListener
	public void close(ContextClosedEvent event) {
		List<SseEmitter> snapshot;
		synchronized (clients) {
			closing = true;
			snapshot = List.copyOf(clients.keySet());
			clients.clear();
		}
		snapshot.forEach(this::finish);
	}

	/** 周期心跳确保代理及时刷新流，并发现已关闭标签页；无正文心跳不受战斗状态影响。 */
	public void heartbeat() {
		clients.keySet().forEach(emitter -> send(emitter, "heartbeat"));
	}

	/**
	 * IOException通常来自断网，IllegalStateException通常来自已经完成的异步响应，两者都清理连接。
	 * 网络失败不回滚已提交聊天，也不改变未读；重连ready会通过数据库恢复内容。
	 */
	private void send(SseEmitter emitter, String type) {
		try {
			emitter.send(SseEmitter.event().name(type).data(type));
		}
		catch (IOException | IllegalStateException disconnected) {
			// 写出失败后的异步响应由容器onError完成；此处不能再次complete，否则会在已失效AsyncContext上重复分派。
			// 只撤销本组件的连接引用，已提交消息留在数据库，其他连接继续收到本次更新。
			clients.remove(emitter);
		}
	}

	/** 正常轮换/退出时结束流；如果容器已在另一线程结束该响应，忽略重复完成的状态异常。 */
	private void finish(SseEmitter emitter) {
		try {
			emitter.complete();
		}
		catch (IllegalStateException alreadyClosed) {
			// 注册表已移除此连接，不能因失效传输打断应用关闭或新连接建立。
		}
	}

}
