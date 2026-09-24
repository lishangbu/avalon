package io.github.lishangbu.avalon.identity.application.query;

import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 经过密码认证后查询内部账号标识；不向浏览器暴露账号实体或密码摘要。
 */
@Component
@RequiredArgsConstructor
public class AccountQueryService {

	private final AccountRepository accounts;

	/**
	 * 用于已通过密码校验或注册测试后的内部账号定位。调用方必须保证账号存在；此方法不是公开账号枚举接口，响应不包含密码摘要。
	 */
	@Transactional
	public long accountId(String username) {
		return accounts.byUsername(username).getId();
	}

}
