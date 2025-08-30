package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

/**
 * 权限(permission)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/28
 */
public interface PermissionMapper {

  /**
   * 通过id查询单条权限数据
   *
   * @param id 主键
   * @return 可选的权限
   */
  Optional<Permission> selectById(Long id);

  /**
   * 新增权限
   *
   * @param permission 实例对象
   * @return 影响行数
   */
  int insert(Permission permission);

  /**
   * 修改权限
   *
   * @param permission 实例对象
   * @return 影响行数
   */
  int updateById(Permission permission);

  /**
   * 通过id删除权限
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);

  /**
   * 查询所有权限
   *
   * @return 权限列表
   */
  List<Permission> selectAll(Permission permission);

  /**
   * 通过角色代码查询所有权限
   *
   * <ol>
   *   <li>如果 roleCodes 为空，返回空结果，避免误返回所有权限
   *   <li>使用 DISTINCT 去重，避免 JOIN 导致重复权限记录
   *   <li>使用 INNER JOIN 提高语义，只有存在角色关联时才返回权限
   *   <li>保持与原有 mapper 方法签名兼容（collection="roleCodes" 保持不变）
   * </ol>
   *
   * @return 权限列表
   */
  List<Permission> selectAllByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
