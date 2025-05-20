package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.shell.dataset.constant.DataSetConstants;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiResource;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiTypeDetailResult;
import io.github.lishangbu.avalon.shell.pokeapi.service.PokeApiTypeService;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

/**
 * 属性相关数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
@ShellCommandGroup(DataSetConstants.DATA_SET_SHELL_GROUP)
public class TypeDataSetShellComponent {

  private final PokeApiTypeService pokeApiTypeService;

  private final TypeRepository typeRepository;


  public TypeDataSetShellComponent(PokeApiTypeService pokeApiTypeService, TypeRepository typeRepository) {
    this.pokeApiTypeService = pokeApiTypeService;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "dataset-refresh-type", value = "刷新数据库中的TYPE表数据")
  public String refreshType() {
    List<PokeApiResource> pokeApiResources = pokeApiTypeService.listPokeApiTypes();
    for (PokeApiResource pokeApiResource : pokeApiResources) {
      PokeApiTypeDetailResult result = pokeApiTypeService.getPokeApiTypeDetailByArg(pokeApiResource.name());
      Type type = new Type();
      type.setId(result.id());
      type.setInternalName(result.name());
      result.names().stream()
        .filter(name -> name.language().name().equals("zh-Hans"))
        .findFirst()
        .ifPresentOrElse(name -> type.setName(name.name()), () -> {
          result.names().stream()
            .filter(name -> name.language().name().equals("zh-Hant"))
            .findFirst()
            .ifPresentOrElse(name -> type.setName(name.name()), () -> type.setName(type.getInternalName()));
        });

      typeRepository.save(type);
    }

    return "数据处理完成，当前共有数据:" + typeRepository.count() + "条";
  }
}
