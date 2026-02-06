package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.Type_;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 属性服务实现
///
/// @author lishangbu
/// @since 2025/8/24
@Service
@RequiredArgsConstructor
public class TypeServiceImpl implements TypeService {
  private final TypeRepository typeRepository;

  /// 根据条件分页查询属性类型，结果按ID升序排序
  ///
  /// @param type 查询条件实体，非空字段将作为过滤条件
  /// @param pageable 分页参数
  /// @return 分页结果，按ID升序排序
  @Override
  public Page<Type> getPageByCondition(Type type, Pageable pageable) {
    return typeRepository.findAll(
        Example.of(
            type,
            ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withMatcher(Type_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher(
                    Type_.INTERNAL_NAME, ExampleMatcher.GenericPropertyMatchers.contains())),
        pageable);
  }

  /// 保存属性类型
  ///
  /// @param type 属性类型实体
  /// @return 保存后的属性类型
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type save(Type type) {
    return typeRepository.save(type);
  }

  /// 根据主键删除属性类型
  ///
  /// @param id 属性类型主键
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeById(Long id) {
    typeRepository.deleteById(id);
  }

  /// 更新属性类型
  ///
  /// @param type 属性类型实体
  /// @return 更新后的属性类型
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type update(Type type) {
    return typeRepository.save(type);
  }

  /// 根据条件查询属性类型列表
  ///
  /// <p>支持按 name/internalName 模糊查询，其余字段精确匹配
  ///
  /// @param type 查询条件，支持部分字段模糊查询
  /// @return 属性类型列表
  @Override
  public List<Type> listByCondition(Type type) {
    ExampleMatcher matcher =
        ExampleMatcher.matching()
            .withIgnoreNullValues()
            .withMatcher(Type_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
            .withMatcher(Type_.INTERNAL_NAME, ExampleMatcher.GenericPropertyMatchers.contains());
    return typeRepository.findAll(Example.of(type, matcher));
  }
}
