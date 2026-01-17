package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import org.apache.ibatis.annotations.Mapper;

/// 招式异常(MoveAilment)数据访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2025/09/14
@Mapper
public interface MoveAilmentMapper extends BaseMapper<MoveAilment> {}
