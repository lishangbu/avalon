package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserVO;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {
  /** 根据用户名查询 UserVO，包含 userRoles（中间表）和 roles（完整角色实体） 由 XML 通过 JOIN 实现并返回 UserVO */
  Optional<UserVO> selectByUsername(@Param("username") String username);
}
