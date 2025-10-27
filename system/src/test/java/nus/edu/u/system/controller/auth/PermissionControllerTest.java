package nus.edu.u.system.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import nus.edu.u.system.domain.vo.permission.PermissionReqVO;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;
import nus.edu.u.system.service.permission.PermissionService;
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
class PermissionControllerTest {

    @Mock private PermissionService permissionService;

    @InjectMocks private PermissionController permissionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void list_returnsPermissions() throws Exception {
        PermissionRespVO respVO = new PermissionRespVO();
        respVO.setId(1L);
        respVO.setName("MANAGE_USERS");
        when(permissionService.listPermissions()).thenReturn(List.of(respVO));

        mockMvc.perform(get("/system/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("MANAGE_USERS"));
    }

    @Test
    void create_returnsIdentifier() throws Exception {
        when(permissionService.createPermission(any(PermissionReqVO.class))).thenReturn(9L);

        PermissionReqVO reqVO = new PermissionReqVO();
        reqVO.setName("CREATE_EVENT");
        reqVO.setKey("CREATE_EVENT");

        mockMvc.perform(
                        post("/system/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(9));
    }

    @Test
    void update_returnsUpdatedPermission() throws Exception {
        PermissionRespVO respVO = new PermissionRespVO();
        respVO.setId(4L);
        respVO.setName("EDIT_EVENT");
        when(permissionService.updatePermission(anyLong(), any(PermissionReqVO.class)))
                .thenReturn(respVO);

        PermissionReqVO reqVO = new PermissionReqVO();
        reqVO.setName("EDIT_EVENT");
        reqVO.setKey("EDIT_EVENT");

        mockMvc.perform(
                        patch("/system/permissions/{id}", 4L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(4));
    }

    @Test
    void delete_returnsBoolean() throws Exception {
        when(permissionService.deletePermission(5L)).thenReturn(true);

        mockMvc.perform(delete("/system/permissions/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(permissionService).deletePermission(5L);
    }

    @Test
    void getPermission_returnsEntry() throws Exception {
        PermissionRespVO respVO = new PermissionRespVO();
        respVO.setId(3L);
        when(permissionService.getPermission(3L)).thenReturn(respVO);

        mockMvc.perform(get("/system/permissions/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3));
    }
}
