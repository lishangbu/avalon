package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import org.apache.ibatis.annotations.Mapper;

/**
 * 道具属性(ItemAttribute)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface ItemAttributeMapper extends BaseMapper<ItemAttribute> {}
