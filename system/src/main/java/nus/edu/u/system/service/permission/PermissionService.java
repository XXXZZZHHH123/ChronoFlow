package nus.edu.u.system.service.permission;

import nus.edu.u.system.domain.vo.permission.PermissionReqVO;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;

import java.util.List;

/**
 * @author Lu Shuwen
 * @date 2025-09-29
 */
public interface PermissionService {

    /**
     * Get All user permissions
     * @return
     */
    List<PermissionRespVO> listPermissions();

    Long createPermission(PermissionReqVO permissionReqVO);

    PermissionRespVO getPermission(Long id);

    PermissionRespVO updatePermission(Long id, PermissionReqVO reqVO);

    Boolean deletePermission(Long id);
}
