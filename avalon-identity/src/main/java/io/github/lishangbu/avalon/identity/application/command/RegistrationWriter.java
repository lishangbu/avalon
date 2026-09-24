package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.application.model.SessionTokens;
import io.github.lishangbu.avalon.identity.persistence.entity.Account;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 独立 Spring 事务入口：账号和首次会话要么共同提交，要么共同回滚。
 */
@Component
@RequiredArgsConstructor
public class RegistrationWriter {

	private final AccountRepository accounts;

	private final SessionCommandService sessions;

	/**
	 * 短事务写入口。查询重名后插入账号并创建首次会话；查询与写入间仍可能竞争，因此数据库唯一约束不能移除。任何首次会话失败都要回滚账号，以允许用户重试注册。
	 */
	@Transactional
	public SessionTokens create(String username, String passwordHash, String label) {
		if (accounts.byUsername(username) != null)
			throw new BusinessException(409, "账号已存在");
		Account account = new Account();

		account.setUsername(username);
		account.setPasswordHash(passwordHash);
		account.setRoles("player");
		// 查重不能替代唯一约束：并发注册最终由数据库决定胜者。
		accounts.saveAndFlush(account);
		return sessions.create(account.getId(), label);
	}

}
