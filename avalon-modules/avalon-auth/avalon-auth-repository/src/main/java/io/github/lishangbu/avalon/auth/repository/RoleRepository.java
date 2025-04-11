package io.github.lishangbu.avalon.auth.repository;

import io.github.lishangbu.avalon.auth.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据存储
 *
 * @author lishangbu
 * @since 2025/4/5
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByCode(String code);
}
