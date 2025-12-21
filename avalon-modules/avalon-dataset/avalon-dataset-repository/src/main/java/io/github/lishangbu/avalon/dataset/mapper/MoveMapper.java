package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.Move;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招式(Move)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface MoveMapper extends BaseMapper<Move> {}
