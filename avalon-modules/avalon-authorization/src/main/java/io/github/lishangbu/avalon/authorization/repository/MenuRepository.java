package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 菜单存储
 *
 * @author lishangbu
 * @since 2025/9/19
 */
@Repository
public interface MenuRepository
    extends ListCrudRepository<Menu, Long>, ListPagingAndSortingRepository<Menu, Long> {
  /**
   * 通过角色代码查询所有菜单
   *
   * <ol>
   *   <li>使用 DISTINCT 去重，避免 JOIN 导致重复权限记录
   *   <li>使用实体关系 join，避免直接依赖中间表
   * </ol>
   *
   * @return 菜单列表
   */
  @Query(
      """
      select distinct m from Menu m
      join m.roles r
      where r.code in :roleCodes
      order by m.sortOrder desc
      """)
  List<Menu> findAllByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
