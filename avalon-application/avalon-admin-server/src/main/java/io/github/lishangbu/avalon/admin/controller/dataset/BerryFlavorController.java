package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 树果风味控制器
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@RestController
@RequestMapping("/berry-flavor")
@RequiredArgsConstructor
public class BerryFlavorController {
  private final BerryFlavorService berryFlavorService;

  @PostMapping("/import")
  public List<BerryFlavor> importBerryFlavors() {
    return berryFlavorService.importBerryFlavors();
  }
}
