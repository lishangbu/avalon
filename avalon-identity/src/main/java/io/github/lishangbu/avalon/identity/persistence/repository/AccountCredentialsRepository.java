package io.github.lishangbu.avalon.identity.persistence.repository;

import io.github.lishangbu.avalon.identity.persistence.entity.AccountCredentials;
import org.springframework.data.repository.Repository;

/** 认证只能读取视图，不提供保存或删除凭据的方法。 */
public interface AccountCredentialsRepository extends Repository<AccountCredentials, Long> {

	AccountCredentials findByUsername(String username);

}
