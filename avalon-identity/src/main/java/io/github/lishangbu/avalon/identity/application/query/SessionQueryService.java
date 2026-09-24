package io.github.lishangbu.avalon.identity.application.query;

import io.github.lishangbu.avalon.identity.application.model.DeviceView;
import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.identity.domain.TokenDigest;
import io.github.lishangbu.avalon.identity.persistence.entity.Account;
import io.github.lishangbu.avalon.identity.persistence.entity.DeviceSession;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import io.github.lishangbu.avalon.identity.persistence.repository.SessionRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 会话查询不签发令牌、不旋转令牌、不撤销设备；认证每次核对数据库失效状态。 设备列表是单条普通 SELECT。认证读取会话后再取账号角色，账号权限变更即时作用于后续请求。
 */
@Component
@RequiredArgsConstructor
public class SessionQueryService {

	private final SessionRepository sessions;

	private final AccountRepository accounts;

	/**
	 * 只读核验访问令牌摘要、撤销状态、访问截止与绝对截止，随后获取账号角色构建内部身份。无效会话返回 null，由安全适配器转换认证失败；查询不延长会话或更新最后访问时间。
	 */
	@Transactional
	public SessionViewer authenticate(String token) {
		DeviceSession session = sessions.byAccessHash(TokenDigest.hash(token));
		if (session == null || !session.getAccessUntil().isAfter(Instant.now())
				|| !session.getRefreshUntil().isAfter(Instant.now()))
			return null;
		Account account = accounts.get(session.getAccountId());
		if (account == null)
			return null;
		Set<String> roles = new HashSet<>(Arrays.asList(account.getRoles().split(",")));
		return new SessionViewer(session.getAccountId(), session.getId(), Set.copyOf(roles));
	}

	/**
	 * 按身份上下文限定账号，只列未撤销且绝对期限未到的设备。当前设备标记由已验证的 sessionId 比较产生，查询结果不包含任何令牌或摘要。
	 */
	@Transactional
	public List<DeviceView> list(SessionViewer viewer) {
		return sessions.active(viewer.accountId(), Instant.now())
			.stream()
			.map(s -> new DeviceView(s.getId().toString(), s.getLabel(), s.getId() == viewer.sessionId()))
			.toList();
	}

}
