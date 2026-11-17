package io.github.lishangbu.avalon.identity.security;

import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** bcrypt 兼容既有摘要格式；密码核验不占用业务写事务。 */
@Configuration
public class PasswordAuthenticationConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(AccountRepository accounts) {
		return username -> {
			var account = accounts.byUsername(username);
			if (account == null)
				throw new UsernameNotFoundException("账号或密码不正确");
			return User.withUsername(account.getUsername())
				.password(account.getPasswordHash())
				.authorities(account.getRoles().split(","))
				.build();
		};
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwords) {
		var provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(passwords);
		return new ProviderManager(provider);
	}

}
