package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Role;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 角色信息(role)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Repository
public interface RoleRepository
    extends ListCrudRepository<Role, Integer>, ListPagingAndSortingRepository<Role, Integer> {}
