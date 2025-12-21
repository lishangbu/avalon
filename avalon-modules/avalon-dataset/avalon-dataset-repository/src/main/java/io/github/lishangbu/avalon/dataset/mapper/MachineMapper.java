package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.Machine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招式机(Machine)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface MachineMapper extends BaseMapper<Machine> {}
