package nus.edu.u.system.service.role;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dataobject.role.RolePermissionDO;
import nus.edu.u.system.domain.vo.permission.PermissionVO;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Resource private RoleMapper roleMapper;

    @Resource private PermissionMapper permissionMapper;

    @Resource private RolePermissionMapper rolePermissionMapper;

    public static final String ORGANIZER_ROLE_KEY = "ORGANIZER";

    @Override
    public List<RoleRespVO> listRoles() {
        List<RoleDO> roleList = roleMapper.selectList(null);
        roleList = roleList.stream().filter(role -> !ORGANIZER_ROLE_KEY.equals(role.getRoleKey())).toList();
        List<RoleRespVO> roleRespVOList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(roleList)) {
            roleList.forEach(role -> roleRespVOList.add(convert(role)));
        }
        return roleRespVOList;
    }

    @Override
    @Transactional
    public RoleRespVO createRole(RoleReqVO roleReqVO) {
        RoleDO role = RoleDO.builder()
                .name(roleReqVO.getName())
                .roleKey(roleReqVO.getKey())
                .permissionList(roleReqVO.getPermissions())
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        boolean isSuccess = roleMapper.insert(role) > 0;
        if (!isSuccess) {
            throw exception(CREATE_ROLE_FAILED);
        }
        RoleRespVO roleRespVO = RoleRespVO.builder()
                .id(role.getId())
                .name(role.getName())
                .key(role.getRoleKey())
                .build();
        if (CollectionUtil.isEmpty(roleReqVO.getPermissions())) {
            return roleRespVO;
        }

        roleReqVO.getPermissions()
                .forEach(permission -> {
                    RolePermissionDO rolePermission = RolePermissionDO.builder()
                            .roleId(role.getId())
                            .permissionId(permission)
                            .build();
                boolean isSuccessInsert = rolePermissionMapper.insert(rolePermission) > 0;
                if (!isSuccessInsert) {
                    throw exception(CREATE_ROLE_FAILED);
                }
                });

        return convert(role);
    }

    @Override
    public RoleRespVO getRole(Long roleId) {
        if (ObjUtil.isNull(roleId)) {
            throw exception(BAD_REQUEST);
        }
        RoleDO role = roleMapper.selectById(roleId);
        if (ObjUtil.isNull(role)) {
            throw exception(CANNOT_FIND_ROLE);
        }
        return convert(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        if (ObjUtil.isNull(roleId)) {
            throw exception(BAD_REQUEST);
        }
        roleMapper.deleteById(roleId);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionDO>()
                .eq(RolePermissionDO::getRoleId, roleId));
    }

    @Override
    @Transactional
    public RoleRespVO updateRole(Long roleId, RoleReqVO roleReqVO) {
        if (ObjUtil.isNull(roleId) || ObjUtil.isNull(roleReqVO)) {
            throw exception(BAD_REQUEST);
        }
        RoleDO role = roleMapper.selectById(roleId);
        if (ObjUtil.isNull(role)) {
            throw exception(CANNOT_FIND_ROLE);
        }
        Set<Long> existPermissionIds = new HashSet<>(role.getPermissionList());
        Set<Long> currentPermissionIds = new HashSet<>(roleReqVO.getPermissions());

        role.setName(roleReqVO.getName());
        role.setRoleKey(roleReqVO.getKey());
        role.setPermissionList(roleReqVO.getPermissions());
        boolean isSuccess = roleMapper.updateById(role) > 0;
        if (!isSuccess) {
            throw exception(UPDATE_ROLE_FAILED);
        }

        Set<Long> toDelete = new HashSet<>(existPermissionIds);
        toDelete.removeAll(currentPermissionIds);
        if (!CollectionUtil.isEmpty(toDelete)) {
            rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionDO>()
                    .in(RolePermissionDO::getPermissionId, toDelete));
        }

        Set<Long> toInsert = new HashSet<>(currentPermissionIds);
        toInsert.removeAll(existPermissionIds);
        if (!CollectionUtil.isEmpty(toInsert)) {
            toInsert.forEach(permissionId -> {
                RolePermissionDO rolePermissionDO = RolePermissionDO.builder()
                        .permissionId(permissionId)
                        .roleId(roleId)
                        .build();
                if (rolePermissionMapper.insert(rolePermissionDO) <= 0) {
                    throw exception(UPDATE_ROLE_FAILED);
                }
            });
        }

        return convert(role);
    }

    private RoleRespVO convert(RoleDO role) {
        if (ObjUtil.isNull(role)) {
            return null;
        }
        RoleRespVO roleRespVO = RoleRespVO.builder()
                .id(role.getId())
                .name(role.getName())
                .key(role.getRoleKey())
                .build();
        if (CollectionUtil.isEmpty(role.getPermissionList())) {
            return roleRespVO;
        }
        List<PermissionDO> permissions = permissionMapper.selectBatchIds(role.getPermissionList());
        List<PermissionVO> permissionVOList = permissions.stream()
                .map(permission -> PermissionVO.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .key(permission.getPermissionKey())
                        .build()
                )
                .toList();
        roleRespVO.setPermissions(permissionVOList);
        return roleRespVO;
    }
}
