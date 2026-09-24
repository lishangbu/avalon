package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.application.model.SessionTokens;
import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.identity.domain.TokenDigest;
import io.github.lishangbu.avalon.identity.persistence.entity.DeviceSession;
import io.github.lishangbu.avalon.identity.persistence.entity.RefreshReceipt;
import io.github.lishangbu.avalon.identity.persistence.repository.RefreshReceiptRepository;
import io.github.lishangbu.avalon.identity.persistence.repository.SessionRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * 不透明 Token 生命周期。刷新复用采取严格策略：撤销该设备，要求重新登录。
 */
@Component
@RequiredArgsConstructor
public class SessionCommandService {

	private final SessionRepository sessions;

	private final RefreshReceiptRepository receipts;

	private final SecureRandom random = new SecureRandom();

	/** 访问令牌存活 15 分钟；设备最长存活 30 天，刷新不无限延长会话。 */
	/**
	 * 创建独立设备会话，生成两枚随机令牌并仅持久化摘要。设备绝对寿命为30天，访问令牌寿命为900秒；注册调用时加入账号创建事务，不能出现账号已建但首次会话丢失的半完成状态。
	 */
	@Transactional
	public SessionTokens create(long accountId, String label) {
		DeviceSession session = new DeviceSession();

		session.setAccountId(accountId);
		session.setLabel(label == null || label.isBlank() ? "浏览器" : label.substring(0, Math.min(label.length(), 80)));
		session.setRefreshUntil(Instant.now().plusSeconds(30L * 86400));
		SessionTokens tokens = rotate(session);
		sessions.save(session);
		return tokens;
	}

	/**
	 * 内部轮换操作，仅在新会话或已锁定的有效会话上调用。替换当前摘要与访问截止时间，但不延长设备绝对寿命。返回值包含明文令牌，不得记录日志。
	 */
	private SessionTokens rotate(DeviceSession session) {
		String access = token(), refresh = token();
		session.setAccessHash(TokenDigest.hash(access));
		session.setRefreshHash(TokenDigest.hash(refresh));
		session.setAccessUntil(Instant.now().plusSeconds(900));
		return new SessionTokens(access, refresh, 900);
	}

	/**
	 * 用 SecureRandom 产生32字节随机数据，编码为无填充 Base64URL。令牌不包含账号信息或权限声明，服务端必须查摘要核验。
	 */
	private String token() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** 必须在事务提交后由 HTTP 层处理 null，不能抛异常把安全撤销一起回滚。 */
	/**
	 * 以刷新摘要定位设备并取得同一行锁，等待后刷新 Hibernate 一级缓存再次比较。旧令牌重放会撤销设备；返回 null 让事务先提交，HTTP
	 * 层随后返回401，避免抛异常回滚安全撤销。
	 */
	@Transactional
	public SessionTokens refresh(String token) {
		String digest = TokenDigest.hash(token);
		Long found = sessions.idByRefreshHash(digest);
		if (found == null) {
			RefreshReceipt receipt = receipts.get(digest);
			if (receipt != null && receipt.getExpiresAt().isAfter(Instant.now())) {
				DeviceSession reused = sessions.lock(receipt.getSessionId());
				if (reused != null)
					reused.setRevoked(true);
			}
			return null;
		}
		DeviceSession locked = sessions.lock(found);
		// 查询可能先于另一事务完成，刷新一级缓存后再比较，防止旧实体绕过轮换检查。
		// 首次查询只读取主键，取得写锁时才装载实体，避免一级缓存中的旧摘要。
		if (!locked.getRefreshHash().equals(digest)) {
			locked.setRevoked(true);
			return null;
		}
		if (locked.isRevoked() || !locked.getRefreshUntil().isAfter(Instant.now()))
			return null;
		RefreshReceipt receipt = new RefreshReceipt();
		receipt.setTokenHash(digest);
		receipt.setSessionId(locked.getId());
		receipt.setExpiresAt(locked.getRefreshUntil());
		receipts.save(receipt);
		return rotate(locked);
	}

	/**
	 * 取得目标设备锁后检查所属账号。越权设备统一返回不存在，不能泄露他人设备信息。设置撤销标记后后续认证立即查库拒绝，重复撤销本账号设备不会签发新令牌。
	 */
	@Transactional
	public void revoke(SessionViewer viewer, long sessionId) {
		DeviceSession session = sessions.lock(sessionId);
		if (session == null || session.getAccountId() != viewer.accountId())
			throw new BusinessException(404, "设备会话不存在");
		session.setRevoked(true);
	}

}
