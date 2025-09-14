package nus.edu.u.system.handler.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserPermissionDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handle getting user's permission
 *
 * @author Lu Shuwen
 * @date 2025-09-14
 */
@Component
public class StpPermissionHandler implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<UserPermissionDTO> userPermissionDTO = userMapper.selectUserWithPermission(Long.valueOf(loginId.toString()));
        return userPermissionDTO.stream()
                .filter(permission -> ObjectUtil.equals(permission.getStatus(), CommonStatusEnum.ENABLE.getStatus()))
                .map(UserPermissionDTO::getPermissionKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserRoleDTO userRoleDTO = userMapper.selectUserWithRole(Long.valueOf(loginId.toString()));
        return userRoleDTO.getRoles().stream()
                .filter(role -> ObjectUtil.equals(role.getStatus(), CommonStatusEnum.ENABLE.getStatus()))
                .map(RoleDTO::getRoleKey)
                .collect(Collectors.toList());
    }
}
