package io.github.lishangbu.avalon.auth.repository;

import io.github.lishangbu.avalon.auth.entity.Role;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据存储
 *
 * @author lishangbu
 * @since 2025/4/5
 */
@Repository
public interface RoleRepository
    extends ListCrudRepository<Role, Long>, ListPagingAndSortingRepository<Role, Long> {

  Optional<Role> findByCode(String code);
}
