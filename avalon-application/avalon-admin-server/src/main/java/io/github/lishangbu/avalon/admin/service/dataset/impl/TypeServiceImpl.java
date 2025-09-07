package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type save(Type type) {
    return null;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteById(Long id) {}

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Type update(Type type) {
    return null;
  }

  @Override
  public Type findById(Long id) {
    return null;
  }
}
