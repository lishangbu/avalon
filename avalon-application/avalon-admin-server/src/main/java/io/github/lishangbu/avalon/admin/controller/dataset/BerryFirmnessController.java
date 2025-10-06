package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 树果坚硬度控制器
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@RestController
@RequestMapping("/berry-firmness")
@RequiredArgsConstructor
public class BerryFirmnessController {
  private final BerryFirmnessService berryFirmnessService;

  @PostMapping("/import")
  public List<BerryFirmness> importBerryFirmnesses() {
    return berryFirmnessService.importBerryFirmnesses();
  }
}
