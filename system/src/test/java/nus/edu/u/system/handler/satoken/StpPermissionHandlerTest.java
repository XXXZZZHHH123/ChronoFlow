package nus.edu.u.system.handler.satoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserPermissionDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StpPermissionHandlerTest {

    @Mock private UserMapper userMapper;

    @InjectMocks private StpPermissionHandler handler;

    @Test
    void getPermissionList_filtersDisabledPermissions() {
        List<UserPermissionDTO> permissions =
                List.of(
                        new UserPermissionDTO(
                                1L,
                                1L,
                                "Manage events",
                                "event:create",
                                3,
                                null,
                                CommonStatusEnum.ENABLE.getStatus()),
                        new UserPermissionDTO(
                                1L,
                                2L,
                                "Disabled",
                                "event:delete",
                                3,
                                null,
                                CommonStatusEnum.DISABLE.getStatus()));

        when(userMapper.selectUserWithPermission(5L)).thenReturn(permissions);

        List<String> result = handler.getPermissionList(5L, "login");

        assertThat(result).containsExactly("event:create");
    }

    @Test
    void getRoleList_filtersDisabledRoles() {
        RoleDTO enabledRole =
                RoleDTO.builder()
                        .roleKey("ADMIN")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();
        RoleDTO disabledRole =
                RoleDTO.builder()
                        .roleKey("GUEST")
                        .status(CommonStatusEnum.DISABLE.getStatus())
                        .build();

        UserRoleDTO userRoleDTO =
                UserRoleDTO.builder().userId(5L).roles(List.of(enabledRole, disabledRole)).build();

        when(userMapper.selectUserWithRole(5L)).thenReturn(userRoleDTO);

        List<String> result = handler.getRoleList("5", "login");

        assertThat(result).containsExactly("ADMIN");
    }
}
