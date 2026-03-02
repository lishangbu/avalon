package io.github.lishangbu.avalon.authorization.model;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

/// 菜单树节点
///
/// 扩展 Menu 实体，用于构建树形结构并持有子节点列表
///
/// @author lishangbu
/// @since 2025/8/28
@Data
public class MenuTreeNode extends Menu {
    private List<MenuTreeNode> children;

    public MenuTreeNode(Menu menu) {
        if (menu != null) {
            BeanUtils.copyProperties(menu, this);
        }
    }
}
