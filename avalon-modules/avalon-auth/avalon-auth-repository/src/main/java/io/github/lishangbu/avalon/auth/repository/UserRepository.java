package io.github.lishangbu.avalon.auth.repository;

import io.github.lishangbu.avalon.auth.entity.User;
import io.github.lishangbu.avalon.auth.model.UserDTO;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据存储
 *
 * @author lishangbu
 * @since 2025/4/5
 */
@Repository
public interface UserRepository extends ListPagingAndSortingRepository<User, Long> {

  @Query(
      """
       SELECT u.id, u.username, u.password, GROUP_CONCAT(r.code) AS roleCodes
                FROM `USER` u
                LEFT JOIN USER_ROLE_RELATION urr ON u.id = urr.user_id
                LEFT JOIN ROLE r ON urr.role_id = r.id
                WHERE u.username = :username
                GROUP BY u.id, u.username, u.password
      """)
  Optional<UserDTO> findByUsername(String username);

  boolean existsByUsername(String username);
}
