package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.ItemCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 道具类别(ItemCategory)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface ItemCategoryMapper extends BaseMapper<ItemCategory> {}
