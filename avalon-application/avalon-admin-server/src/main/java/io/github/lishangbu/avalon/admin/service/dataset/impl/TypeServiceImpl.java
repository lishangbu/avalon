package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.Type_;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 属性服务实现
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@Service
@RequiredArgsConstructor
public class TypeServiceImpl implements TypeService {
  private final TypeRepository typeRepository;
  private final PokeApiService pokeApiService;

  /**
   * 导入属性类型数据
   *
   * <p>通过 PokeApiService 拉取并保存属性类型数据。
   *
   * @return 导入后的属性类型列表
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<Type> importTypes() {

    return pokeApiService.importData(
        PokeDataTypeEnum.TYPE,
        typeData -> {
          Type type = new Type();
          type.setInternalName(typeData.name());
          type.setId(typeData.id().longValue());
          type.setName(typeData.name());
          LocalizationUtils.getLocalizationName(typeData.names())
              .ifPresent(name -> type.setName(name.name()));
          return type;
        },
        typeRepository::save,
        io.github.lishangbu.avalon.pokeapi.model.pokemon.Type.class);
  }

  /**
   * 根据条件分页查询属性类型，结果按ID升序排序
   *
   * @param type 查询条件实体，非空字段将作为过滤条件
   * @param pageable 分页参数
   * @return 分页结果，按ID升序排序
   */
  @Override
  public Page<Type> getPageByCondition(Type type, Pageable pageable) {
    Specification<Type> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (type.getName() != null) {
            predicates.add(cb.like(root.get(Type_.NAME), String.format("%%%s%%", type.getName())));
          }
          if (type.getInternalName() != null) {
            predicates.add(
                cb.like(
                    root.get(Type_.INTERNAL_NAME),
                    String.format("%%%s%%", type.getInternalName())));
          }
          // 按ID升序排序
          query.orderBy(cb.asc(root.get(Type_.ID)));
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return typeRepository.findAll(spec, pageable);
  }

  /**
   * 保存属性类型
   *
   * @param type 属性类型实体
   * @return 保存后的属性类型
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type save(Type type) {
    return typeRepository.save(type);
  }

  /**
   * 根据主键删除属性类型
   *
   * @param id 属性类型主键
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeById(Long id) {
    typeRepository.deleteById(id);
  }

  /**
   * 更新属性类型
   *
   * @param type 属性类型实体
   * @return 更新后的属性类型
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type update(Type type) {
    return typeRepository.save(type);
  }
}
