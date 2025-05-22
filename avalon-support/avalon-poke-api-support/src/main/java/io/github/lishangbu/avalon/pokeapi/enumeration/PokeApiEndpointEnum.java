package io.github.lishangbu.avalon.pokeapi.enumeration;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;

/**
 * Poke Api 端点枚举
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public enum PokeApiEndpointEnum {
  /** 属性 */
  BERRY("berry", Berry.class),

  BERRY_FIRMNESS("berry-firmness", BerryFirmness.class),

  /** 属性 */
  TYPE("type", Type.class);

  /** POKE API接口访问URI */
  private final String uri;

  /** 返回的资源映射到对应的Class上 */
  private final Class responseType;

  PokeApiEndpointEnum(String uri, Class responseType) {
    this.uri = uri;
    this.responseType = responseType;
  }

  public String getUri() {
    return uri;
  }

  public Class getResponseType() {
    return responseType;
  }
}
