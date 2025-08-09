package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 属性数据异常处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class TypeDatasetParseStrategy implements BasicDataSetParseStrategy {
  private final TypeRepository typeRepository;

  public TypeDatasetParseStrategy(TypeRepository typeRepository) {
    this.typeRepository = typeRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource instanceof io.github.lishangbu.avalon.pokeapi.model.pokemon.Type typeData) {
      Type type = new Type();
      type.setId(typeData.id());
      type.setInternalName(typeData.name());
      Optional<Name> localizationName = LocalizationUtils.getLocalizationName(typeData.names());
      type.setName(localizationName.map(Name::name).orElse(type.getInternalName()));
      return type;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.TYPE;
  }

  @Override
  public JpaRepository getRepository() {
    return this.typeRepository;
  }
}
