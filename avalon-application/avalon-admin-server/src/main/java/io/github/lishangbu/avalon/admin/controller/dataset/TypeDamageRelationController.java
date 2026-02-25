package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 属性克制关系控制器
///
/// 提供属性克制关系的 REST API，直接通过 Service/Repository 操作数据
///
/// @author lishangbu
/// @since 2025/12/06
@RestController
@RequestMapping("/type-damage-relation")
@RequiredArgsConstructor
public class TypeDamageRelationController {
    private final TypeDamageRelationService typeDamageRelationService;
}
