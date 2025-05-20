package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiTemplate;
import io.github.lishangbu.avalon.shell.dataset.constant.DataSetConstants;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * 属性相关数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
@ShellCommandGroup(DataSetConstants.DATA_SET_SHELL_GROUP)
public class TypeDataSetShellComponent {

  private final PokeApiTemplate pokeApiTemplate;

  private final TypeRepository typeRepository;

  public TypeDataSetShellComponent(PokeApiTemplate pokeApiTemplate, TypeRepository typeRepository) {
    this.pokeApiTemplate = pokeApiTemplate;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "refresh-db-type", value = "刷新数据库中的TYPE表数据")
  public String refreshType() {
    NamedAPIResourceList namedApiResources = pokeApiTemplate.listTypes(0, 100);

    for (NamedApiResource namedApiResource : namedApiResources.results()) {
      io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type result =
          pokeApiTemplate.getType(namedApiResource.name());
      Type type = new Type();
      type.setId(result.id());
      type.setInternalName(result.name());
      result.names().stream()
          .filter(name -> name.language().name().equals("zh-Hans"))
          .findFirst()
          .ifPresentOrElse(
              name -> type.setName(name.name()),
              () -> {
                result.names().stream()
                    .filter(name -> name.language().name().equals("zh-Hant"))
                    .findFirst()
                    .ifPresentOrElse(
                        name -> type.setName(name.name()),
                        () -> type.setName(type.getInternalName()));
              });

      typeRepository.save(type);
    }

    return "数据处理完成，当前共有数据:" + typeRepository.count() + "条";
  }
}
