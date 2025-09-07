package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 权限(permission)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/28
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /**
   * 通过角色代码查询所有权限
   *
   * <ol>
   *   <li>如果 roleCodes 为空，返回空结果，避免误返回所有权限（需在 Service 层判断）
   *   <li>使用 DISTINCT 去重，避免 JOIN 导致重复权限记录
   *   <li>使用实体关系 join，避免直接依赖中间表
   *   <li>保持与原有 mapper 方法签名兼容（collection="roleCodes" 保持不变）
   * </ol>
   *
   * @return 权限列表
   */
  @Query(
      """
      select distinct p from Permission p
      join p.roles r
      where r.code in :roleCodes
      """)
  List<Permission> findAllByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
