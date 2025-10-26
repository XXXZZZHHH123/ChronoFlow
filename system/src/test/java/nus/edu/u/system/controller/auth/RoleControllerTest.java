package nus.edu.u.system.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import nus.edu.u.system.domain.vo.role.RoleAssignReqVO;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.service.role.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock private RoleService roleService;

    @InjectMocks private RoleController roleController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listRoles_returnsAllRoles() throws Exception {
        RoleRespVO respVO = RoleRespVO.builder().id(1L).name("Admin").build();
        when(roleService.listRoles()).thenReturn(List.of(respVO));

        mockMvc.perform(get("/system/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Admin"));
    }

    @Test
    void createRole_persistsRole() throws Exception {
        RoleRespVO respVO = RoleRespVO.builder().id(8L).name("Organizer").build();
        when(roleService.createRole(any(RoleReqVO.class))).thenReturn(respVO);

        RoleReqVO reqVO = new RoleReqVO();
        reqVO.setName("Organizer");
        reqVO.setKey("ORGANIZER");

        mockMvc.perform(
                        post("/system/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(8));
    }

    @Test
    void updateRole_returnsUpdatedEntity() throws Exception {
        RoleRespVO respVO = RoleRespVO.builder().id(3L).name("Editor").build();
        when(roleService.updateRole(eq(3L), any(RoleReqVO.class))).thenReturn(respVO);

        RoleReqVO reqVO = new RoleReqVO();
        reqVO.setName("Editor");
        reqVO.setKey("EDITOR");

        mockMvc.perform(
                        patch("/system/roles/{roleId}", 3L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Editor"));
    }

    @Test
    void deleteRole_returnsTrue() throws Exception {
        mockMvc.perform(delete("/system/roles/{roleId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        verify(roleService).deleteRole(4L);
    }

    @Test
    void assignRole_delegatesToService() throws Exception {
        RoleAssignReqVO reqVO = new RoleAssignReqVO();
        reqVO.setUserId(5L);
        reqVO.setRoles(List.of(1L, 2L));

        mockMvc.perform(
                        post("/system/roles/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(roleService).assignRoles(any(RoleAssignReqVO.class));
    }
}
