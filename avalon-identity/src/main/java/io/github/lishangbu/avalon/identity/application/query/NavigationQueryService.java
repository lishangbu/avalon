package io.github.lishangbu.avalon.identity.application.query;

import io.github.lishangbu.avalon.identity.domain.NavigationRules;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

/** 账号来自已认证身份，不接受客户端指定其他账号。仅返回导航标识。 */
@Component
@RequiredArgsConstructor
public class NavigationQueryService {

	private final AccountRepository accounts;

	@Transactional
	public List<String> read(long owner) {
		return NavigationRules.restore(accounts.get(owner).getNavigation());
	}

}
