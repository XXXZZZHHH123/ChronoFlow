package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import org.apache.ibatis.annotations.Mapper;


/**
 * User Mapper
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    default UserDO selectByUsername(String username) {
        return this.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)
        );
    }
}
