package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/// 菜单表数据存储
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2025/08/20
@Repository
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {
    /// 根据角色代码列表查询对应的菜单集合
    ///
    /// @param roleCodes 角色代码列表
    /// @return 匹配的菜单列表
    @Query(
            """
                    select distinct m from Role r
                    join r.menus m
                    where r.code in :roleCodes
                    order by m.sortingOrder desc
                    """)
    List<Menu> findAllByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
