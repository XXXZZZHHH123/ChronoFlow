package nus.edu.u.system.service.role;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.vo.role.RoleListRespVO;
import nus.edu.u.system.mapper.role.RoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {
    @Resource private RoleMapper roleMapper;

    @Override
    public List<RoleListRespVO> listRolesExcludingAdmin() {
        return roleMapper.selectRolesExcludingAdmin().stream()
                .map(
                        role -> {
                            RoleListRespVO vo = new RoleListRespVO();
                            vo.setId(role.getId());
                            vo.setRoleName(role.getName()); // 注意：RoleDO 里字段叫 name
                            vo.setRoleKey(role.getRoleKey());
                            return vo;
                        })
                .collect(Collectors.toList());
    }
}
