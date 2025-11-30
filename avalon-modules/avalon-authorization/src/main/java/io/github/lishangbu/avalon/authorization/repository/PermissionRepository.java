package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 权限(permission)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/28
 */
@Repository
public interface PermissionRepository
    extends ListCrudRepository<Permission, Long>, ListPagingAndSortingRepository<Permission, Long> {

  /**
   * 通过角色代码查询所有权限
   *
   * <ol>
   *   <li>使用 DISTINCT 去重，避免 JOIN 导致重复权限记录
   *   <li>使用实体关系 join，避免直接依赖中间表
   * </ol>
   *
   * @return 权限列表
   */
  @Query(
      """
      select distinct p from Permission p
      join p.roles r
      where r.code in :roleCodes
        and p.enabled is true
      """)
  List<Permission> findAllEnabledPermissionsByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
