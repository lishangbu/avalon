package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.TYPE;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.Optional;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 属性相关数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
public class TypeDataSetShellComponent extends AbstractDataSetShellComponent {

  private final PokeApiFactory pokeApiFactory;

  private final TypeRepository typeRepository;

  public TypeDataSetShellComponent(PokeApiFactory pokeApiFactory, TypeRepository typeRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.typeRepository = typeRepository;
  }

  @Override
  @ShellMethod(key = "dataset refresh type", value = "刷新数据库中的属性表数据")
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(TYPE);
    return this.saveEntityData(
        namedApiResources.results(), this::convertToType, typeRepository, Type::getName);
  }

  private Type convertToType(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.pokemon.Type result =
        pokeApiFactory.getSingleResource(TYPE, NamedApiResourceUtils.getId(namedApiResource));
    Type type = new Type();
    type.setId(result.id());
    type.setInternalName(result.name());
    Optional<Name> localizationName = LocalizationUtils.getLocalizationName(result.names());
    type.setName(localizationName.map(Name::name).orElse(type.getInternalName()));
    return type;
  }
}
