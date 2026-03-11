package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 属性服务。
public interface TypeService {

    /// 根据条件分页查询属性。
    Page<Type> getPageByCondition(Type type, Pageable pageable);

    /// 新增属性。
    Type save(Type type);

    /// 更新属性。
    Type update(Type type);

    /// 根据主键删除属性。
    void removeById(Long id);

    /// 根据条件查询属性列表。
    List<Type> listByCondition(Type type);
}
