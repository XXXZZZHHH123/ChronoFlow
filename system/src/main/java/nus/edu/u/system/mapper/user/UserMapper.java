package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;


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
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getUsername, username)
                        .eq(UserDO::getDeleted, false)
        );
    }

    UserRoleDTO selectUserWithRole(Long userId);

    // ===== exists series, for Service reuse to avoid duplication of count code =====
    default boolean existsUsername(String username, Long excludeId) {
        return this.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getDeleted, false)
                .ne(excludeId != null, UserDO::getId, excludeId)) > 0;
    }

    default boolean existsEmail(String email, Long excludeId) {
        return this.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getEmail, email)
                .eq(UserDO::getDeleted, false)
                .ne(excludeId != null, UserDO::getId, excludeId)) > 0;
    }

    default boolean existsPhone(String phone, Long excludeId) {
        return this.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getPhone, phone)
                .eq(UserDO::getDeleted, false)
                .ne(excludeId != null, UserDO::getId, excludeId)) > 0;
    }


    /** Directly query the original record (including deleted=1), bypassing the MP automatic condition */
    @Select("SELECT * FROM sys_user WHERE id = #{id} LIMIT 1")
    UserDO selectRawById(@Param("id") Long id);

    @Update("UPDATE sys_user SET deleted = 0, update_time = NOW(), updater = #{updater} " +
            "WHERE id = #{id} AND deleted = 1")
    int restoreRawById(@Param("id") Long id, @Param("updater") String updater);


    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

    List<UserRoleDTO> selectAllUsersWithRoles();


    @Select("SELECT id FROM sys_user WHERE email = #{email} AND deleted = 0 LIMIT 1")
    Long selectIdByEmail(@Param("email") String email);

    @Select({
            "<script>",
            "SELECT email FROM sys_user WHERE deleted = 0 AND email IN",
            "<foreach collection='emails' item='e' open='(' separator=',' close=')'>",
            "  #{e}",
            "</foreach>",
            "</script>"
    })
    List<String> selectExistingEmails(@Param("emails") List<String> emails);

}
