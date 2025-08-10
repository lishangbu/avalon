package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import io.github.lishangbu.avalon.shell.dataset.component.strategy.BasicDataSetParseStrategy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抽象的数据集处理组件
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@ShellComponent
public class DataSetShellComponent {
  private final JdbcAggregateTemplate jdbcAggregateTemplate;
  private final PokeApiService pokeApiService;
  private final Map<PokeApiDataTypeEnum, BasicDataSetParseStrategy> basicDataSetParseStrategyMap;

  public DataSetShellComponent(
      JdbcAggregateTemplate jdbcAggregateTemplate,
      PokeApiService pokeApiService,
      List<BasicDataSetParseStrategy> basicDataSetParseStrategies) {
    this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    this.pokeApiService = pokeApiService;
    this.basicDataSetParseStrategyMap = new ConcurrentHashMap<>(basicDataSetParseStrategies.size());
    basicDataSetParseStrategies.forEach(
        (basicDataSetParseStrategy) -> {
          basicDataSetParseStrategyMap.put(
              basicDataSetParseStrategy.getDataType(), basicDataSetParseStrategy);
        });
  }

  /**
   * 刷新数据
   *
   * @return 刷新数据的结果
   */
  @ShellMethod(key = "dataset refresh", value = "刷新数据库的基础数据")
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "要刷新的数据类型", valueProvider = PokeApiDataTypeNameProvider.class)
          String type) {
    PokeApiDataTypeEnum apiDataTypeEnum = PokeApiDataTypeEnum.getDataTypeByTypeName(type);
    NamedAPIResourceList namedApiResources = pokeApiService.listNamedAPIResources(apiDataTypeEnum);
    BasicDataSetParseStrategy basicDataSetParseStrategy =
        basicDataSetParseStrategyMap.get(apiDataTypeEnum);
    if (basicDataSetParseStrategy == null) {
      return "不支持处理数据:" + type;
    }
    jdbcAggregateTemplate.insertAll(
        namedApiResources.results().stream()
            .map(
                namedApiResource ->
                    basicDataSetParseStrategy.convertToEntity(
                        pokeApiService.getEntityFromUri(
                            apiDataTypeEnum, NamedApiResourceUtils.getId(namedApiResource))))
            .filter(Objects::nonNull)
            .toList());
    return String.format(
        "数据处理完成，本次处理了数据:%s",
        namedApiResources.results().stream()
            .map(NamedApiResource::name)
            .collect(Collectors.joining(",")));
  }
}
