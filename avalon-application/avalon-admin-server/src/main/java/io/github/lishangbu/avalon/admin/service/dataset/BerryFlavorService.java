package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import java.util.List;

/**
 * 树果风味服务
 *
 * <p>风味决定了宝可梦根据<a href="https://pokeapi.co/docs/v2#natures">性格</a>食用树果时是受益还是受损。详情可参考<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Flavor">Bulbapedia</a>
 *
 * @author lishangbu
 * @since 2025/10/5
 */
public interface BerryFlavorService {

  /**
   * 导入树果风味列表
   *
   * @return 树果风味列表
   */
  List<BerryFlavor> importBerryFlavors();
}
