package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

    /** 查当前有效角色ID（deleted = false） */
    default List<Long> selectAliveRoleIdsByUser(Long userId) {
        return this.selectList(
                Wrappers.<UserRoleDO>lambdaQuery()
                        .select(UserRoleDO::getRoleId)
                        .eq(UserRoleDO::getUserId, userId)
                        .eq(UserRoleDO::getDeleted, false)
        ).stream().map(UserRoleDO::getRoleId).toList();
    }

    /** 批量逻辑删除（置 deleted = true），审计字段由 MetaObjectHandler 更新 */
    default int batchLogicalDelete(Long userId, Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return 0;
        return this.update(new UserRoleDO(),
                Wrappers.<UserRoleDO>lambdaUpdate()
                        .set(UserRoleDO::getDeleted, true)
                        .eq(UserRoleDO::getUserId, userId)
                        .eq(UserRoleDO::getDeleted, false)
                        .in(UserRoleDO::getRoleId, roleIds)
        );
    }

    /** 批量复活（deleted: true -> false），审计字段由 MetaObjectHandler 更新 */
    default int batchRevive(Long userId, Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return 0;
        return this.update(new UserRoleDO(),
                Wrappers.<UserRoleDO>lambdaUpdate()
                        .set(UserRoleDO::getDeleted, false)
                        .eq(UserRoleDO::getUserId, userId)
                        .eq(UserRoleDO::getDeleted, true)
                        .in(UserRoleDO::getRoleId, roleIds)
        );
    }

    /**
     * 只插入“确实不存在”的关系（已存在或刚复活的不再插）
     * 返回插入条数
     */
    default int insertMissing(Long userId, Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return 0;

        // 查现在已存在（有效）的 roleId
        List<Long> existing = this.selectList(
                Wrappers.<UserRoleDO>lambdaQuery()
                        .select(UserRoleDO::getRoleId)
                        .eq(UserRoleDO::getUserId, userId)
                        .eq(UserRoleDO::getDeleted, false)
                        .in(UserRoleDO::getRoleId, roleIds)
        ).stream().map(UserRoleDO::getRoleId).toList();

        Set<Long> need = new HashSet<>(roleIds);
        need.removeAll(new HashSet<>(existing));
        if (need.isEmpty()) return 0;

        int rows = 0;
        for (Long rid : need) {
            UserRoleDO rec = UserRoleDO.builder()
                    .userId(userId)
                    .roleId(rid)
                    .build();
            rows += this.insert(rec);
        }
        return rows;
    }

    default List<Long> selectRoleIdsByUserId(Long userId) {
        return this.selectObjs(
                Wrappers.<UserRoleDO>lambdaQuery()
                        .select(UserRoleDO::getRoleId)
                        .eq(UserRoleDO::getUserId, userId)
                        .eq(UserRoleDO::getDeleted, false)
        ).stream().map(o -> (Long) o).toList();
    }
}
