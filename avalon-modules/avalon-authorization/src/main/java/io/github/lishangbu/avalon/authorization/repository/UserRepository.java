package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.User;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
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
public interface UserRepository
    extends ListCrudRepository<User, Integer>, ListPagingAndSortingRepository<User, Integer> {
  /**
   * 根据用户名查询用户信息
   *
   * <p>使用派生查询，Spring Data JDBC 会在返回 User 时自动填充由 {@code @MappedCollection} 关联的集合字段
   *
   * @param username 用户名，不能为空
   * @return 包含关联集合的用户信息（若不存在返回空）
   */
  Optional<User> findByUsername(String username);
}
