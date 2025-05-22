package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 抽象的数据集处理组件
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public abstract class AbstractDataSetShellComponent {

  public abstract String refreshData(Integer offset, Integer limit);

  /**
   * 保存所有数据
   *
   * @param resources 资源列表
   * @param converter 转换函数
   * @param repository 数据库仓库
   * @param nameExtractor 名称提取函数
   * @param <T> 实体类型
   * @return 处理结果字符串
   */
  protected <T> String saveSingleEntityData(
      List<NamedApiResource> resources,
      Function<NamedApiResource, T> converter,
      JpaRepository<T, ?> repository,
      Function<T, String> nameExtractor) {
    List<T> entities = resources.stream().map(converter).collect(Collectors.toList());
    repository.saveAllAndFlush(entities);
    return String.format(
        "数据处理完成，本次处理了数据:%s。\r\n当前共有数据:%d条",
        entities.stream().map(nameExtractor).collect(Collectors.joining(",")), repository.count());
  }

  /**
   * 保存所有数据（重载版本，支持返回多个实体）
   *
   * @param resources 资源列表
   * @param converter 转换函数，返回一个实体列表
   * @param repository 数据库仓库
   * @param nameExtractor 名称提取函数
   * @param <T> 实体类型
   * @return 处理结果字符串
   */
  protected <T> String saveMultipleEntityData(
      List<NamedApiResource> resources,
      Function<NamedApiResource, List<T>> converter,
      JpaRepository<T, ?> repository,
      Function<T, String> nameExtractor) {
    // 扁平化所有转换后的实体列表
    List<T> entities =
        resources.stream()
            .flatMap(resource -> converter.apply(resource).stream()) // 将每个 NamedApiResource 转换为多个实体
            .collect(Collectors.toList());

    // 保存所有实体
    repository.saveAllAndFlush(entities);

    // 格式化返回的处理结果
    return String.format(
        "数据处理完成，本次处理了数据:%s。\r\n当前共有数据:%d条",
        entities.stream().map(nameExtractor).collect(Collectors.joining(",")), repository.count());
  }
}
