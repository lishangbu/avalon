package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 基础数据解析策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
public interface BasicDataSetParseStrategy {

  Object convertToEntity(Object singleResource);

  PokeApiDataTypeEnum getDataType();

  JpaRepository getRepository();
}
