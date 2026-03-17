package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 招式伤害类别服务。
public interface MoveDamageClassService {

    /// 根据条件分页查询招式伤害类别。
    Page<MoveDamageClass> getPageByCondition(MoveDamageClass moveDamageClass, Pageable pageable);

    /// 新增招式伤害类别。
    MoveDamageClass save(MoveDamageClass moveDamageClass);

    /// 更新招式伤害类别。
    MoveDamageClass update(MoveDamageClass moveDamageClass);

    /// 根据主键删除招式伤害类别。
    void removeById(Long id);

    /// 根据条件查询招式伤害类别列表。
    List<MoveDamageClass> listByCondition(MoveDamageClass moveDamageClass);
}
