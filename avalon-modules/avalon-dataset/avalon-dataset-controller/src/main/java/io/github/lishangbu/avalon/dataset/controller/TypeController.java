package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.service.TypeService;
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

/// 属性控制器
///
/// @author lishangbu
/// @since 2026/3/11
@RestController
@RequestMapping("/type")
@RequiredArgsConstructor
public class TypeController {
    private final TypeService typeService;

    /// 分页条件查询属性
    ///
    /// @param pageable 分页参数
    /// @param type     查询条件，支持 name/internalName 模糊查询
    /// @return 属性分页结果
    @GetMapping("/page")
    public Page<Type> getTypePage(Pageable pageable, Type type) {
        return typeService.getPageByCondition(type, pageable);
    }

    /// 新增属性
    ///
    /// @param type 属性实体
    /// @return 保存后的属性
    @PostMapping
    public Type save(@RequestBody Type type) {
        return typeService.save(type);
    }

    /// 更新属性
    ///
    /// @param type 属性实体
    /// @return 更新后的属性
    @PutMapping
    public Type update(@RequestBody Type type) {
        return typeService.update(type);
    }

    /// 根据 ID 删除属性
    ///
    /// @param id 属性主键
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        typeService.removeById(id);
    }

    /// 条件查询属性列表
    ///
    /// @param type 查询条件，支持 name/internalName 模糊查询
    /// @return 属性列表
    @GetMapping("/list")
    public List<Type> listTypes(Type type) {
        return typeService.listByCondition(type);
    }
}
