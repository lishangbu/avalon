package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 招式伤害类别控制器
///
/// @author lishangbu
/// @since 2026/3/19
@RestController
@RequestMapping("/move-damage-class")
@RequiredArgsConstructor
public class MoveDamageClassController {
    private final MoveDamageClassService moveDamageClassService;

    /// 分页条件查询招式伤害类别
    ///
    /// @param pageable        分页参数
    /// @param moveDamageClass 查询条件，支持 name/internalName 模糊查询
    /// @return 招式伤害类别分页结果
    @GetMapping("/page")
    public Page<MoveDamageClass> getMoveDamageClassPage(
            Pageable pageable, MoveDamageClass moveDamageClass) {
        return moveDamageClassService.getPageByCondition(moveDamageClass, pageable);
    }

    /// 新增招式伤害类别
    ///
    /// @param moveDamageClass 招式伤害类别实体
    /// @return 保存后的招式伤害类别
    @PostMapping
    public MoveDamageClass save(@RequestBody MoveDamageClass moveDamageClass) {
        return moveDamageClassService.save(moveDamageClass);
    }

    /// 更新招式伤害类别
    ///
    /// @param moveDamageClass 招式伤害类别实体
    /// @return 更新后的招式伤害类别
    @PutMapping
    public MoveDamageClass update(@RequestBody MoveDamageClass moveDamageClass) {
        return moveDamageClassService.update(moveDamageClass);
    }

    /// 根据 ID 删除招式伤害类别
    ///
    /// @param id 招式伤害类别主键
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        moveDamageClassService.removeById(id);
    }

    /// 条件查询招式伤害类别列表
    ///
    /// @param moveDamageClass 查询条件，支持 name/internalName 模糊查询
    /// @return 招式伤害类别列表
    @GetMapping("/list")
    public List<MoveDamageClass> listMoveDamageClasses(MoveDamageClass moveDamageClass) {
        return moveDamageClassService.listByCondition(moveDamageClass);
    }
}
