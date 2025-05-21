package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * 属性相关数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
@ShellCommandGroup("数据集处理")
public class TypeDataSetShellComponent {

  private final PokeApiService pokeApiService;

  private final TypeRepository typeRepository;

  public TypeDataSetShellComponent(PokeApiService pokeApiService, TypeRepository typeRepository) {
    this.pokeApiService = pokeApiService;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "dataset refresh type", value = "刷新数据库中的TYPE表数据")
  public String refreshType(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listTypes(offset, limit);
    List<io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type> loadedTypes =
        new ArrayList<>();
    for (NamedApiResource namedApiResource : namedApiResources.results()) {
      io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type result =
          pokeApiService.getType(namedApiResource.name());
      loadedTypes.add(result);
      Type type = new Type();
      type.setId(result.id());
      type.setInternalName(result.name());
      // 星晶属性没有简体中文，所以特殊处理一下多取一个繁体中文
      Optional<Name> localizationName =
          LocalizationUtils.getLocalizationName(
              result.names(),
              LocalizationUtils.SIMPLIFIED_CHINESE,
              LocalizationUtils.TRADITIONAL_CHINESE);
      type.setName(
          localizationName.isPresent() ? localizationName.get().name() : type.getInternalName());
      typeRepository.save(type);
    }

    return String.format(
        "数据处理完成，本次处理从API接口获取了数据:%s。\r\n当前共有数据:%d条",
        loadedTypes.stream()
            .map(io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type::name)
            .collect(Collectors.joining(",")),
        typeRepository.count());
  }
}
