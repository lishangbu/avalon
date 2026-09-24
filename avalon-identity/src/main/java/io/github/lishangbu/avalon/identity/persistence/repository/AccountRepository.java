package io.github.lishangbu.avalon.identity.persistence.repository;

import io.github.lishangbu.avalon.identity.persistence.entity.Account;

import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/**
 * 账号读取入口，唯一性同时由数据库保护，不能只依赖预先查询。
 */
public interface AccountRepository extends EntityRepository<Account, Long> {

	@Query("select e from Account e where username = ?1")
	Account byUsername(String username);

}
