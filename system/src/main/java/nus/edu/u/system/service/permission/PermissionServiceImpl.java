package nus.edu.u.system.service.permission;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;
import nus.edu.u.system.domain.dataobject.role.RolePermissionDO;
import nus.edu.u.system.domain.vo.permission.PermissionReqVO;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;
import nus.edu.u.system.enums.permission.PermissionTypeEnum;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import org.springframework.stereotype.Service;

/**
 * @author Lu Shuwen
 * @date 2025-09-29
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    @Resource private PermissionMapper permissionMapper;

    @Resource private RolePermissionMapper rolePermissionMapper;

    public static final String USER_PERMISSION_PREFIX = "user";

    @Override
    public List<PermissionRespVO> listPermissions() {
        List<PermissionDO> permissionList =
                permissionMapper.selectList(
                        new LambdaQueryWrapper<PermissionDO>()
                                .likeRight(PermissionDO::getPermissionKey, USER_PERMISSION_PREFIX));
        return permissionList.stream().map(this::convert).toList();
    }

    @Override
    public Long createPermission(PermissionReqVO permissionReqVO) {
        if (Objects.isNull(permissionReqVO)) {
            throw exception(BAD_REQUEST);
        }
        PermissionDO permission =
                PermissionDO.builder()
                        .name(permissionReqVO.getName())
                        .permissionKey(permissionReqVO.getKey())
                        .description(permissionReqVO.getDescription())
                        .type(PermissionTypeEnum.API.getType())
                        .build();
        return permission.getId();
    }

    @Override
    public PermissionRespVO getPermission(Long id) {
        if (ObjUtil.isNull(id)) {
            throw exception(BAD_REQUEST);
        }
        return convert(permissionMapper.selectById(id));
    }

    @Override
    public PermissionRespVO updatePermission(Long id, PermissionReqVO reqVO) {
        if (ObjUtil.isNull(id) || ObjUtil.isNull(reqVO)) {
            throw exception(BAD_REQUEST);
        }
        PermissionDO currentPermission = permissionMapper.selectById(id);
        if (ObjUtil.isNull(currentPermission)) {
            throw exception(CANNOT_FIND_PERMISSION);
        }
        PermissionDO permission =
                PermissionDO.builder()
                        .id(id)
                        .name(reqVO.getName())
                        .permissionKey(reqVO.getKey())
                        .description(reqVO.getDescription())
                        .build();
        boolean isSuccess = permissionMapper.updateById(permission) > 0;
        if (!isSuccess) {
            throw exception(UPDATE_PERMISSION_FAILED);
        }
        return convert(permission);
    }

    @Override
    public Boolean deletePermission(Long id) {
        if (ObjUtil.isNull(id)) {
            throw exception(BAD_REQUEST);
        }
        List<RolePermissionDO> rolePermissionList =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionDO>()
                                .eq(RolePermissionDO::getPermissionId, id));
        if (CollectionUtil.isNotEmpty(rolePermissionList)) {
            throw exception(CANNOT_DELETE_PERMISSION);
        }
        return permissionMapper.deleteById(id) > 0;
    }

    private PermissionRespVO convert(PermissionDO permission) {
        if (ObjUtil.isNull(permission)) {
            return null;
        }
        return PermissionRespVO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .key(permission.getPermissionKey())
                .description(permission.getDescription())
                .build();
    }
}
