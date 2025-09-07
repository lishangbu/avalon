package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户信息(user)表数据库访问层
 *
 * <p>提供对用户信息的增删改查等操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  /**
   * 通过用户名查询用户信息
   *
   * @param username 用户名
   * @return 用户信息
   */
  @Query(
      """
          select distinct u from User u
          left join fetch u.roles
          where u.username = :username
      """)
  Optional<User> findUserWithRolesByUsername(@Param("username") String username);
}
