package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.mapstruct.TypeMapstruct;
import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.mapper.TypeMapper;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 属性服务实现
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@Service
@RequiredArgsConstructor
public class TypeServiceImpl implements TypeService {
  private final TypeMapper typeMapper;
  private final PokeApiService pokeApiService;
  private final TypeMapstruct typeMapstruct;

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
        typeMapstruct::toDatasetType,
        typeMapper::insert,
        io.github.lishangbu.avalon.pokeapi.model.pokemon.Type.class);
  }

  /**
   * 根据条件分页查询属性类型，结果按ID升序排序
   *
   * @param page 分页参数
   * @param type 查询条件实体，非空字段将作为过滤条件
   * @return 分页结果，按ID升序排序
   */
  @Override
  public IPage<Type> getTypePage(Page<Type> page, Type type) {
    return typeMapper.selectList(page, type);
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
    typeMapper.insert(type);
    return type;
  }

  /**
   * 根据主键删除属性类型
   *
   * @param id 属性类型主键
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeById(Integer id) {
    typeMapper.deleteById(id);
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
    typeMapper.updateById(type);
    return type;
  }

  /**
   * 根据条件查询属性类型列表
   *
   * <p>支持按 name/internalName 模糊查询，其余字段精确匹配
   *
   * @param type 查询条件，支持部分字段模糊查询
   * @return 属性类型列表
   */
  @Override
  public List<Type> listByCondition(Type type) {
    return typeMapper.selectList(type);
  }
}
