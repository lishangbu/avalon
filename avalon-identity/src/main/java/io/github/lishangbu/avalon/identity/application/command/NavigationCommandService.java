package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.domain.NavigationRules;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 整组覆盖是幂等操作；账号行锁保证并发保存不会产生混合位置，最后提交者生效。 */
@Component
@RequiredArgsConstructor
public class NavigationCommandService {

	private final AccountRepository accounts;

	@Transactional
	public List<String> save(long owner, List<String> slots) {
		List<String> valid = NavigationRules.validate(slots);
		accounts.lock(owner).setNavigation(String.join(",", valid));
		return valid;
	}

}
