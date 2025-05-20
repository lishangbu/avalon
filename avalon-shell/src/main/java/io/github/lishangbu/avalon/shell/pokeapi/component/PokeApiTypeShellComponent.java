package io.github.lishangbu.avalon.shell.pokeapi.component;

import io.github.lishangbu.avalon.json.util.JsonUtils;
import io.github.lishangbu.avalon.shell.pokeapi.constant.PokeApiConstants;
import io.github.lishangbu.avalon.shell.pokeapi.service.PokeApiTypeService;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * 属性相关调用poke-api命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
@ShellCommandGroup(PokeApiConstants.POKE_API_SHELL_GROUP)
public class PokeApiTypeShellComponent {

  private final PokeApiTypeService pokeApiTypeService;

  public PokeApiTypeShellComponent(PokeApiTypeService pokeApiTypeService) {
    this.pokeApiTypeService = pokeApiTypeService;
  }

  @ShellMethod(key = "poke-api-print-types", value = "输出poke-api网站的属性数据")
  public String printPokeApiType() {
    return JsonUtils.toPrettyJson(pokeApiTypeService.listPokeApiTypes());
  }

  @ShellMethod(key = "poke-api-print-type", value = "输出poke-api网站的属性数据")
  public String printPokeApiTypeDetail(String arg) {
    return JsonUtils.toPrettyJson(pokeApiTypeService.getPokeApiTypeDetailByArg(arg));
  }
}
